package Ecom.inventory_service.consumer;

import Ecom.inventory_service.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final ProductService productService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Deduct stock only after payment confirmed (order.confirmed).
     * Manual ack: offset is committed ONLY after stock deduction succeeds.
     * If deduction fails, the message is retried (up to 3x) then dead-lettered.
     */
    @KafkaListener(topics = "order.confirmed", groupId = "inventory-service")
    public void handleOrderConfirmed(String message, Acknowledgment ack) {
        try {
            log.info("Received order.confirmed — deducting stock: {}", message);
            Map<String, Object> event = objectMapper.readValue(message, Map.class);
            List<Map<String, Object>> items = (List<Map<String, Object>>) event.get("items");
            if (items != null) {
                for (Map<String, Object> item : items) {
                    Long productId = ((Number) item.get("productId")).longValue();
                    Integer quantity = ((Number) item.get("quantity")).intValue();
                    log.info("Deducting {} units from productId={}", quantity, productId);
                    productService.deductStock(productId, quantity);
                }
            }
            // FIX: only ack after ALL stock deductions succeed
            ack.acknowledge();
            log.info("order.confirmed processed and acknowledged");
        } catch (Exception e) {
            log.error("Failed to process order.confirmed event: {}", e.getMessage(), e);
            // Do NOT ack — error handler will retry then DLT
            throw new RuntimeException("Failed to process order.confirmed", e);
        }
    }

    /**
     * Restore stock when order is cancelled (payment failed).
     * Manual ack: offset committed only after stock is fully restored.
     */
    @KafkaListener(topics = "order.cancelled", groupId = "inventory-service")
    public void handleOrderCancelled(String message, Acknowledgment ack) {
        try {
            log.info("Received order.cancelled — restoring stock: {}", message);
            Map<String, Object> event = objectMapper.readValue(message, Map.class);
            List<Map<String, Object>> items = (List<Map<String, Object>>) event.get("items");
            if (items != null) {
                for (Map<String, Object> item : items) {
                    Long productId = ((Number) item.get("productId")).longValue();
                    Integer quantity = ((Number) item.get("quantity")).intValue();
                    log.info("Restoring {} units to productId={}", quantity, productId);
                    productService.restoreStock(productId, quantity);
                }
            }
            ack.acknowledge();
            log.info("order.cancelled processed and acknowledged");
        } catch (Exception e) {
            log.error("Failed to process order.cancelled event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process order.cancelled", e);
        }
    }
}
