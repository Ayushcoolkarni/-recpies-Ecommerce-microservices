package Ecom.recipe_service.service;

import Ecom.recipe_service.dto.request.RecipeRequest;
import Ecom.recipe_service.dto.response.*;
import Ecom.recipe_service.entity.*;
import Ecom.recipe_service.exception.ResourceNotFoundException;
import Ecom.recipe_service.mapper.RecipeMapper;
import Ecom.recipe_service.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecipeServiceImpl implements RecipeService {

    private final RecipeRepository recipeRepository;
    private final IngredientRepository ingredientRepository;
    private final RecipeIngredientRepository recipeIngredientRepository;
    private final RecipeMapper recipeMapper;

    @Override
    public RecipeResponse createRecipe(RecipeRequest request) {
        Recipe recipe = Recipe.builder()
                .name(request.getName())
                .description(request.getDescription())
                .instructions(request.getInstructions())
                .imageUrl(request.getImageUrl())
                .prepTimeMinutes(request.getPrepTimeMinutes())
                .defaultServings(request.getDefaultServings())
                .category(request.getCategory())
                .build();
        recipe = recipeRepository.save(recipe);

        if (request.getIngredientIds() != null) {
            for (int i = 0; i < request.getIngredientIds().size(); i++) {
                Long ingredientId = request.getIngredientIds().get(i);
                Double qty = request.getQuantities().get(i);
                Ingredient ingredient = ingredientRepository.findById(ingredientId)
                        .orElseThrow(() -> new ResourceNotFoundException("Ingredient not found"));
                RecipeIngredient ri = RecipeIngredient.builder()
                        .recipe(recipe).ingredient(ingredient)
                        .quantityPerServing(qty).build();
                recipeIngredientRepository.save(ri);
            }
        }
        return recipeMapper.toResponse(recipeRepository.findById(recipe.getId()).get());
    }

    @Override
    public RecipeResponse getRecipeById(Long id) {
        return recipeMapper.toResponse(recipeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe not found")));
    }

    @Override
    public List<RecipeResponse> getAllRecipes() {
        return recipeRepository.findAll().stream()
                .map(recipeMapper::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<RecipeResponse> searchRecipes(String name) {
        return recipeRepository.findByNameContainingIgnoreCase(name).stream()
                .map(recipeMapper::toResponse).collect(Collectors.toList());
    }

    @Override
    public RecipeResponse updateRecipe(Long id, RecipeRequest request) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe not found"));
        recipe.setName(request.getName());
        recipe.setDescription(request.getDescription());
        recipe.setInstructions(request.getInstructions());
        recipe.setCategory(request.getCategory());
        return recipeMapper.toResponse(recipeRepository.save(recipe));
    }

    @Override
    public void deleteRecipe(Long id) {
        recipeRepository.deleteById(id);
    }
}