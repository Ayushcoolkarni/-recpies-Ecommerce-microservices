package Ecom.recipe_service.service;

import Ecom.recipe_service.dto.request.RecipeRequest;
import Ecom.recipe_service.dto.response.RecipeResponse;
import Ecom.recipe_service.dto.response.ScaledRecipeResponse;
import Ecom.recipe_service.enums.Category;
import java.util.List;

public interface RecipeService {

    RecipeResponse createRecipe(RecipeRequest request);

    RecipeResponse getRecipeById(Long id);

    List<RecipeResponse> getAllRecipes();

    List<RecipeResponse> searchRecipes(String name);

    /** Returns recipes filtered by category (e.g. DINNER, BREAKFAST). */
    List<RecipeResponse> getRecipesByCategory(Category category);

    /**
     * Returns the recipe with all ingredient quantities scaled to the
     * requested number of servings, plus live pricing from inventory-service.
     */
    ScaledRecipeResponse getScaledRecipe(Long id, int servings);

    RecipeResponse updateRecipe(Long id, RecipeRequest request);

    void deleteRecipe(Long id);
}
