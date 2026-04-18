package Ecom.order_service.kafka;

import Ecom.order_service.entity.Order;
import Ecom.order_service.entity.OrderStatusHistory;
import Ecom.order_service.enums.OrderStatus;
import Ecom.order_service.event.OrderPlacedEvent;
import Ecom.order_service.event.PaymentFailedEvent;
import Ecom.order_service.event.PaymentSuccessEvent;
import Ecom.order_service.repository.OrderRepository;
import Ecom.order_service.repository.OrderStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Saga Step 2 (Order side) — reacts to payment outcomes.
 *
 * payment.success → PENDING → CONFIRMED → publishes order.confirmed
 * payment.failed  → PENDING → CANCELLED → publishes order.cancelled  ← compensating tx
 *
 * Manual ack: Kafka offset is committed ONLY after the DB write + downstream
 * publish both succeed. If either fails, the error handler retries up to 3x,
 * then dead-letters the message to payment.success.DLT / payment.failed.DLT.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final OrderRepository              orderRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    // ── PAYMENT SUCCESS ──────────────────────────────────────────

    @KafkaListener(
        topics          = KafkaTopicConfig.PAYMENT_SUCCESS,
        groupId         = "order-service-group",
        containerFactory = "paymentSuccessKafkaListenerContainerFactory"
    )
    @Transactional
    public void onPaymentSuccess(PaymentSuccessEvent event, Acknowledgment ack) {
        log.info("[SAGA] payment.success received — orderId={} txn={}",
                event.getOrderId(), event.getTransactionId());

        Order order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> {
                    log.error("[SAGA] Order {} not found on payment.success", event.getOrderId());
                    return new RuntimeException("Order not found: " + event.getOrderId());
                });

        // Idempotency guard — skip if already moved past PENDING
        if (order.getStatus() != OrderStatus.PENDING) {
            log.warn("[SAGA] Order {} is already {} — skipping payment.success",
                    order.getOrderId(), order.getStatus());
            ack.acknowledge(); // ack so it doesn't retry an idempotent skip
            return;
        }

        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);

        saveHistory(order.getOrderId(), OrderStatus.CONFIRMED,
                "Payment confirmed — txn: " + event.getTransactionId());

        // Publish order.confirmed → inventory-service deducts stock
        OrderPlacedEvent confirmedEvent = OrderPlacedEvent.builder()
                .orderId(order.getOrderId())
                .userId(order.getUserId())
                .totalAmount(order.getTotalAmount())
                .status(OrderStatus.CONFIRMED.name())
                .build();

        kafkaTemplate.send(KafkaTopicConfig.ORDER_CONFIRMED, confirmedEvent);
        log.info("[SAGA] order.confirmed published — orderId={}", order.getOrderId());

        // FIX: commit offset only after DB write + publish both succeed
        ack.acknowledge();
    }

    // ── PAYMENT FAILED — compensating transaction ────────────────

    @KafkaListener(
        topics          = KafkaTopicConfig.PAYMENT_FAILED,
        groupId         = "order-service-group",
        containerFactory = "paymentFailedKafkaListenerContainerFactory"
    )
    @Transactional
    public void onPaymentFailed(PaymentFailedEvent event, Acknowledgment ack) {
        log.warn("[SAGA] payment.failed received — orderId={} reason={}",
                event.getOrderId(), event.getReason());

        Order order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> {
                    log.error("[SAGA] Order {} not found on payment.failed", event.getOrderId());
                    return new RuntimeException("Order not found: " + event.getOrderId());
                });

        // Idempotency guard
        if (order.getStatus() == OrderStatus.CANCELLED) {
            log.warn("[SAGA] Order {} already CANCELLED — skipping", order.getOrderId());
            ack.acknowledge();
            return;
        }

        // ── COMPENSATING TRANSACTION ─────────────────────────────
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        saveHistory(order.getOrderId(), OrderStatus.CANCELLED,
                "Payment failed — " + event.getReason() + ". Order auto-cancelled.");

        // Publish order.cancelled → notification-service alerts the user
        OrderPlacedEvent cancelledEvent = OrderPlacedEvent.builder()
                .orderId(order.getOrderId())
                .userId(order.getUserId())
                .totalAmount(order.getTotalAmount())
                .status(OrderStatus.CANCELLED.name())
                .build();

        kafkaTemplate.send(KafkaTopicConfig.ORDER_CANCELLED, cancelledEvent);
        log.warn("[SAGA] order.cancelled published — orderId={}", order.getOrderId());

        // FIX: commit offset only after DB write + publish both succeed
        ack.acknowledge();
    }

    // ── helper ───────────────────────────────────────────────────

    private void saveHistory(Long orderId, OrderStatus status, String note) {
        historyRepository.save(OrderStatusHistory.builder()
                .orderId(orderId)
                .status(status)
                .note(note)
                .build());
    }
}
