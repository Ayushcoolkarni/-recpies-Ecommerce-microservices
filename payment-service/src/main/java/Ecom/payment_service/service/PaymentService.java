package Ecom.payment_service.service;

import Ecom.payment_service.dto.request.*;
import Ecom.payment_service.dto.response.PaymentResponse;
import java.util.List;

public interface PaymentService {
    PaymentResponse initiatePayment(PaymentRequest request);
    PaymentResponse getPaymentByOrderId(Long orderId);
    List<PaymentResponse> getPaymentsByUser(Long userId);
    PaymentResponse processRefund(RefundRequest request);
}