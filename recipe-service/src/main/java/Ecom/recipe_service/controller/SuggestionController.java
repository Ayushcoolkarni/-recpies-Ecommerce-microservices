package Ecom.recipe_service.controller;

import Ecom.recipe_service.dto.request.SuggestionRequest;
import Ecom.recipe_service.dto.response.SuggestionResponse;
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

    @PostMapping
    public ResponseEntity<SuggestionResponse> submit(@RequestBody SuggestionRequest request) {
        return ResponseEntity.ok(suggestionService.submitSuggestion(request));
    }

    @GetMapping
    public ResponseEntity<List<SuggestionResponse>> getAll() {
        return ResponseEntity.ok(suggestionService.getAllSuggestions());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<SuggestionResponse>> getByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(suggestionService.getSuggestionsByUser(userId));
    }
}