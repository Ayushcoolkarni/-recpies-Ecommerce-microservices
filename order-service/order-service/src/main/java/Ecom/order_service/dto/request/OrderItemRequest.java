package Ecom.order_service.dto.request;

import lombok.Data;

@Data
public class OrderItemRequest {
    private Long productId;
    private String ingredientName;
    private Integer quantity;
    private Double pricePerUnit;
}