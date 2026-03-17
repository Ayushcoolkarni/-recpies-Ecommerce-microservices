package Ecom.recipe_service.service;

import Ecom.recipe_service.dto.request.SuggestionRequest;
import Ecom.recipe_service.dto.response.SuggestionResponse;
import Ecom.recipe_service.enums.SuggestionStatus;
import java.util.List;

public interface SuggestionService {

    SuggestionResponse submitSuggestion(SuggestionRequest request);

    List<SuggestionResponse> getAllSuggestions();

    List<SuggestionResponse> getSuggestionsByUser(Long userId);

    /**
     * Called by admin-service after reviewing a suggestion.
     * Updates the status to APPROVED or REJECTED inside recipe-service
     * so the source of truth stays consistent.
     */
    SuggestionResponse updateStatus(Long id, SuggestionStatus status);
}
