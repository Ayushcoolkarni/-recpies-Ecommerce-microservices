package Ecom.order_service.event;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class OrderPlacedEvent {
    private Long orderId;
    private Long userId;
    private Double totalAmount;
    private String status;
}