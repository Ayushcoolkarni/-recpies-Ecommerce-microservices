package Ecom.recipe_service.service;

import Ecom.recipe_service.dto.request.RecipeRequest;
import Ecom.recipe_service.dto.response.RecipeResponse;
import java.util.List;

public interface RecipeService {
    RecipeResponse createRecipe(RecipeRequest request);
    RecipeResponse getRecipeById(Long id);
    List<RecipeResponse> getAllRecipes();
    List<RecipeResponse> searchRecipes(String name);
    RecipeResponse updateRecipe(Long id, RecipeRequest request);
    void deleteRecipe(Long id);
}