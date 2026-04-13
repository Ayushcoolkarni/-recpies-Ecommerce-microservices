package Ecom.notification_service.consumer;

import Ecom.notification_service.dto.OrderPlacedEvent;
import Ecom.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes three Kafka topics published by order-service:
 *
 *   order.placed    → send order confirmation email
 *   order.shipped   → send shipping notification email
 *   order.delivered → send delivery notification email
 *
 * All three use OrderPlacedEvent as the payload.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = "order.placed", groupId = "notification-group")
    public void onOrderPlaced(OrderPlacedEvent event) {
        log.info("Received order.placed — orderId={} userId={}",
                event.getOrderId(), event.getUserId());
        notificationService.sendOrderConfirmation(event);
    }

    @KafkaListener(topics = "order.shipped", groupId = "notification-group")
    public void onOrderShipped(OrderPlacedEvent event) {
        log.info("Received order.shipped — orderId={} userId={}",
                event.getOrderId(), event.getUserId());
        notificationService.sendShippingNotification(event);
    }

    @KafkaListener(topics = "order.delivered", groupId = "notification-group")
    public void onOrderDelivered(OrderPlacedEvent event) {
        log.info("Received order.delivered — orderId={} userId={}",
                event.getOrderId(), event.getUserId());
        notificationService.sendDeliveryNotification(event);
    }
    @KafkaListener(topics = "order.confirmed", groupId = "notification-group")
    public void onOrderConfirmed(OrderPlacedEvent event) {
        log.info("Received order.confirmed — orderId={}", event.getOrderId());
        notificationService.sendPaymentConfirmedNotification(event);
    }

    @KafkaListener(topics = "order.cancelled", groupId = "notification-group")
    public void onOrderCancelled(OrderPlacedEvent event) {
        log.warn("Received order.cancelled — orderId={}", event.getOrderId());
        notificationService.sendOrderCancelledNotification(event);
    }
}
