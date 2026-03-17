package Ecom.recipe_service.controller;

import Ecom.recipe_service.dto.request.SuggestionRequest;
import Ecom.recipe_service.dto.response.SuggestionResponse;
import Ecom.recipe_service.enums.SuggestionStatus;
import Ecom.recipe_service.service.SuggestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/suggestions")
@RequiredArgsConstructor
public class SuggestionController {

    private final SuggestionService suggestionService;

    /** POST /suggestions — user submits a new recipe suggestion */
    @PostMapping
    public ResponseEntity<SuggestionResponse> submit(@RequestBody SuggestionRequest request) {
        return ResponseEntity.ok(suggestionService.submitSuggestion(request));
    }

    /** GET /suggestions — admin lists all suggestions */
    @GetMapping
    public ResponseEntity<List<SuggestionResponse>> getAll() {
        return ResponseEntity.ok(suggestionService.getAllSuggestions());
    }

    /** GET /suggestions/user/{userId} — user views their own submissions */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<SuggestionResponse>> getByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(suggestionService.getSuggestionsByUser(userId));
    }

    /**
     * PATCH /suggestions/{id}/status?status=APPROVED
     * Called by admin-service after reviewing a suggestion so the status
     * in recipe-service stays in sync with the admin decision.
     *
     * Valid values: PENDING, APPROVED, REJECTED
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<SuggestionResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam SuggestionStatus status) {
        return ResponseEntity.ok(suggestionService.updateStatus(id, status));
    }
}
