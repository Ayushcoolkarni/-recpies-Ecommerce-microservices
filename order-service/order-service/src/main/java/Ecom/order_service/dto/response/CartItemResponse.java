package Ecom.order_service.dto.response;

import lombok.*;

@Data @Builder
public class CartItemResponse {
    private Long id;
    private Long productId;
    private String ingredientName;
    private Integer quantity;
    private Double pricePerUnit;
    private Double lineTotal;       // quantity × pricePerUnit
}
