package com.example.admin_service.mapper;

import com.example.admin_service.dto.response.SuggestionReviewResponse;
import com.example.admin_service.entity.SuggestionReview;
import org.springframework.stereotype.Component;

@Component
public class SuggestionReviewMapper {

    public SuggestionReviewResponse toResponse(SuggestionReview review) {
        return SuggestionReviewResponse.builder()
                .id(review.getId())
                .suggestionId(review.getSuggestionId())
                .adminId(review.getAdminId())
                .decision(review.getDecision())
                .notes(review.getNotes())
                .reviewedAt(review.getReviewedAt())
                .build();
    }
}