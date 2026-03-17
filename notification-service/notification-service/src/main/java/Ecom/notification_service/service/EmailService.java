package Ecom.notification_service.service;

public interface EmailService {

    /**
     * Sends an HTML email.
     *
     * @param to      recipient email address
     * @param subject email subject line
     * @param htmlBody HTML content of the email body
     */
    void sendHtml(String to, String subject, String htmlBody);
}
