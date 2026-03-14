package com.example.admin_service.repository;

import com.example.admin_service.entity.SuggestionReview;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SuggestionReviewRepository extends JpaRepository<SuggestionReview, Long> {
    List<SuggestionReview> findByAdminId(Long adminId);
    List<SuggestionReview> findBySuggestionId(Long suggestionId);
    List<SuggestionReview> findByDecision(String decision);
}