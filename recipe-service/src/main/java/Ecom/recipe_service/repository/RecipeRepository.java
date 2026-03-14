package Ecom.recipe_service.repository;

import Ecom.recipe_service.entity.Recipe;
import Ecom.recipe_service.enums.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {
    List<Recipe> findByCategory(Category category);
    List<Recipe> findByNameContainingIgnoreCase(String name);
}