package com.example.admin_service.service;

import com.example.admin_service.dto.request.*;
import com.example.admin_service.dto.response.*;
import java.util.List;

public interface AdminService {

    // ── Order management ─────────────────────────────────────────
    Object getAllOrders();
    Object updateOrderStatus(OrderStatusRequest request);

    // ── Inventory management ──────────────────────────────────────
    Object getAllProducts();
    Object updateStock(StockUpdateRequest request);

    // ── Recipe suggestion management ──────────────────────────────
    Object getAllSuggestions();

    /**
     * Reviews a suggestion: saves local SuggestionReview record,
     * writes audit log, AND syncs status back to recipe-service.
     */
    SuggestionReviewResponse reviewSuggestion(SuggestionReviewRequest request);

    // ── Audit logs ────────────────────────────────────────────────
    List<AuditLogResponse> getAuditLogs();
    List<AuditLogResponse> getAuditLogsByAdmin(Long adminId);

    // ── Sales statistics ──────────────────────────────────────────
    /** Returns aggregated sales stats. period = "daily" | "weekly" | "monthly" */
    SalesStatsResponse getSalesStats(String period);

    // ── Customer management ───────────────────────────────────────
    Object getAllUsers();
    Object getUserById(Long userId);
}
