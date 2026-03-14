package Ecom.recipe_service.service;

import Ecom.recipe_service.dto.request.SuggestionRequest;
import Ecom.recipe_service.dto.response.SuggestionResponse;
import Ecom.recipe_service.entity.RecipeSuggestion;
import Ecom.recipe_service.enums.SuggestionStatus;
import Ecom.recipe_service.mapper.SuggestionMapper;
import Ecom.recipe_service.repository.RecipeSuggestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SuggestionServiceImpl implements SuggestionService {

    private final RecipeSuggestionRepository suggestionRepository;
    private final SuggestionMapper suggestionMapper;

    @Override
    public SuggestionResponse submitSuggestion(SuggestionRequest request) {

        RecipeSuggestion suggestion = RecipeSuggestion.builder()
                .userId(request.getUserId())
                .recipeName(request.getRecipeName())
                .description(request.getDescription())
                .status(SuggestionStatus.PENDING)
                .build();

        return suggestionMapper.toResponse(
                suggestionRepository.save(suggestion)
        );
    }

    @Override
    public List<SuggestionResponse> getAllSuggestions() {
        return suggestionRepository.findAll()
                .stream()
                .map(suggestionMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<SuggestionResponse> getSuggestionsByUser(Long userId) {
        return suggestionRepository.findByUserId(userId)
                .stream()
                .map(suggestionMapper::toResponse)
                .collect(Collectors.toList());
    }
}

