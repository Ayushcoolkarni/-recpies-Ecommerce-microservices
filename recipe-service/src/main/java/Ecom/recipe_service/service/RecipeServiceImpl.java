package Ecom.recipe_service.service;

import Ecom.recipe_service.client.InventoryClient;
import Ecom.recipe_service.dto.request.RecipeRequest;
import Ecom.recipe_service.dto.response.*;
import Ecom.recipe_service.entity.*;
import Ecom.recipe_service.enums.Category;
import Ecom.recipe_service.exception.ResourceNotFoundException;
import Ecom.recipe_service.mapper.RecipeMapper;
import Ecom.recipe_service.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecipeServiceImpl implements RecipeService {

    private final RecipeRepository              recipeRepository;
    private final IngredientRepository          ingredientRepository;
    private final RecipeIngredientRepository    recipeIngredientRepository;
    private final RecipeMapper                  recipeMapper;
    private final InventoryClient               inventoryClient;

    // ── CREATE ───────────────────────────────────────────────────

    @Override
    @Transactional
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
                Double qty        = request.getQuantities().get(i);

                Ingredient ingredient = ingredientRepository.findById(ingredientId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Ingredient not found: " + ingredientId));

                recipeIngredientRepository.save(
                        RecipeIngredient.builder()
                                .recipe(recipe)
                                .ingredient(ingredient)
                                .quantityPerServing(qty)
                                .build());
            }
        }

        return recipeMapper.toResponse(
                recipeRepository.findById(recipe.getId())
                        .orElseThrow());
    }

    // ── READ ─────────────────────────────────────────────────────

    @Override
    public RecipeResponse getRecipeById(Long id) {
        return recipeMapper.toResponse(findRecipe(id));
    }

    @Override
    public List<RecipeResponse> getAllRecipes() {
        return recipeRepository.findAll().stream()
                .map(recipeMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<RecipeResponse> searchRecipes(String name) {
        return recipeRepository.findByNameContainingIgnoreCase(name).stream()
                .map(recipeMapper::toResponse)
                .collect(Collectors.toList());
    }

    // ── CATEGORY FILTER ──────────────────────────────────────────

    @Override
    public List<RecipeResponse> getRecipesByCategory(Category category) {
        return recipeRepository.findByCategory(category).stream()
                .map(recipeMapper::toResponse)
                .collect(Collectors.toList());
    }

    // ── SCALED RECIPE ────────────────────────────────────────────

    @Override
    public ScaledRecipeResponse getScaledRecipe(Long id, int servings) {
        if (servings < 1) {
            throw new IllegalArgumentException("servings must be at least 1");
        }

        Recipe recipe = findRecipe(id);

        List<ScaledIngredientResponse> scaledIngredients =
                recipe.getRecipeIngredients().stream()
                        .map(ri -> buildScaledIngredient(ri, servings))
                        .collect(Collectors.toList());

        // Estimated total — null if any price is missing
        Double estimatedTotal = null;
        boolean allPricesAvailable = scaledIngredients.stream()
                .allMatch(i -> i.getLineTotal() != null);

        if (allPricesAvailable) {
            estimatedTotal = round(scaledIngredients.stream()
                    .mapToDouble(ScaledIngredientResponse::getLineTotal)
                    .sum());
        }

        log.info("Scaled recipe id={} for {} servings — estimatedTotal={}", id, servings, estimatedTotal);

        return ScaledRecipeResponse.builder()
                .id(recipe.getId())
                .name(recipe.getName())
                .description(recipe.getDescription())
                .instructions(recipe.getInstructions())
                .imageUrl(recipe.getImageUrl())
                .prepTimeMinutes(recipe.getPrepTimeMinutes())
                .defaultServings(recipe.getDefaultServings())
                .requestedServings(servings)
                .category(recipe.getCategory())
                .ingredients(scaledIngredients)
                .estimatedTotal(estimatedTotal)
                .build();
    }

    // ── UPDATE ───────────────────────────────────────────────────

    @Override
    @Transactional
    public RecipeResponse updateRecipe(Long id, RecipeRequest request) {
        Recipe recipe = findRecipe(id);
        recipe.setName(request.getName());
        recipe.setDescription(request.getDescription());
        recipe.setInstructions(request.getInstructions());
        recipe.setImageUrl(request.getImageUrl());
        recipe.setPrepTimeMinutes(request.getPrepTimeMinutes());
        recipe.setDefaultServings(request.getDefaultServings());
        recipe.setCategory(request.getCategory());
        return recipeMapper.toResponse(recipeRepository.save(recipe));
    }

    // ── DELETE ───────────────────────────────────────────────────

    @Override
    public void deleteRecipe(Long id) {
        recipeRepository.deleteById(id);
        log.info("Recipe {} deleted", id);
    }

    // ── private helpers ──────────────────────────────────────────

    private Recipe findRecipe(Long id) {
        return recipeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe not found: " + id));
    }

    private ScaledIngredientResponse buildScaledIngredient(RecipeIngredient ri, int servings) {
        double scaledQty = round(ri.getQuantityPerServing() * servings);

        // Fetch live price from inventory-service (graceful — null if unavailable)
        Double price     = ri.getIngredient().getProductId() != null
                ? inventoryClient.getPricePerUnit(ri.getIngredient().getProductId())
                : null;
        Double lineTotal = (price != null) ? round(scaledQty * price) : null;

        return ScaledIngredientResponse.builder()
                .id(ri.getIngredient().getId())
                .name(ri.getIngredient().getName())
                .unit(ri.getUnit() != null ? ri.getUnit() : ri.getIngredient().getUnit())
                .productId(ri.getIngredient().getProductId())
                .scaledQuantity(scaledQty)
                .pricePerUnit(price)
                .lineTotal(lineTotal)
                .build();
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
