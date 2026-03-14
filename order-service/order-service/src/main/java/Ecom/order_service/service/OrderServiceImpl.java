package Ecom.order_service.service;

import Ecom.order_service.dto.request.*;
import Ecom.order_service.dto.response.*;
import Ecom.order_service.entity.*;
import Ecom.order_service.enums.OrderStatus;
import Ecom.order_service.event.OrderPlacedEvent;
import Ecom.order_service.exception.ResourceNotFoundException;
import Ecom.order_service.mapper.OrderMapper;
import Ecom.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    @Override
    public OrderResponse placeOrder(OrderRequest request) {
        List<OrderItem> items = request.getItems().stream().map(i ->
                OrderItem.builder()
                        .productId(i.getProductId())
                        .ingredientName(i.getIngredientName())
                        .quantity(i.getQuantity())
                        .pricePerUnit(i.getPricePerUnit())
                        .build()
        ).collect(Collectors.toList());

        double total = items.stream()
                .mapToDouble(i -> i.getPricePerUnit() * i.getQuantity())
                .sum();

        Order order = Order.builder()
                .userId(request.getUserId())
                .addressId(request.getAddressId())
                .totalAmount(total)
                .orderItems(items)
                .build();

        items.forEach(i -> i.setOrder(order));
        Order saved = orderRepository.save(order);

        kafkaTemplate.send("order.placed", OrderPlacedEvent.builder()
                .orderId(saved.getOrderId())
                .userId(saved.getUserId())
                .totalAmount(saved.getTotalAmount())
                .status(saved.getStatus().name())
                .build());

        return orderMapper.toResponse(saved);
    }

    @Override
    public OrderResponse getOrderById(Long id) {
        return orderMapper.toResponse(orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found")));
    }

    @Override
    public List<OrderResponse> getOrdersByUser(Long userId) {
        return orderRepository.findByUserId(userId).stream()
                .map(orderMapper::toResponse).collect(Collectors.toList());
    }

    @Override
    public OrderResponse updateOrderStatus(Long id, OrderStatus status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        order.setStatus(status);
        Order saved = orderRepository.save(order);

        if (status == OrderStatus.SHIPPED) {
            kafkaTemplate.send("order.shipped", OrderPlacedEvent.builder()
                    .orderId(saved.getOrderId())
                    .userId(saved.getUserId())
                    .totalAmount(saved.getTotalAmount())
                    .status(saved.getStatus().name())
                    .build());
        }

        return orderMapper.toResponse(saved);
    }

    @Override
    public void cancelOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }
}