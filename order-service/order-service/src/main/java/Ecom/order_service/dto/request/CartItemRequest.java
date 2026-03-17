package Ecom.order_service.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CartItemRequest {

    @NotNull(message = "productId is required")
    private Long productId;

    @NotBlank(message = "ingredientName is required")
    private String ingredientName;

    @NotNull
    @Min(value = 1, message = "quantity must be at least 1")
    private Integer quantity;

    @NotNull(message = "pricePerUnit is required")
    private Double pricePerUnit;
}
