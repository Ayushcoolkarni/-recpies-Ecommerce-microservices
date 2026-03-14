package Ecom.inventory_service.dto.response;

import lombok.*;

@Data @Builder
public class ProductResponse {
    private Long id;
    private String name;
    private String description;
    private String unit;
    private Double pricePerUnit;
    private Integer stockQuantity;
    private String imageUrl;
    private String category;
    private boolean isAvailable;
}