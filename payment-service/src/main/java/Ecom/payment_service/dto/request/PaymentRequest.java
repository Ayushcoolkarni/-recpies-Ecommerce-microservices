package Ecom.payment_service.dto.request;

import lombok.Data;

@Data
public class PaymentRequest {
    private Long orderId;
    private Long userId;
    private Double amount;
    private String gateway;
    private String paymentToken;
}