package Ecom.order_service.service;

import Ecom.order_service.dto.request.CartItemRequest;
import Ecom.order_service.dto.response.CartResponse;
import Ecom.order_service.dto.response.OrderResponse;

public interface CartService {

    /** Returns the cart for userId, creating an empty one if it does not exist. */
    CartResponse getCart(Long userId);

    /** Adds an ingredient to the cart. If productId already exists, quantity is incremented. */
    CartResponse addItem(Long userId, CartItemRequest request);

    /** Updates quantity of a specific cart item. Passing quantity ≤ 0 removes the item. */
    CartResponse updateItem(Long userId, Long itemId, Integer quantity);

    /** Removes a single item from the cart. */
    CartResponse removeItem(Long userId, Long itemId);

    /** Empties the cart entirely without placing an order. */
    void clearCart(Long userId);

    /** Validates stock, converts cart → Order, clears cart, returns the placed order. */
    OrderResponse checkout(Long userId, Long addressId);
}
