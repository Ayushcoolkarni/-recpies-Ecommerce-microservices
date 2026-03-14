package Ecom.order_service.dto.response;

import lombok.*;

@Data @Builder
public class OrderItemResponse {
    private Long id;
    private Long productId;
    private String ingredientName;
    private Integer quantity;
    private Double pricePerUnit;
}