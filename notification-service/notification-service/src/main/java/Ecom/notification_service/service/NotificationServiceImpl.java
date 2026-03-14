package Ecom.notification_service.service;

import Ecom.notification_service.dto.OrderPlacedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    @Override
    public void sendOrderConfirmation(OrderPlacedEvent event) {
        // In production: integrate with SendGrid / JavaMailSender
        log.info("Sending ORDER CONFIRMATION to userId: {} for orderId: {} | Amount: {}",
                event.getUserId(), event.getOrderId(), event.getTotalAmount());
    }

    @Override
    public void sendShippingNotification(OrderPlacedEvent event) {
        log.info("Sending SHIPPING NOTIFICATION to userId: {} for orderId: {}",
                event.getUserId(), event.getOrderId());
    }
}