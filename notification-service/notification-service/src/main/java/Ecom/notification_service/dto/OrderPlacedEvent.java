package Ecom.notification_service.dto;

import lombok.*;

/**
 * Kafka event payload consumed from order.placed / order.shipped / order.delivered.
 *
 * userEmail is populated by notification-service itself by calling user-service
 * (see UserServiceClient) — it is NOT expected to be present in the Kafka message.
 */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderPlacedEvent {
    private Long   orderId;
    private Long   userId;
    private Double totalAmount;
    private String status;

    /**
     * Resolved at runtime by notification-service via UserServiceClient.
     * Not part of the Kafka message payload.
     */
    private String userEmail;
}
