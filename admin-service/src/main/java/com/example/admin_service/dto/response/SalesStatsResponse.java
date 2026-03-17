package com.example.admin_service.dto.response;

import lombok.*;
import java.util.Map;

@Data @Builder
public class SalesStatsResponse {

    /** Total number of orders in the period */
    private Long totalOrders;

    /** Total revenue (sum of order amounts) */
    private Double totalRevenue;

    /** Average order value */
    private Double averageOrderValue;

    /** Orders grouped by status e.g. { "PENDING": 5, "DELIVERED": 20 } */
    private Map<String, Long> ordersByStatus;

    /** The period label e.g. "daily", "weekly", "monthly" */
    private String period;
}
