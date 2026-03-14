package Ecom.inventory_service.mapper;

import Ecom.inventory_service.dto.response.ProductResponse;
import Ecom.inventory_service.entity.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public ProductResponse toResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .unit(product.getUnit())
                .pricePerUnit(product.getPricePerUnit())
                .stockQuantity(product.getStockQuantity())
                .imageUrl(product.getImageUrl())
                .category(product.getCategory())
                .isAvailable(product.isAvailable())
                .build();
    }
}
