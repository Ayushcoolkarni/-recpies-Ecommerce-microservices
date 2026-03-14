package Ecom.recipe_service.entity;

import Ecom.recipe_service.enums.Category;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "recipes")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String instructions;

    private String imageUrl;
    private Integer prepTimeMinutes;
    private Integer defaultServings;

    @Enumerated(EnumType.STRING)
    private Category category;

    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecipeIngredient> recipeIngredients;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}