package Ecom.order_service.controller;

import Ecom.order_service.dto.request.OrderRequest;
import Ecom.order_service.dto.response.OrderResponse;
import Ecom.order_service.dto.response.OrderTrackingResponse;
import Ecom.order_service.enums.OrderStatus;
import Ecom.order_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /** POST /orders — place an order directly (without cart) */
    @PostMapping
    public ResponseEntity<OrderResponse> place(@RequestBody OrderRequest request) {
        return ResponseEntity.ok(orderService.placeOrder(request));
    }

    /** GET /orders/{id} — get order details */
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    /** GET /orders/user/{userId} — all orders for a user */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderResponse>> getByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(orderService.getOrdersByUser(userId));
    }

    /** GET /orders/mine — current user's orders using JWT header from gateway */
    @GetMapping("/mine")
    public ResponseEntity<List<OrderResponse>> getMyOrders(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(orderService.getOrdersByUser(userId));
    }

    /** PATCH /orders/{id}/status?status=SHIPPED — update order status */
    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam OrderStatus status) {
        return ResponseEntity.ok(orderService.updateOrderStatus(id, status));
    }

    /**
     * GET /orders/{id}/tracking
     * Returns the full status timeline with estimated delivery date.
     * Example response:
     * {
     *   "orderId": 1,
     *   "currentStatus": "SHIPPED",
     *   "orderedAt": "2026-03-16T10:00:00",
     *   "estimatedDelivery": "21 Mar 2026",
     *   "history": [
     *     { "status": "PENDING",   "note": "Order placed successfully", "changedAt": "..." },
     *     { "status": "CONFIRMED", "note": "Status updated to CONFIRMED","changedAt": "..." },
     *     { "status": "SHIPPED",   "note": "Status updated to SHIPPED",  "changedAt": "..." }
     *   ]
     * }
     */
    @GetMapping("/{id}/tracking")
    public ResponseEntity<OrderTrackingResponse> tracking(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderTracking(id));
    }

    /** GET /orders/all — admin: all orders across all users */
    @GetMapping("/all")
    public ResponseEntity<List<OrderResponse>> getAll() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    /** DELETE /orders/{id} — cancel order */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        orderService.cancelOrder(id);
        return ResponseEntity.noContent().build();
    }
}
