package Ecom.recipe_service.dto.response;

import Ecom.recipe_service.enums.Category;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder
public class RecipeResponse {
    private Long id;
    private String name;
    private String description;
    private String instructions;
    private String imageUrl;
    private Integer prepTimeMinutes;
    private Integer defaultServings;
    private Category category;
    private LocalDateTime createdAt;
    private List<IngredientResponse> ingredients;
}