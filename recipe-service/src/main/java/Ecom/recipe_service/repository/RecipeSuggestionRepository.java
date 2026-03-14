package Ecom.recipe_service.repository;

import Ecom.recipe_service.entity.RecipeSuggestion;
import Ecom.recipe_service.enums.SuggestionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RecipeSuggestionRepository extends JpaRepository<RecipeSuggestion, Long> {
    List<RecipeSuggestion> findByStatus(SuggestionStatus status);
    List<RecipeSuggestion> findByUserId(Long userId);
}