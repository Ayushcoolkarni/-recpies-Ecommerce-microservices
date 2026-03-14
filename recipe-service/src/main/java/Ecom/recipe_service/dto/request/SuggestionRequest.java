package Ecom.recipe_service.dto.request;

import lombok.Data;

@Data
public class SuggestionRequest {
    private Long userId;
    private String recipeName;
    private String ingredients;
    private String description;
}