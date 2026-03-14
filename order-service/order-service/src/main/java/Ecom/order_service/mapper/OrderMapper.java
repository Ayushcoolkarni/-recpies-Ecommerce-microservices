package Ecom.order_service.mapper;

import Ecom.order_service.dto.response.*;
import Ecom.order_service.entity.*;
import org.springframework.stereotype.Component;
import java.util.stream.Collectors;

@Component
public class OrderMapper {

    public OrderResponse toResponse(Order order) {
        return OrderResponse.builder()
                .orderId(order.getOrderId())
                .userId(order.getUserId())
                .addressId(order.getAddressId())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .createdAt(order.getCreatedAt())
                .items(order.getOrderItems() != null
                        ? order.getOrderItems().stream()
                        .map(this::toItemResponse)
                        .collect(Collectors.toList())
                        : null)
                .build();
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        return OrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .ingredientName(item.getIngredientName())
                .quantity(item.getQuantity())
                .pricePerUnit(item.getPricePerUnit())
                .build();
    }
}