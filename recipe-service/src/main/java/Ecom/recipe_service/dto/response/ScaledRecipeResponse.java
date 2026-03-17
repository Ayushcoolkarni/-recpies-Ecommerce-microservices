package Ecom.recipe_service.dto.response;

import Ecom.recipe_service.enums.Category;
import lombok.*;
import java.util.List;

@Data @Builder
public class ScaledRecipeResponse {
    private Long id;
    private String name;
    private String description;
    private String instructions;
    private String imageUrl;
    private Integer prepTimeMinutes;
    private Integer defaultServings;
    private Integer requestedServings;
    private Category category;

    /** All ingredients with quantities already multiplied by requestedServings */
    private List<ScaledIngredientResponse> ingredients;

    /**
     * Sum of all lineTotals — estimated cost to add this recipe to cart.
     * Null if any ingredient price is unavailable from inventory-service.
     */
    private Double estimatedTotal;
}
