package Ecom.order_service.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Calls inventory-service to check stock before placing an order.
 *
 * Correct endpoint: GET /products/{id}/in-stock
 * (matches ProductController in inventory-service)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryClient {

    private final RestClient.Builder restClientBuilder;

    public boolean isInStock(Long productId) {
        try {
            Boolean response = restClientBuilder.build()
                    .get()
                    .uri("http://inventory-service/products/{id}/in-stock", productId)
                    .retrieve()
                    .body(Boolean.class);

            log.info("Stock check for productId {} → {}", productId, response);
            return Boolean.TRUE.equals(response);

        } catch (Exception e) {
            log.error("Error calling inventory-service for productId {}: {}", productId, e.getMessage());
            return false;
        }
    }
}
