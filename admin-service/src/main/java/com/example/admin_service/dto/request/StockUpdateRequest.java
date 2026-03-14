package com.example.admin_service.dto.request;

import lombok.Data;

@Data
public class StockUpdateRequest {
    private Long productId;
    private Integer quantity;
    private Long adminId;
}