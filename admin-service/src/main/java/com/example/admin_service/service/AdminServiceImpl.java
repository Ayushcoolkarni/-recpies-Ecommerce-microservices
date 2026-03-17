package com.example.admin_service.service;

import com.example.admin_service.dto.request.*;
import com.example.admin_service.dto.response.*;
import com.example.admin_service.entity.*;
import com.example.admin_service.enums.AdminAction;
import com.example.admin_service.mapper.*;
import com.example.admin_service.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.*;
import java.util.stream.Collectors;

/**
 * NOTE ON PACKAGE:
 * Original code was in service.impl — this has been moved to service
 * so Spring can correctly autowire the AdminService bean everywhere.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final RestClient orderRestClient;
    private final RestClient inventoryRestClient;
    private final RestClient recipeRestClient;
    private final RestClient userRestClient;

    private final AuditLogRepository        auditLogRepository;
    private final SuggestionReviewRepository suggestionReviewRepository;
    private final AuditLogMapper            auditLogMapper;
    private final SuggestionReviewMapper    suggestionReviewMapper;

    // ── ORDER MANAGEMENT ─────────────────────────────────────────

    @Override
    public Object getAllOrders() {
        return orderRestClient.get()
                .uri("/orders")
                .retrieve()
                .body(Object.class);
    }

    @Override
    public Object updateOrderStatus(OrderStatusRequest request) {
        Object result = orderRestClient.patch()
                .uri("/orders/{id}/status?status={status}",
                        request.getOrderId(), request.getStatus())
                .retrieve()
                .body(Object.class);

        saveAuditLog(request.getAdminId(), AdminAction.UPDATE_ORDER_STATUS,
                "Order", request.getOrderId(),
                "Status updated to " + request.getStatus());

        log.info("Admin {} updated order {} to status {}",
                request.getAdminId(), request.getOrderId(), request.getStatus());
        return result;
    }

    // ── INVENTORY MANAGEMENT ─────────────────────────────────────

    @Override
    public Object getAllProducts() {
        return inventoryRestClient.get()
                .uri("/products")
                .retrieve()
                .body(Object.class);
    }

    @Override
    public Object updateStock(StockUpdateRequest request) {
        Object result = inventoryRestClient.patch()
                .uri("/products/{id}/stock?quantity={qty}",
                        request.getProductId(), request.getQuantity())
                .retrieve()
                .body(Object.class);

        saveAuditLog(request.getAdminId(), AdminAction.MANAGE_INVENTORY,
                "Product", request.getProductId(),
                "Stock updated to " + request.getQuantity());

        log.info("Admin {} updated stock for product {} to {}",
                request.getAdminId(), request.getProductId(), request.getQuantity());
        return result;
    }

    // ── SUGGESTION MANAGEMENT ─────────────────────────────────────

    @Override
    public Object getAllSuggestions() {
        return recipeRestClient.get()
                .uri("/suggestions")
                .retrieve()
                .body(Object.class);
    }

    @Override
    public SuggestionReviewResponse reviewSuggestion(SuggestionReviewRequest request) {

        // 1. Save local review record
        SuggestionReview review = SuggestionReview.builder()
                .suggestionId(request.getSuggestionId())
                .adminId(request.getAdminId())
                .decision(request.getDecision())
                .notes(request.getNotes())
                .build();
        SuggestionReview saved = suggestionReviewRepository.save(review);

        // 2. Sync status back to recipe-service so the source of truth stays consistent
        try {
            recipeRestClient.patch()
                    .uri("/suggestions/{id}/status?status={status}",
                            request.getSuggestionId(),
                            request.getDecision().toUpperCase())
                    .retrieve()
                    .toBodilessEntity();

            log.info("Suggestion {} status synced to recipe-service as {}",
                    request.getSuggestionId(), request.getDecision());
        } catch (Exception e) {
            log.error("Failed to sync suggestion status to recipe-service: {}", e.getMessage());
        }

        // 3. Write audit log
        AdminAction action = request.getDecision().equalsIgnoreCase("APPROVED")
                ? AdminAction.APPROVE_SUGGESTION
                : AdminAction.REJECT_SUGGESTION;

        saveAuditLog(request.getAdminId(), action,
                "Suggestion", request.getSuggestionId(),
                "Decision: " + request.getDecision() + " | Notes: " + request.getNotes());

        return suggestionReviewMapper.toResponse(saved);
    }

    // ── AUDIT LOGS ────────────────────────────────────────────────

    @Override
    public List<AuditLogResponse> getAuditLogs() {
        return auditLogRepository.findAll().stream()
                .map(auditLogMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<AuditLogResponse> getAuditLogsByAdmin(Long adminId) {
        return auditLogRepository.findByAdminId(adminId).stream()
                .map(auditLogMapper::toResponse)
                .collect(Collectors.toList());
    }

    // ── SALES STATISTICS ─────────────────────────────────────────

    @Override
    @SuppressWarnings("unchecked")
    public SalesStatsResponse getSalesStats(String period) {
        // Fetch all orders from order-service
        List<Map<String, Object>> orders;
        try {
            orders = (List<Map<String, Object>>) orderRestClient.get()
                    .uri("/orders")
                    .retrieve()
                    .body(List.class);
        } catch (Exception e) {
            log.error("Failed to fetch orders for stats: {}", e.getMessage());
            return SalesStatsResponse.builder()
                    .totalOrders(0L).totalRevenue(0.0)
                    .averageOrderValue(0.0)
                    .ordersByStatus(Map.of())
                    .period(period)
                    .build();
        }

        if (orders == null) orders = List.of();

        long totalOrders = orders.size();

        double totalRevenue = orders.stream()
                .mapToDouble(o -> {
                    Object amt = o.get("totalAmount");
                    return amt != null ? ((Number) amt).doubleValue() : 0.0;
                })
                .sum();

        double avgOrderValue = totalOrders > 0
                ? Math.round((totalRevenue / totalOrders) * 100.0) / 100.0
                : 0.0;

        Map<String, Long> byStatus = orders.stream()
                .collect(Collectors.groupingBy(
                        o -> o.getOrDefault("status", "UNKNOWN").toString(),
                        Collectors.counting()
                ));

        log.info("Stats computed — period={} orders={} revenue={}",
                period, totalOrders, totalRevenue);

        return SalesStatsResponse.builder()
                .totalOrders(totalOrders)
                .totalRevenue(Math.round(totalRevenue * 100.0) / 100.0)
                .averageOrderValue(avgOrderValue)
                .ordersByStatus(byStatus)
                .period(period)
                .build();
    }

    // ── USER MANAGEMENT ───────────────────────────────────────────

    @Override
    public Object getAllUsers() {
        try {
            return userRestClient.get()
                    .uri("/users")
                    .retrieve()
                    .body(Object.class);
        } catch (Exception e) {
            log.error("Failed to fetch users: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public Object getUserById(Long userId) {
        return userRestClient.get()
                .uri("/users/{id}", userId)
                .retrieve()
                .body(Object.class);
    }

    // ── private helpers ───────────────────────────────────────────

    private void saveAuditLog(Long adminId, AdminAction action,
                              String targetType, Long targetId, String details) {
        auditLogRepository.save(AuditLog.builder()
                .adminId(adminId)
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .details(details)
                .build());
    }
}
