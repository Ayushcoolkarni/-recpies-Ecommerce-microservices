package Ecom.notification_service.consumer;

import Ecom.notification_service.dto.OrderPlacedEvent;
import Ecom.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Consumes order lifecycle events from Kafka and dispatches email notifications.
 *
 * Topics consumed:
 *   order.placed    → order confirmation email
 *   order.confirmed → payment confirmed email
 *   order.shipped   → shipping notification email
 *   order.delivered → delivery notification email
 *   order.cancelled → cancellation email
 *
 * Manual ack: offset is committed ONLY after the email is successfully sent.
 * If the SMTP call fails, the error handler retries 3x then dead-letters the event.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = "order.placed", groupId = "notification-group")
    public void onOrderPlaced(OrderPlacedEvent event, Acknowledgment ack) {
        log.info("Received order.placed — orderId={} userId={}", event.getOrderId(), event.getUserId());
        try {
            notificationService.sendOrderConfirmation(event);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to send order.placed notification for orderId={}: {}", event.getOrderId(), e.getMessage(), e);
            throw new RuntimeException("Notification failed for order.placed", e);
        }
    }

    @KafkaListener(topics = "order.confirmed", groupId = "notification-group")
    public void onOrderConfirmed(OrderPlacedEvent event, Acknowledgment ack) {
        log.info("Received order.confirmed — orderId={}", event.getOrderId());
        try {
            notificationService.sendPaymentConfirmedNotification(event);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to send order.confirmed notification for orderId={}: {}", event.getOrderId(), e.getMessage(), e);
            throw new RuntimeException("Notification failed for order.confirmed", e);
        }
    }

    @KafkaListener(topics = "order.shipped", groupId = "notification-group")
    public void onOrderShipped(OrderPlacedEvent event, Acknowledgment ack) {
        log.info("Received order.shipped — orderId={}", event.getOrderId());
        try {
            notificationService.sendShippingNotification(event);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to send order.shipped notification for orderId={}: {}", event.getOrderId(), e.getMessage(), e);
            throw new RuntimeException("Notification failed for order.shipped", e);
        }
    }

    @KafkaListener(topics = "order.delivered", groupId = "notification-group")
    public void onOrderDelivered(OrderPlacedEvent event, Acknowledgment ack) {
        log.info("Received order.delivered — orderId={}", event.getOrderId());
        try {
            notificationService.sendDeliveryNotification(event);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to send order.delivered notification for orderId={}: {}", event.getOrderId(), e.getMessage(), e);
            throw new RuntimeException("Notification failed for order.delivered", e);
        }
    }

    @KafkaListener(topics = "order.cancelled", groupId = "notification-group")
    public void onOrderCancelled(OrderPlacedEvent event, Acknowledgment ack) {
        log.warn("Received order.cancelled — orderId={}", event.getOrderId());
        try {
            notificationService.sendOrderCancelledNotification(event);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to send order.cancelled notification for orderId={}: {}", event.getOrderId(), e.getMessage(), e);
            throw new RuntimeException("Notification failed for order.cancelled", e);
        }
    }
}
