package com.example.admin_service.dto.request;

import lombok.Data;

@Data
public class SuggestionReviewRequest {
    private Long suggestionId;
    private Long adminId;
    private String decision;
    private String notes;
}