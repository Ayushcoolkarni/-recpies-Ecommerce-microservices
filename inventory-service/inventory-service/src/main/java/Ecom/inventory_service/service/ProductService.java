package Ecom.inventory_service.service;

import Ecom.inventory_service.dto.request.ProductRequest;
import Ecom.inventory_service.dto.response.ProductResponse;
import java.util.List;

public interface ProductService {
    ProductResponse createProduct(ProductRequest request);
    ProductResponse getProductById(Long id);
    List<ProductResponse> getAllProducts();
    List<ProductResponse> getAvailableProducts();
    ProductResponse updateProduct(Long id, ProductRequest request);
    ProductResponse updateStock(Long id, Integer quantity);
    void deleteProduct(Long id);

    void restoreStock(Long productId, Integer quantity);
    void deductStock(Long productId, Integer quantity);


    boolean isInStock(Long productId);
}