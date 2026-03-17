package Ecom.recipe_service.dto.response;

import lombok.*;

@Data @Builder
public class ScaledIngredientResponse {
    private Long id;
    private String name;
    private String unit;
    private Long productId;

    /** quantityPerServing × requestedServings */
    private Double scaledQuantity;

    /** Live price from inventory-service (null if unavailable) */
    private Double pricePerUnit;

    /** scaledQuantity × pricePerUnit (null if price unavailable) */
    private Double lineTotal;
}
