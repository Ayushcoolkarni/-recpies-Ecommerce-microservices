package Ecom.order_service.controller;

import Ecom.order_service.dto.request.CartItemRequest;
import Ecom.order_service.dto.response.CartResponse;
import Ecom.order_service.dto.response.OrderResponse;
import Ecom.order_service.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    /**
     * GET /cart/{userId}
     * View current cart with all items, subtotal, estimated tax and total.
     */
    @GetMapping("/{userId}")
    public ResponseEntity<CartResponse> getCart(@PathVariable Long userId) {
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    /**
     * POST /cart/{userId}/items
     * Add an ingredient to the cart.
     * If the same productId already exists, quantity is incremented.
     *
     * Body: { "productId": 1, "ingredientName": "Tomato", "quantity": 3, "pricePerUnit": 2.50 }
     */
    @PostMapping("/{userId}/items")
    public ResponseEntity<CartResponse> addItem(
            @PathVariable Long userId,
            @Valid @RequestBody CartItemRequest request) {
        return ResponseEntity.ok(cartService.addItem(userId, request));
    }

    /**
     * PATCH /cart/{userId}/items/{itemId}?quantity=2
     * Update the quantity of a specific cart item.
     * Passing quantity=0 removes the item.
     */
    @PatchMapping("/{userId}/items/{itemId}")
    public ResponseEntity<CartResponse> updateItem(
            @PathVariable Long userId,
            @PathVariable Long itemId,
            @RequestParam Integer quantity) {
        return ResponseEntity.ok(cartService.updateItem(userId, itemId, quantity));
    }

    /**
     * DELETE /cart/{userId}/items/{itemId}
     * Remove a single item from the cart.
     */
    @DeleteMapping("/{userId}/items/{itemId}")
    public ResponseEntity<CartResponse> removeItem(
            @PathVariable Long userId,
            @PathVariable Long itemId) {
        return ResponseEntity.ok(cartService.removeItem(userId, itemId));
    }

    /**
     * DELETE /cart/{userId}
     * Clear the entire cart without placing an order.
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> clearCart(@PathVariable Long userId) {
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /cart/{userId}/checkout?addressId=1
     * Validates stock → places order → clears cart → returns the new order.
     */
    @PostMapping("/{userId}/checkout")
    public ResponseEntity<OrderResponse> checkout(
            @PathVariable Long userId,
            @RequestParam Long addressId) {
        return ResponseEntity.ok(cartService.checkout(userId, addressId));
    }
}
