package Ecom.user_service.repository;

import Ecom.user_service.entity.SavedRecipe;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SavedRecipeRepository extends JpaRepository<SavedRecipe, Long> {
    List<SavedRecipe> findByUserId(Long userId);
    void deleteByUserIdAndRecipeId(Long userId, Long recipeId);
}