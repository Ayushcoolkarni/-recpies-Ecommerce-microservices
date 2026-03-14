package Ecom.inventory_service.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "products")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String unit;
    private Double pricePerUnit;
    private Integer stockQuantity;
    private String imageUrl;
    private String category;
    private boolean isAvailable;

    @PrePersist
    protected void onCreate() {
        if (isAvailable == false) isAvailable = true;
    }
}