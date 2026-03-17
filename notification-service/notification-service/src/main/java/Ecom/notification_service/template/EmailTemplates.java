package Ecom.notification_service.template;

/**
 * Simple inline HTML email templates for all three order notification types.
 * Call the static build* methods to get a ready-to-send HTML string.
 */
public final class EmailTemplates {

    private EmailTemplates() {}

    // ── Order Confirmation ────────────────────────────────────────

    public static String buildOrderConfirmation(Long orderId, Double totalAmount) {
        return """
                <html>
                <body style="font-family: Arial, sans-serif; color: #333; max-width: 600px; margin: auto;">
                  <div style="background: #f9f5f0; padding: 24px; border-radius: 8px;">
                    <h2 style="color: #4a3728;">🛒 Order Confirmed!</h2>
                    <p>Thank you for your order. We've received it and it's being prepared.</p>
                    <table style="width:100%%; border-collapse:collapse; margin: 16px 0;">
                      <tr>
                        <td style="padding: 8px; background:#fff; border-radius:4px;"><strong>Order ID</strong></td>
                        <td style="padding: 8px; background:#fff;">#%d</td>
                      </tr>
                      <tr>
                        <td style="padding: 8px;"><strong>Total Amount</strong></td>
                        <td style="padding: 8px;">₹%.2f</td>
                      </tr>
                      <tr>
                        <td style="padding: 8px; background:#fff;"><strong>Status</strong></td>
                        <td style="padding: 8px; background:#fff; color:#2e7d32;"><strong>CONFIRMED</strong></td>
                      </tr>
                    </table>
                    <p style="color:#888; font-size:13px;">
                      You'll receive another email when your order is shipped.
                    </p>
                    <hr style="border:none; border-top:1px solid #e0d5c9; margin: 16px 0;">
                    <p style="font-size:12px; color:#aaa;">RecipeEcom · Fresh ingredients, great recipes</p>
                  </div>
                </body>
                </html>
                """.formatted(orderId, totalAmount);
    }

    // ── Shipping Notification ─────────────────────────────────────

    public static String buildShippingNotification(Long orderId) {
        return """
                <html>
                <body style="font-family: Arial, sans-serif; color: #333; max-width: 600px; margin: auto;">
                  <div style="background: #eaf4fb; padding: 24px; border-radius: 8px;">
                    <h2 style="color: #1565c0;">🚚 Your Order is on the Way!</h2>
                    <p>Great news — your order has been shipped and is heading your way.</p>
                    <table style="width:100%%; border-collapse:collapse; margin: 16px 0;">
                      <tr>
                        <td style="padding: 8px; background:#fff; border-radius:4px;"><strong>Order ID</strong></td>
                        <td style="padding: 8px; background:#fff;">#%d</td>
                      </tr>
                      <tr>
                        <td style="padding: 8px;"><strong>Status</strong></td>
                        <td style="padding: 8px; color:#1565c0;"><strong>SHIPPED</strong></td>
                      </tr>
                    </table>
                    <p>You can track your order status anytime in the app.</p>
                    <hr style="border:none; border-top:1px solid #cde; margin: 16px 0;">
                    <p style="font-size:12px; color:#aaa;">RecipeEcom · Fresh ingredients, great recipes</p>
                  </div>
                </body>
                </html>
                """.formatted(orderId);
    }

    // ── Delivery Notification ─────────────────────────────────────

    public static String buildDeliveryNotification(Long orderId) {
        return """
                <html>
                <body style="font-family: Arial, sans-serif; color: #333; max-width: 600px; margin: auto;">
                  <div style="background: #e8f5e9; padding: 24px; border-radius: 8px;">
                    <h2 style="color: #2e7d32;">✅ Order Delivered!</h2>
                    <p>Your ingredients have been delivered. Time to cook something amazing!</p>
                    <table style="width:100%%; border-collapse:collapse; margin: 16px 0;">
                      <tr>
                        <td style="padding: 8px; background:#fff; border-radius:4px;"><strong>Order ID</strong></td>
                        <td style="padding: 8px; background:#fff;">#%d</td>
                      </tr>
                      <tr>
                        <td style="padding: 8px;"><strong>Status</strong></td>
                        <td style="padding: 8px; color:#2e7d32;"><strong>DELIVERED</strong></td>
                      </tr>
                    </table>
                    <p>Enjoyed your experience? Rate your order in the app!</p>
                    <hr style="border:none; border-top:1px solid #c8e6c9; margin: 16px 0;">
                    <p style="font-size:12px; color:#aaa;">RecipeEcom · Fresh ingredients, great recipes</p>
                  </div>
                </body>
                </html>
                """.formatted(orderId);
    }
}
