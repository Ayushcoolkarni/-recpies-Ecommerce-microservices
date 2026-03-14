package Ecom.recipe_service.mapper;

import Ecom.recipe_service.dto.response.SuggestionResponse;
import Ecom.recipe_service.entity.RecipeSuggestion;
import org.springframework.stereotype.Component;

@Component
public class SuggestionMapper {

    public SuggestionResponse toResponse(RecipeSuggestion suggestion) {

        return SuggestionResponse.builder()
                .id(suggestion.getId())
                .userId(suggestion.getUserId())
                .recipeName(suggestion.getRecipeName())
                .ingredients(suggestion.getIngredients())
                .description(suggestion.getDescription())
                .status(suggestion.getStatus())
                .submittedAt(suggestion.getSubmittedAt())
                .build();
    }
}

