package Ecom.order_service.service;

import Ecom.order_service.dto.request.OrderRequest;
import Ecom.order_service.dto.response.OrderResponse;
import Ecom.order_service.dto.response.OrderTrackingResponse;
import Ecom.order_service.enums.OrderStatus;
import java.util.List;

public interface OrderService {
    OrderResponse placeOrder(OrderRequest request);
    OrderResponse getOrderById(Long id);
    List<OrderResponse> getOrdersByUser(Long userId);
    OrderResponse updateOrderStatus(Long id, OrderStatus status);
    void cancelOrder(Long id);
    OrderTrackingResponse getOrderTracking(Long id);
}
