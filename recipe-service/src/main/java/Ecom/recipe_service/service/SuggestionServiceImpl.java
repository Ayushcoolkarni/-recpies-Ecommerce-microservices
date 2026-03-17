package Ecom.recipe_service.service;

import Ecom.recipe_service.dto.request.SuggestionRequest;
import Ecom.recipe_service.dto.response.SuggestionResponse;
import Ecom.recipe_service.entity.RecipeSuggestion;
import Ecom.recipe_service.enums.SuggestionStatus;
import Ecom.recipe_service.exception.ResourceNotFoundException;
import Ecom.recipe_service.mapper.SuggestionMapper;
import Ecom.recipe_service.repository.RecipeSuggestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SuggestionServiceImpl implements SuggestionService {

    private final RecipeSuggestionRepository suggestionRepository;
    private final SuggestionMapper           suggestionMapper;

    // ── SUBMIT ───────────────────────────────────────────────────

    @Override
    public SuggestionResponse submitSuggestion(SuggestionRequest request) {
        RecipeSuggestion suggestion = RecipeSuggestion.builder()
                .userId(request.getUserId())
                .recipeName(request.getRecipeName())
                .ingredients(request.getIngredients())
                .description(request.getDescription())
                .status(SuggestionStatus.PENDING)
                .build();

        log.info("Suggestion submitted by userId={} for recipe '{}'",
                request.getUserId(), request.getRecipeName());

        return suggestionMapper.toResponse(suggestionRepository.save(suggestion));
    }

    // ── READ ─────────────────────────────────────────────────────

    @Override
    public List<SuggestionResponse> getAllSuggestions() {
        return suggestionRepository.findAll().stream()
                .map(suggestionMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<SuggestionResponse> getSuggestionsByUser(Long userId) {
        return suggestionRepository.findByUserId(userId).stream()
                .map(suggestionMapper::toResponse)
                .collect(Collectors.toList());
    }

    // ── UPDATE STATUS ─────────────────────────────────────────────

    @Override
    @Transactional
    public SuggestionResponse updateStatus(Long id, SuggestionStatus status) {
        RecipeSuggestion suggestion = suggestionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Suggestion not found: " + id));

        suggestion.setStatus(status);
        RecipeSuggestion saved = suggestionRepository.save(suggestion);

        log.info("Suggestion {} status updated to {}", id, status);
        return suggestionMapper.toResponse(saved);
    }
}
