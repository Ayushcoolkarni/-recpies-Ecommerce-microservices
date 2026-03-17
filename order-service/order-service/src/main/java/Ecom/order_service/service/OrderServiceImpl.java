package Ecom.order_service.service;

import Ecom.order_service.client.InventoryClient;
import Ecom.order_service.dto.request.OrderRequest;
import Ecom.order_service.dto.response.OrderResponse;
import Ecom.order_service.dto.response.OrderTrackingResponse;
import Ecom.order_service.entity.Order;
import Ecom.order_service.entity.OrderItem;
import Ecom.order_service.entity.OrderStatusHistory;
import Ecom.order_service.enums.OrderStatus;
import Ecom.order_service.event.OrderPlacedEvent;
import Ecom.order_service.exception.ResourceNotFoundException;
import Ecom.order_service.mapper.OrderMapper;
import Ecom.order_service.repository.OrderRepository;
import Ecom.order_service.repository.OrderStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final OrderMapper orderMapper;
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;
    private final InventoryClient inventoryClient;

    // ── PLACE ORDER ──────────────────────────────────────────────

    @Override
    @Transactional
    public OrderResponse placeOrder(OrderRequest request) {

        // 1. Stock-check every item first
        for (var item : request.getItems()) {
            if (!inventoryClient.isInStock(item.getProductId())) {
                throw new RuntimeException(
                        "Product out of stock: " + item.getProductId()
                                + " (" + item.getIngredientName() + ")"
                );
            }
        }

        // 2. Build order items
        List<OrderItem> items = request.getItems().stream()
                .map(i -> OrderItem.builder()
                        .productId(i.getProductId())
                        .ingredientName(i.getIngredientName())
                        .quantity(i.getQuantity())
                        .pricePerUnit(i.getPricePerUnit())
                        .build())
                .collect(Collectors.toList());

        double total = items.stream()
                .mapToDouble(i -> i.getPricePerUnit() * i.getQuantity())
                .sum();

        // 3. Persist order
        Order order = Order.builder()
                .userId(request.getUserId())
                .addressId(request.getAddressId())
                .totalAmount(total)
                .status(OrderStatus.PENDING)
                .build();

        items.forEach(i -> i.setOrder(order));
        order.setOrderItems(items);

        Order saved = orderRepository.save(order);

        // 4. Record initial history entry
        saveHistory(saved.getOrderId(), OrderStatus.PENDING, "Order placed successfully");

        // 5. Publish order.placed event (triggers inventory deduction + notification)
        kafkaTemplate.send("order.placed", buildEvent(saved));
        log.info("order.placed published — orderId={}", saved.getOrderId());

        return orderMapper.toResponse(saved);
    }

    // ── GET ──────────────────────────────────────────────────────

    @Override
    public OrderResponse getOrderById(Long id) {
        return orderMapper.toResponse(findOrder(id));
    }

    @Override
    public List<OrderResponse> getOrdersByUser(Long userId) {
        return orderRepository.findByUserId(userId).stream()
                .map(orderMapper::toResponse)
                .collect(Collectors.toList());
    }

    // ── UPDATE STATUS ────────────────────────────────────────────

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long id, OrderStatus status) {
        Order order = findOrder(id);
        order.setStatus(status);
        Order saved = orderRepository.save(order);

        // Record every status change in history
        saveHistory(saved.getOrderId(), status, "Status updated to " + status);

        // Publish topic-specific events
        if (status == OrderStatus.SHIPPED) {
            kafkaTemplate.send("order.shipped", buildEvent(saved));
            log.info("order.shipped published — orderId={}", saved.getOrderId());
        }

        if (status == OrderStatus.DELIVERED) {
            kafkaTemplate.send("order.delivered", buildEvent(saved));
            log.info("order.delivered published — orderId={}", saved.getOrderId());
        }

        return orderMapper.toResponse(saved);
    }

    // ── CANCEL ───────────────────────────────────────────────────

    @Override
    @Transactional
    public void cancelOrder(Long id) {
        Order order = findOrder(id);
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        saveHistory(id, OrderStatus.CANCELLED, "Order cancelled by user");
        log.info("Order {} cancelled", id);
    }

    // ── TRACKING ─────────────────────────────────────────────────

    @Override
    public OrderTrackingResponse getOrderTracking(Long id) {
        Order order = findOrder(id);

        List<OrderTrackingResponse.TrackingEvent> events =
                historyRepository.findByOrderIdOrderByChangedAtAsc(id).stream()
                        .map(h -> OrderTrackingResponse.TrackingEvent.builder()
                                .status(h.getStatus())
                                .note(h.getNote())
                                .changedAt(h.getChangedAt())
                                .build())
                        .collect(Collectors.toList());

        // Estimate delivery: 5 days from placement
        String estimated = order.getCreatedAt()
                .plusDays(5)
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy"));

        return OrderTrackingResponse.builder()
                .orderId(order.getOrderId())
                .userId(order.getUserId())
                .currentStatus(order.getStatus())
                .orderedAt(order.getCreatedAt())
                .estimatedDelivery(estimated)
                .history(events)
                .build();
    }

    // ── private helpers ──────────────────────────────────────────

    private Order findOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
    }

    private void saveHistory(Long orderId, OrderStatus status, String note) {
        historyRepository.save(OrderStatusHistory.builder()
                .orderId(orderId)
                .status(status)
                .note(note)
                .build());
    }

    private OrderPlacedEvent buildEvent(Order order) {
        List<OrderPlacedEvent.OrderItemEvent> eventItems =
                (order.getOrderItems() == null ? List.<OrderItem>of() : order.getOrderItems())
                        .stream()
                        .map(i -> OrderPlacedEvent.OrderItemEvent.builder()
                                .productId(i.getProductId())
                                .quantity(i.getQuantity())
                                .build())
                        .collect(Collectors.toList());

        return OrderPlacedEvent.builder()
                .orderId(order.getOrderId())
                .userId(order.getUserId())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus().name())
                .items(eventItems)
                .build();
    }
}
