package Ecom.notification_service.consumer;

import Ecom.notification_service.dto.OrderPlacedEvent;
import Ecom.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = "order.placed", groupId = "notification-group")
    public void onOrderPlaced(OrderPlacedEvent event) {
        log.info("Received order.placed event for orderId: {}", event.getOrderId());
        notificationService.sendOrderConfirmation(event);
    }

    @KafkaListener(topics = "order.shipped", groupId = "notification-group")
    public void onOrderShipped(OrderPlacedEvent event) {
        log.info("Received order.shipped event for orderId: {}", event.getOrderId());
        notificationService.sendShippingNotification(event);
    }
}