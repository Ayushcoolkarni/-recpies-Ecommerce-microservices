package Ecom.payment_service.service;

import Ecom.payment_service.dto.request.PaymentRequest;
import Ecom.payment_service.dto.request.RefundRequest;
import Ecom.payment_service.dto.response.PaymentResponse;
import Ecom.payment_service.entity.Payment;
import Ecom.payment_service.entity.Refund;
import Ecom.payment_service.enums.PaymentStatus;
import Ecom.payment_service.event.PaymentFailedEvent;
import Ecom.payment_service.event.PaymentSuccessEvent;
import Ecom.payment_service.exception.ResourceNotFoundException;
import Ecom.payment_service.kafka.KafkaTopicConfig;
import Ecom.payment_service.mapper.PaymentMapper;
import Ecom.payment_service.repository.PaymentRepository;
import Ecom.payment_service.repository.RefundRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository                          paymentRepository;
    private final RefundRepository                           refundRepository;
    private final PaymentMapper                              paymentMapper;
    private final KafkaTemplate<String, PaymentSuccessEvent> successTemplate;
    private final KafkaTemplate<String, PaymentFailedEvent>  failedTemplate;

    @Override
    @Transactional
    public PaymentResponse initiatePayment(PaymentRequest request) {
        String transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();

        Payment payment = Payment.builder()
                .orderId(request.getOrderId())
                .userId(request.getUserId())
                .amount(request.getAmount())
                .gateway(request.getGateway())
                .transactionId(transactionId)
                .status(PaymentStatus.SUCCESS)
                .build();

        Payment saved = paymentRepository.save(payment);

        PaymentSuccessEvent event = PaymentSuccessEvent.builder()
                .orderId(saved.getOrderId())
                .userId(saved.getUserId())
                .transactionId(transactionId)
                .amount(saved.getAmount())
                .build();
        successTemplate.send(KafkaTopicConfig.PAYMENT_SUCCESS, event);
        log.info("[SAGA] payment.success published — orderId={}", saved.getOrderId());

        return paymentMapper.toResponse(saved);
    }

    @Override
    public PaymentResponse getPaymentByOrderId(Long orderId) {
        return paymentMapper.toResponse(
                paymentRepository.findByOrderId(orderId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Payment not found for orderId: " + orderId)));
    }

    @Override
    public List<PaymentResponse> getPaymentsByUser(Long userId) {
        return paymentRepository.findByUserId(userId).stream()
                .map(paymentMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PaymentResponse processRefund(RefundRequest request) {
        Payment payment = paymentRepository.findByOrderId(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment not found for orderId: " + request.getOrderId()));

        payment.setStatus(PaymentStatus.REFUNDED);
        paymentRepository.save(payment);

        Refund refund = Refund.builder()
                .paymentId(payment.getId())
                .amount(request.getAmount() != null ? request.getAmount() : payment.getAmount())
                .reason(request.getReason())
                .status("PROCESSED")
                .build();
        refundRepository.save(refund);

        PaymentFailedEvent failedEvent = PaymentFailedEvent.builder()
                .orderId(payment.getOrderId())
                .userId(payment.getUserId())
                .reason("Refund processed: " + request.getReason())
                .amount(payment.getAmount())
                .build();
        failedTemplate.send(KafkaTopicConfig.PAYMENT_FAILED, failedEvent);
        log.info("[REFUND] payment.failed published — orderId={}", payment.getOrderId());

        return paymentMapper.toResponse(payment);
    }
}
