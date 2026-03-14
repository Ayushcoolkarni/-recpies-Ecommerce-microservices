package Ecom.inventory_service.repository;

import Ecom.inventory_service.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByCategory(String category);
    List<Product> findByIsAvailableTrue();
    List<Product> findByNameContainingIgnoreCase(String name);
    List<Product> findByStockQuantityLessThan(Integer threshold);
}