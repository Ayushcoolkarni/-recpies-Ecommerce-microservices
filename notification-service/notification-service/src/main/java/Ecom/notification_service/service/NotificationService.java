package Ecom.notification_service.service;

import Ecom.notification_service.dto.OrderPlacedEvent;

public interface NotificationService {
    void sendOrderConfirmation(OrderPlacedEvent event);
    void sendShippingNotification(OrderPlacedEvent event);
    void sendDeliveryNotification(OrderPlacedEvent event);
    void sendPaymentConfirmedNotification(OrderPlacedEvent event);
    void sendOrderCancelledNotification(OrderPlacedEvent event);
}
