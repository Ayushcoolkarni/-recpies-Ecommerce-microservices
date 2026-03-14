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

    // ─── Order Management ───────────────────────────────────────

    @GetMapping("/orders")
    public ResponseEntity<Object> getAllOrders() {
        return ResponseEntity.ok(adminService.getAllOrders());
    }

    @PatchMapping("/orders/status")
    public ResponseEntity<Object> updateOrderStatus(
            @RequestBody OrderStatusRequest request) {
        return ResponseEntity.ok(adminService.updateOrderStatus(request));
    }

    // ─── Inventory Management ────────────────────────────────────

    @GetMapping("/products")
    public ResponseEntity<Object> getAllProducts() {
        return ResponseEntity.ok(adminService.getAllProducts());
    }

    @PatchMapping("/products/stock")
    public ResponseEntity<Object> updateStock(
            @RequestBody StockUpdateRequest request) {
        return ResponseEntity.ok(adminService.updateStock(request));
    }

    // ─── Recipe Suggestion Management ────────────────────────────

    @GetMapping("/suggestions")
    public ResponseEntity<Object> getAllSuggestions() {
        return ResponseEntity.ok(adminService.getAllSuggestions());
    }

    @PostMapping("/suggestions/review")
    public ResponseEntity<SuggestionReviewResponse> reviewSuggestion(
            @RequestBody SuggestionReviewRequest request) {
        return ResponseEntity.ok(adminService.reviewSuggestion(request));
    }

    // ─── Audit Log ───────────────────────────────────────────────

    @GetMapping("/audit-logs")
    public ResponseEntity<List<AuditLogResponse>> getAuditLogs() {
        return ResponseEntity.ok(adminService.getAuditLogs());
    }

    @GetMapping("/audit-logs/{adminId}")
    public ResponseEntity<List<AuditLogResponse>> getAuditLogsByAdmin(
            @PathVariable Long adminId) {
        return ResponseEntity.ok(adminService.getAuditLogsByAdmin(adminId));
    }
}