package Ecom.payment_service.service;

import Ecom.payment_service.dto.request.*;
import Ecom.payment_service.dto.response.PaymentResponse;
import Ecom.payment_service.entity.*;
import Ecom.payment_service.enums.PaymentStatus;
import Ecom.payment_service.exception.ResourceNotFoundException;
import Ecom.payment_service.mapper.PaymentMapper;
import Ecom.payment_service.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final PaymentMapper paymentMapper;

    @Override
    public PaymentResponse initiatePayment(PaymentRequest request) {
        // In production: call Stripe/PayPal SDK here with request.getPaymentToken()
        String transactionId = UUID.randomUUID().toString();

        Payment payment = Payment.builder()
                .orderId(request.getOrderId())
                .userId(request.getUserId())
                .amount(request.getAmount())
                .gateway(request.getGateway())
                .transactionId(transactionId)
                .status(PaymentStatus.SUCCESS) // mock success
                .build();

        return paymentMapper.toResponse(paymentRepository.save(payment));
    }

    @Override
    public PaymentResponse getPaymentByOrderId(Long orderId) {
        return paymentMapper.toResponse(paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found")));
    }

    @Override
    public List<PaymentResponse> getPaymentsByUser(Long userId) {
        return paymentRepository.findByUserId(userId).stream()
                .map(paymentMapper::toResponse).collect(Collectors.toList());
    }

    @Override
    public PaymentResponse processRefund(RefundRequest request) {
        Payment payment = paymentRepository.findById(request.getPaymentId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        payment.setStatus(PaymentStatus.REFUNDED);
        paymentRepository.save(payment);

        Refund refund = Refund.builder()
                .paymentId(payment.getId())
                .amount(request.getAmount())
                .reason(request.getReason())
                .status("PROCESSED")
                .build();
        refundRepository.save(refund);

        return paymentMapper.toResponse(payment);
    }
}