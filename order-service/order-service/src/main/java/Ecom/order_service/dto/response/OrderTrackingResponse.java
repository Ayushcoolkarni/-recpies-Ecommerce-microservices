package Ecom.order_service.dto.response;

import Ecom.order_service.enums.OrderStatus;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder
public class OrderTrackingResponse {

    private Long orderId;
    private Long userId;
    private OrderStatus currentStatus;
    private LocalDateTime orderedAt;
    private String estimatedDelivery;   // e.g. "22 Mar 2026"
    private List<TrackingEvent> history;

    @Data @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class TrackingEvent {
        private OrderStatus status;
        private String note;
        private LocalDateTime changedAt;
    }
}
