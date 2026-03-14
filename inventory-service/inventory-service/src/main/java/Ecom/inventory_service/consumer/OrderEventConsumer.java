package Ecom.inventory_service.consumer;

import Ecom.inventory_service.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final ProductService productService;

    @KafkaListener(topics = "order.placed", groupId = "inventory-service")
    public void handleOrderPlaced(Map<String, Object> event) {
        log.info("Received order.placed event: {}", event);

        Object itemsObj = event.get("items");
        if (itemsObj instanceof java.util.List<?> items) {
            for (Object itemObj : items) {
                if (itemObj instanceof Map<?, ?> item) {
                    Long productId = ((Number) item.get("productId")).longValue();
                    Integer quantity = ((Number) item.get("quantity")).intValue();
                    log.info("Deducting {} units from productId: {}", quantity, productId);
                    productService.deductStock(productId, quantity);
                }
            }
        }
    }
}