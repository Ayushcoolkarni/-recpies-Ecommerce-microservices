package Ecom.recipe_service.service;

import Ecom.recipe_service.dto.request.SuggestionRequest;
import Ecom.recipe_service.dto.response.SuggestionResponse;
import java.util.List;

public interface SuggestionService {

    SuggestionResponse submitSuggestion(SuggestionRequest request);

    List<SuggestionResponse> getAllSuggestions();

    List<SuggestionResponse> getSuggestionsByUser(Long userId);
}

