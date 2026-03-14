package Ecom.recipe_service.dto.response;

import Ecom.recipe_service.enums.SuggestionStatus;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder
public class SuggestionResponse {
    private Long id;
    private Long userId;
    private String recipeName;
    private String ingredients;
    private String description;
    private SuggestionStatus status;
    private LocalDateTime submittedAt;
}