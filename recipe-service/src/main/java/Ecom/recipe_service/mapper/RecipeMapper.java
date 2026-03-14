package Ecom.recipe_service.mapper;

import Ecom.recipe_service.dto.response.*;
import Ecom.recipe_service.entity.*;
import org.springframework.stereotype.Component;
import java.util.stream.Collectors;

@Component
public class RecipeMapper {

    public RecipeResponse toResponse(Recipe recipe) {
        return RecipeResponse.builder()
                .id(recipe.getId())
                .name(recipe.getName())
                .description(recipe.getDescription())
                .instructions(recipe.getInstructions())
                .imageUrl(recipe.getImageUrl())
                .prepTimeMinutes(recipe.getPrepTimeMinutes())
                .defaultServings(recipe.getDefaultServings())
                .category(recipe.getCategory())
                .createdAt(recipe.getCreatedAt())
                .ingredients(recipe.getRecipeIngredients() != null
                        ? recipe.getRecipeIngredients().stream()
                        .map(this::toIngredientResponse)
                        .collect(Collectors.toList())
                        : null)
                .build();
    }

    private IngredientResponse toIngredientResponse(RecipeIngredient ri) {
        return IngredientResponse.builder()
                .id(ri.getIngredient().getId())
                .name(ri.getIngredient().getName())
                .unit(ri.getUnit() != null ? ri.getUnit() : ri.getIngredient().getUnit())
                .productId(ri.getIngredient().getProductId())
                .quantityPerServing(ri.getQuantityPerServing())
                .build();
    }
}