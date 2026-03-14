package com.example.admin_service.service.impl;

import com.example.admin_service.dto.request.*;
import com.example.admin_service.dto.response.*;
import com.example.admin_service.entity.*;
import com.example.admin_service.enums.AdminAction;
import com.example.admin_service.exception.ResourceNotFoundException;
import com.example.admin_service.mapper.*;
import com.example.admin_service.repository.*;
import com.example.admin_service.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminServiceImpl implements AdminService {

    private final RestClient orderRestClient;
    private final RestClient inventoryRestClient;
    private final RestClient recipeRestClient;
    private final AuditLogRepository auditLogRepository;
    private final SuggestionReviewRepository suggestionReviewRepository;
    private final AuditLogMapper auditLogMapper;
    private final SuggestionReviewMapper suggestionReviewMapper;

    // ─── Order Management ───────────────────────────────────────

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

        saveAuditLog(
                request.getAdminId(),
                AdminAction.UPDATE_ORDER_STATUS,
                "Order",
                request.getOrderId(),
                "Status updated to " + request.getStatus()
        );
        return result;
    }

    // ─── Inventory Management ────────────────────────────────────

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

        saveAuditLog(
                request.getAdminId(),
                AdminAction.MANAGE_INVENTORY,
                "Product",
                request.getProductId(),
                "Stock updated to " + request.getQuantity()
        );
        return result;
    }

    // ─── Recipe Suggestion Management ────────────────────────────

    @Override
    public Object getAllSuggestions() {
        return recipeRestClient.get()
                .uri("/suggestions")
                .retrieve()
                .body(Object.class);
    }

    @Override
    public SuggestionReviewResponse reviewSuggestion(SuggestionReviewRequest request) {
        SuggestionReview review = SuggestionReview.builder()
                .suggestionId(request.getSuggestionId())
                .adminId(request.getAdminId())
                .decision(request.getDecision())
                .notes(request.getNotes())
                .build();

        SuggestionReview saved = suggestionReviewRepository.save(review);

        AdminAction action = request.getDecision().equalsIgnoreCase("APPROVED")
                ? AdminAction.APPROVE_SUGGESTION
                : AdminAction.REJECT_SUGGESTION;

        saveAuditLog(
                request.getAdminId(),
                action,
                "Suggestion",
                request.getSuggestionId(),
                "Decision: " + request.getDecision()
        );

        return suggestionReviewMapper.toResponse(saved);
    }

    // ─── Audit Log ───────────────────────────────────────────────

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

    // ─── Private Helper ──────────────────────────────────────────

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