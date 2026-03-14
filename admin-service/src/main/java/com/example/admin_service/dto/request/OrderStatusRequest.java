package com.example.admin_service.dto.request;

import lombok.Data;

@Data
public class OrderStatusRequest {
    private Long orderId;
    private String status;
    private Long adminId;
}