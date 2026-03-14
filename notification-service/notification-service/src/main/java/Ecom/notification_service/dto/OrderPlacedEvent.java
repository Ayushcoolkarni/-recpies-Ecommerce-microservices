package Ecom.notification_service.dto;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class OrderPlacedEvent {
    private Long orderId;
    private Long userId;
    private Double totalAmount;
    private String status;
}