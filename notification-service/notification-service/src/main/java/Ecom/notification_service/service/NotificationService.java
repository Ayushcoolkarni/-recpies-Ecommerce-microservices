package Ecom.notification_service.service;

import Ecom.notification_service.dto.OrderPlacedEvent;

public interface NotificationService {

    /** Triggered by order.placed Kafka topic */
    void sendOrderConfirmation(OrderPlacedEvent event);

    /** Triggered by order.shipped Kafka topic */
    void sendShippingNotification(OrderPlacedEvent event);

    /** Triggered by order.delivered Kafka topic */
    void sendDeliveryNotification(OrderPlacedEvent event);
}
