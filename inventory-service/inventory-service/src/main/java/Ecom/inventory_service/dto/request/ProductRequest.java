package Ecom.inventory_service.dto.request;

import lombok.Data;

@Data
public class ProductRequest {
    private String name;
    private String description;
    private String unit;
    private Double pricePerUnit;
    private Integer stockQuantity;
    private String imageUrl;
    private String category;
}