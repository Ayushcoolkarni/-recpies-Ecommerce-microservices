package Ecom.payment_service.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentRequest {

    @NotNull(message = "orderId is required")
    private Long orderId;

    @NotNull(message = "userId is required")
    private Long userId;

    @NotNull
    @Min(value = 1, message = "amount must be greater than 0")
    private Double amount;

    /** Gateway: "SAGA" (default/test mode), "STRIPE", "RAZORPAY" */
    private String gateway = "SAGA";

    /** Only required when gateway = STRIPE. Ignored in SAGA/test mode. */
    private String paymentMethodId;

    private String currency = "INR";
}
