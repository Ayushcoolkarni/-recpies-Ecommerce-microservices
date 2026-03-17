package Ecom.order_service.dto.response;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder
public class CartResponse {
    private Long cartId;
    private Long userId;
    private List<CartItemResponse> items;
    private Double subtotal;        // sum of all lineTotals
    private Double estimatedTax;    // subtotal × 8%
    private Double total;           // subtotal + estimatedTax
    private LocalDateTime createdAt;
}
