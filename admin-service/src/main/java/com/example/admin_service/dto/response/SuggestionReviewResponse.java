package com.example.admin_service.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Data @Builder
public class SuggestionReviewResponse {
    private Long id;
    private Long suggestionId;
    private Long adminId;
    private String decision;
    private String notes;
    private LocalDateTime reviewedAt;
}