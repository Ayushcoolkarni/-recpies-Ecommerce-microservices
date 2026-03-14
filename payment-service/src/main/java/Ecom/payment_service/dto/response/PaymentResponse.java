package Ecom.payment_service.dto.response;

import Ecom.payment_service.enums.PaymentStatus;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder
public class PaymentResponse {
    private Long id;
    private Long orderId;
    private Long userId;
    private Double amount;
    private String currency;
    private PaymentStatus status;
    private String gateway;
    private String transactionId;
    private LocalDateTime createdAt;
}