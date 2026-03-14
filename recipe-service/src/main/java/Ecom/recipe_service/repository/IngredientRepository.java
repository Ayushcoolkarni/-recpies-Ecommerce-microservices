package Ecom.recipe_service.repository;

import Ecom.recipe_service.entity.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {}