package Ecom.notification_service.service;

import Ecom.notification_service.client.UserServiceClient;
import Ecom.notification_service.dto.OrderPlacedEvent;
import Ecom.notification_service.template.EmailTemplates;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final EmailService      emailService;
    private final UserServiceClient userServiceClient;

    // ── Order Confirmation ────────────────────────────────────────

    @Override
    public void sendOrderConfirmation(OrderPlacedEvent event) {
        String email = resolveEmail(event);
        if (email == null) return;

        emailService.sendHtml(
                email,
                "Order Confirmed – #" + event.getOrderId(),
                EmailTemplates.buildOrderConfirmation(event.getOrderId(), event.getTotalAmount())
        );

        log.info("Order confirmation sent → userId={} orderId={}",
                event.getUserId(), event.getOrderId());
    }

    // ── Shipping Notification ─────────────────────────────────────

    @Override
    public void sendShippingNotification(OrderPlacedEvent event) {
        String email = resolveEmail(event);
        if (email == null) return;

        emailService.sendHtml(
                email,
                "Your Order #" + event.getOrderId() + " has been Shipped!",
                EmailTemplates.buildShippingNotification(event.getOrderId())
        );

        log.info("Shipping notification sent → userId={} orderId={}",
                event.getUserId(), event.getOrderId());
    }

    // ── Delivery Notification ─────────────────────────────────────

    @Override
    public void sendDeliveryNotification(OrderPlacedEvent event) {
        String email = resolveEmail(event);
        if (email == null) return;

        emailService.sendHtml(
                email,
                "Your Order #" + event.getOrderId() + " has been Delivered!",
                EmailTemplates.buildDeliveryNotification(event.getOrderId())
        );

        log.info("Delivery notification sent → userId={} orderId={}",
                event.getUserId(), event.getOrderId());
    }

    // ── private helpers ───────────────────────────────────────────

    /**
     * Resolves the recipient email address.
     * First checks if the event already carries an email (future proofing),
     * then falls back to calling user-service.
     * Returns null if email cannot be resolved — caller skips sending.
     */
    private String resolveEmail(OrderPlacedEvent event) {
        // Use email from event if already present
        if (event.getUserEmail() != null && !event.getUserEmail().isBlank()) {
            return event.getUserEmail();
        }

        // Fetch from user-service
        String email = userServiceClient.getEmailByUserId(event.getUserId());

        if (email == null) {
            log.warn("Cannot send notification — no email found for userId={}",
                    event.getUserId());
        }

        return email;
    }
}
