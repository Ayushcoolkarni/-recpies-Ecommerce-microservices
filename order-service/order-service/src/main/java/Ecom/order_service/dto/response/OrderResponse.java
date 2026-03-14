package Ecom.order_service.dto.response;

import Ecom.order_service.enums.OrderStatus;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder
public class OrderResponse {
    private Long orderId;
    private Long userId;
    private Long addressId;
    private OrderStatus status;
    private Double totalAmount;
    private LocalDateTime createdAt;
    private List<OrderItemResponse> items;
}