package Ecom.recipe_service.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Calls inventory-service to fetch the live price for an ingredient.
 * Gracefully returns null if the service is unreachable — the scaled
 * recipe response will still work, just without price data.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryClient {

    private final RestClient.Builder restClientBuilder;

    /**
     * GET /products/{id}
     * Returns pricePerUnit or null if unavailable.
     */
    public Double getPricePerUnit(Long productId) {
        try {
            Map<?, ?> response = restClientBuilder.build()
                    .get()
                    .uri("http://inventory-service/products/{id}", productId)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.get("pricePerUnit") != null) {
                return ((Number) response.get("pricePerUnit")).doubleValue();
            }
        } catch (Exception e) {
            log.warn("Could not fetch price for productId={}: {}", productId, e.getMessage());
        }
        return null;
    }
}
