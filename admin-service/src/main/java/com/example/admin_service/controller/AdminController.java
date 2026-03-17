package com.example.admin_service.controller;

import com.example.admin_service.dto.request.*;
import com.example.admin_service.dto.response.*;
import com.example.admin_service.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // ── Order Management ─────────────────────────────────────────

    /** GET /admin/orders — list all orders across all users */
    @GetMapping("/orders")
    public ResponseEntity<Object> getAllOrders() {
        return ResponseEntity.ok(adminService.getAllOrders());
    }

    /**
     * PATCH /admin/orders/status
     * Body: { "orderId": 1, "status": "SHIPPED", "adminId": 5 }
     */
    @PatchMapping("/orders/status")
    public ResponseEntity<Object> updateOrderStatus(
            @RequestBody OrderStatusRequest request) {
        return ResponseEntity.ok(adminService.updateOrderStatus(request));
    }

    // ── Inventory Management ─────────────────────────────────────

    /** GET /admin/products — list all products */
    @GetMapping("/products")
    public ResponseEntity<Object> getAllProducts() {
        return ResponseEntity.ok(adminService.getAllProducts());
    }

    /**
     * PATCH /admin/products/stock
     * Body: { "productId": 3, "quantity": 100, "adminId": 5 }
     */
    @PatchMapping("/products/stock")
    public ResponseEntity<Object> updateStock(
            @RequestBody StockUpdateRequest request) {
        return ResponseEntity.ok(adminService.updateStock(request));
    }

    // ── Recipe Suggestion Management ──────────────────────────────

    /** GET /admin/suggestions — all pending and reviewed suggestions */
    @GetMapping("/suggestions")
    public ResponseEntity<Object> getAllSuggestions() {
        return ResponseEntity.ok(adminService.getAllSuggestions());
    }

    /**
     * POST /admin/suggestions/review
     * Body: { "suggestionId": 2, "adminId": 5, "decision": "APPROVED", "notes": "Great idea" }
     * Saves local review, syncs status to recipe-service, writes audit log.
     */
    @PostMapping("/suggestions/review")
    public ResponseEntity<SuggestionReviewResponse> reviewSuggestion(
            @RequestBody SuggestionReviewRequest request) {
        return ResponseEntity.ok(adminService.reviewSuggestion(request));
    }

    // ── Sales Statistics ─────────────────────────────────────────

    /**
     * GET /admin/stats?period=daily   — today's stats
     * GET /admin/stats?period=weekly  — this week's stats
     * GET /admin/stats?period=monthly — this month's stats
     *
     * Response: { totalOrders, totalRevenue, averageOrderValue, ordersByStatus, period }
     */
    @GetMapping("/stats")
    public ResponseEntity<SalesStatsResponse> getSalesStats(
            @RequestParam(defaultValue = "daily") String period) {
        return ResponseEntity.ok(adminService.getSalesStats(period));
    }

    // ── Customer Management ───────────────────────────────────────

    /** GET /admin/users — list all registered users */
    @GetMapping("/users")
    public ResponseEntity<Object> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    /** GET /admin/users/{userId} — get a specific user's details */
    @GetMapping("/users/{userId}")
    public ResponseEntity<Object> getUserById(@PathVariable Long userId) {
        return ResponseEntity.ok(adminService.getUserById(userId));
    }

    // ── Audit Logs ────────────────────────────────────────────────

    /** GET /admin/audit-logs — all admin actions */
    @GetMapping("/audit-logs")
    public ResponseEntity<List<AuditLogResponse>> getAuditLogs() {
        return ResponseEntity.ok(adminService.getAuditLogs());
    }

    /** GET /admin/audit-logs/{adminId} — actions by a specific admin */
    @GetMapping("/audit-logs/{adminId}")
    public ResponseEntity<List<AuditLogResponse>> getAuditLogsByAdmin(
            @PathVariable Long adminId) {
        return ResponseEntity.ok(adminService.getAuditLogsByAdmin(adminId));
    }
}
