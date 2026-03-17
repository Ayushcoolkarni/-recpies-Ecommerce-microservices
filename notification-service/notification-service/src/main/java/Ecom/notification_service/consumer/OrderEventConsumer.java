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
 *   order.delivered → send delivery notification email  ← NEW
 *
 * All three use the same event payload (OrderPlacedEvent).
 * The groupId is shared so each notification is consumed once per service instance.
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
}
