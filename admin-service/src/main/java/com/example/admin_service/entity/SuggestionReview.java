package com.example.admin_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "suggestion_reviews")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SuggestionReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long suggestionId;
    private Long adminId;
    private String decision;
    private String notes;
    private LocalDateTime reviewedAt;

    @PrePersist
    protected void onCreate() { reviewedAt = LocalDateTime.now(); }
}