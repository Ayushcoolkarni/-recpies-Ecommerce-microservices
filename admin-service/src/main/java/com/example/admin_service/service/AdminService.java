package com.example.admin_service.service;

import com.example.admin_service.dto.request.*;
import com.example.admin_service.dto.response.*;
import java.util.List;

public interface AdminService {

    // Order management
    Object getAllOrders();
    Object updateOrderStatus(OrderStatusRequest request);

    // Inventory management
    Object getAllProducts();
    Object updateStock(StockUpdateRequest request);

    // Recipe suggestion management
    Object getAllSuggestions();
    SuggestionReviewResponse reviewSuggestion(SuggestionReviewRequest request);

    // Audit log
    List<AuditLogResponse> getAuditLogs();
    List<AuditLogResponse> getAuditLogsByAdmin(Long adminId);
}