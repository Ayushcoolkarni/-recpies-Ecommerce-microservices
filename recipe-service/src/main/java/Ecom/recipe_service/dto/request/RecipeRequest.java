package Ecom.recipe_service.dto.request;

import Ecom.recipe_service.enums.Category;
import lombok.Data;
import java.util.List;

@Data
public class RecipeRequest {
    private String name;
    private String description;
    private String instructions;
    private String imageUrl;
    private Integer prepTimeMinutes;
    private Integer defaultServings;
    private Category category;
    private List<Long> ingredientIds;
    private List<Double> quantities;
}