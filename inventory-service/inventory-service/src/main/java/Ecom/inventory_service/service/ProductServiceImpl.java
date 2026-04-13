package Ecom.inventory_service.service;

import Ecom.inventory_service.dto.request.ProductRequest;
import Ecom.inventory_service.dto.response.ProductResponse;
import Ecom.inventory_service.entity.Product;
import Ecom.inventory_service.exception.ResourceNotFoundException;
import Ecom.inventory_service.mapper.ProductMapper;
import Ecom.inventory_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Override
    public ProductResponse createProduct(ProductRequest request) {

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .unit(request.getUnit())
                .pricePerUnit(request.getPricePerUnit())
                .stockQuantity(request.getStockQuantity())
                .imageUrl(request.getImageUrl())
                .category(request.getCategory())
                .isAvailable(true)
                .build();

        Product saved = productRepository.save(product);

        log.info("Product created with id: {}", saved.getId());

        return productMapper.toResponse(saved);
    }

    @Override
    public ProductResponse getProductById(Long id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product not found with id: " + id));

        return productMapper.toResponse(product);
    }

    @Override
    public List<ProductResponse> getAllProducts() {

        return productRepository.findAll()
                .stream()
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductResponse> getAvailableProducts() {

        return productRepository.findByIsAvailableTrue()
                .stream()
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ProductResponse updateProduct(Long id, ProductRequest request) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product not found with id: " + id));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPricePerUnit(request.getPricePerUnit());
        product.setStockQuantity(request.getStockQuantity());

        Product updated = productRepository.save(product);

        log.info("Product updated: {}", updated.getId());

        return productMapper.toResponse(updated);
    }

    @Override
    public ProductResponse updateStock(Long id, Integer quantity) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product not found with id: " + id));

        product.setStockQuantity(quantity);
        product.setAvailable(quantity > 0);

        Product updated = productRepository.save(product);

        log.info("Stock updated for productId {} → {}", id, quantity);

        return productMapper.toResponse(updated);
    }

    @Override
    public void deductStock(Long productId, Integer quantity) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product not found with id: " + productId));

        int newStock = product.getStockQuantity() - quantity;

        if (newStock < 0) {
            log.warn("Insufficient stock for productId {}", productId);
            newStock = 0;
        }

        product.setStockQuantity(newStock);
        product.setAvailable(newStock > 0);

        productRepository.save(product);

        log.info("Stock deducted for productId {} → remaining {}", productId, newStock);
    }

    @Override
    public boolean isInStock(Long productId) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product not found with id: " + productId));

        return product.getStockQuantity() > 0;
    }

    @Override
    public void restoreStock(Long productId, Integer quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product not found with id: " + productId));

        int newStock = product.getStockQuantity() + quantity;
        product.setStockQuantity(newStock);
        product.setAvailable(newStock > 0);
        productRepository.save(product);
        log.info("Stock restored for productId {} + {} → total {}", productId, quantity, newStock);
    }

    @Override
    public void deleteProduct(Long id) {

        productRepository.deleteById(id);

        log.info("Product deleted with id: {}", id);
    }
}