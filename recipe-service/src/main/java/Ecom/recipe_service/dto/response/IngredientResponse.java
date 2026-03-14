package Ecom.recipe_service.dto.response;

import lombok.*;

@Data @Builder
public class IngredientResponse {
    private Long id;
    private String name;
    private String unit;
    private Long productId;
    private Double quantityPerServing;
}