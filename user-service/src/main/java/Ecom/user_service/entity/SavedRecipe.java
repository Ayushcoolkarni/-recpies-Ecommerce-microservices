package Ecom.user_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "saved_recipes")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SavedRecipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private Long recipeId;
    private String notes;
    private LocalDateTime savedAt;

    @PrePersist
    protected void onCreate() { savedAt = LocalDateTime.now(); }
}