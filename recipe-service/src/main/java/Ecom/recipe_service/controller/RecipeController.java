package Ecom.recipe_service.controller;

import Ecom.recipe_service.dto.request.RecipeRequest;
import Ecom.recipe_service.dto.response.RecipeResponse;
import Ecom.recipe_service.dto.response.ScaledRecipeResponse;
import Ecom.recipe_service.enums.Category;
import Ecom.recipe_service.service.RecipeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/recipes")
@RequiredArgsConstructor
public class RecipeController {

    private final RecipeService recipeService;

    /** POST /recipes — create a recipe with linked ingredients */
    @PostMapping
    public ResponseEntity<RecipeResponse> create(@RequestBody RecipeRequest request) {
        return ResponseEntity.ok(recipeService.createRecipe(request));
    }

    /** GET /recipes/{id} — get recipe with default servings */
    @GetMapping("/{id}")
    public ResponseEntity<RecipeResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(recipeService.getRecipeById(id));
    }

    /**
     * GET /recipes/{id}/scaled?servings=4
     * Returns the recipe with ingredient quantities scaled to the requested
     * number of servings, plus live prices from inventory-service.
     *
     * Example response per ingredient:
     * {
     *   "name": "Tomato",
     *   "scaledQuantity": 8.0,
     *   "unit": "pcs",
     *   "pricePerUnit": 2.50,
     *   "lineTotal": 20.00
     * }
     */
    @GetMapping("/{id}/scaled")
    public ResponseEntity<ScaledRecipeResponse> getScaled(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int servings) {
        return ResponseEntity.ok(recipeService.getScaledRecipe(id, servings));
    }

    /**
     * GET /recipes — list all recipes
     * GET /recipes?category=DINNER — filter by category
     *
     * Valid categories: BREAKFAST, LUNCH, DINNER, SNACK, DESSERT, BEVERAGE
     */
    @GetMapping
    public ResponseEntity<List<RecipeResponse>> getAll(
            @RequestParam(required = false) Category category) {
        if (category != null) {
            return ResponseEntity.ok(recipeService.getRecipesByCategory(category));
        }
        return ResponseEntity.ok(recipeService.getAllRecipes());
    }

    /**
     * GET /recipes/search?name=pasta
     * Case-insensitive name search.
     */
    @GetMapping("/search")
    public ResponseEntity<List<RecipeResponse>> search(@RequestParam String name) {
        return ResponseEntity.ok(recipeService.searchRecipes(name));
    }

    /** PUT /recipes/{id} — full update */
    @PutMapping("/{id}")
    public ResponseEntity<RecipeResponse> update(
            @PathVariable Long id,
            @RequestBody RecipeRequest request) {
        return ResponseEntity.ok(recipeService.updateRecipe(id, request));
    }

    /** DELETE /recipes/{id} */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        recipeService.deleteRecipe(id);
        return ResponseEntity.noContent().build();
    }
}
