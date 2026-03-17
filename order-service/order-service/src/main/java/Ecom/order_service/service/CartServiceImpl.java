package Ecom.order_service.service;

import Ecom.order_service.client.InventoryClient;
import Ecom.order_service.dto.request.CartItemRequest;
import Ecom.order_service.dto.request.OrderItemRequest;
import Ecom.order_service.dto.request.OrderRequest;
import Ecom.order_service.dto.response.CartItemResponse;
import Ecom.order_service.dto.response.CartResponse;
import Ecom.order_service.dto.response.OrderResponse;
import Ecom.order_service.entity.Cart;
import Ecom.order_service.entity.CartItem;
import Ecom.order_service.exception.ResourceNotFoundException;
import Ecom.order_service.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private static final double TAX_RATE = 0.08; // 8 %

    private final CartRepository   cartRepository;
    private final OrderService     orderService;
    private final InventoryClient  inventoryClient;

    // ── GET ──────────────────────────────────────────────────────

    @Override
    public CartResponse getCart(Long userId) {
        return toResponse(getOrCreate(userId));
    }

    // ── ADD ──────────────────────────────────────────────────────

    @Override
    @Transactional
    public CartResponse addItem(Long userId, CartItemRequest request) {
        Cart cart = getOrCreate(userId);

        // If the same product already exists, just increment quantity
        cart.getCartItems().stream()
                .filter(i -> i.getProductId().equals(request.getProductId()))
                .findFirst()
                .ifPresentOrElse(
                        existing -> existing.setQuantity(existing.getQuantity() + request.getQuantity()),
                        () -> cart.getCartItems().add(
                                CartItem.builder()
                                        .cart(cart)
                                        .productId(request.getProductId())
                                        .ingredientName(request.getIngredientName())
                                        .quantity(request.getQuantity())
                                        .pricePerUnit(request.getPricePerUnit())
                                        .build()
                        )
                );

        log.info("Item added to cart for userId={} productId={}", userId, request.getProductId());
        return toResponse(cartRepository.save(cart));
    }

    // ── UPDATE ───────────────────────────────────────────────────

    @Override
    @Transactional
    public CartResponse updateItem(Long userId, Long itemId, Integer quantity) {
        Cart cart = getOrThrow(userId);

        CartItem item = cart.getCartItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found: " + itemId));

        if (quantity <= 0) {
            cart.getCartItems().remove(item);
            log.info("Cart item {} removed (quantity={})", itemId, quantity);
        } else {
            item.setQuantity(quantity);
            log.info("Cart item {} updated to quantity={}", itemId, quantity);
        }

        return toResponse(cartRepository.save(cart));
    }

    // ── REMOVE ───────────────────────────────────────────────────

    @Override
    @Transactional
    public CartResponse removeItem(Long userId, Long itemId) {
        Cart cart = getOrThrow(userId);

        boolean removed = cart.getCartItems().removeIf(i -> i.getId().equals(itemId));
        if (!removed) {
            throw new ResourceNotFoundException("Cart item not found: " + itemId);
        }

        log.info("Cart item {} removed for userId={}", itemId, userId);
        return toResponse(cartRepository.save(cart));
    }

    // ── CLEAR ────────────────────────────────────────────────────

    @Override
    @Transactional
    public void clearCart(Long userId) {
        Cart cart = getOrThrow(userId);
        cart.getCartItems().clear();
        cartRepository.save(cart);
        log.info("Cart cleared for userId={}", userId);
    }

    // ── CHECKOUT ─────────────────────────────────────────────────

    @Override
    @Transactional
    public OrderResponse checkout(Long userId, Long addressId) {
        Cart cart = getOrThrow(userId);

        if (cart.getCartItems().isEmpty()) {
            throw new RuntimeException("Cannot checkout with an empty cart");
        }

        // Stock-check every item before touching the order table
        for (CartItem item : cart.getCartItems()) {
            if (!inventoryClient.isInStock(item.getProductId())) {
                throw new RuntimeException(
                        "Product out of stock: " + item.getProductId()
                        + " (" + item.getIngredientName() + ")"
                );
            }
        }

        // Build order request from cart items
        List<OrderItemRequest> orderItems = cart.getCartItems().stream()
                .map(i -> {
                    OrderItemRequest oi = new OrderItemRequest();
                    oi.setProductId(i.getProductId());
                    oi.setIngredientName(i.getIngredientName());
                    oi.setQuantity(i.getQuantity());
                    oi.setPricePerUnit(i.getPricePerUnit());
                    return oi;
                })
                .collect(Collectors.toList());

        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setUserId(userId);
        orderRequest.setAddressId(addressId);
        orderRequest.setItems(orderItems);

        OrderResponse placed = orderService.placeOrder(orderRequest);

        // Clear the cart after successful placement
        cart.getCartItems().clear();
        cartRepository.save(cart);

        log.info("Checkout complete — userId={} orderId={}", userId, placed.getOrderId());
        return placed;
    }

    // ── private helpers ──────────────────────────────────────────

    /** Returns existing cart or creates a new empty one. */
    private Cart getOrCreate(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Cart c = Cart.builder()
                            .userId(userId)
                            .cartItems(new ArrayList<>())
                            .build();
                    return cartRepository.save(c);
                });
    }

    /** Returns existing cart or throws 404. */
    private Cart getOrThrow(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cart not found for userId: " + userId));
    }

    /** Converts a Cart entity to CartResponse with computed totals. */
    private CartResponse toResponse(Cart cart) {
        List<CartItemResponse> itemResponses = cart.getCartItems().stream()
                .map(i -> CartItemResponse.builder()
                        .id(i.getId())
                        .productId(i.getProductId())
                        .ingredientName(i.getIngredientName())
                        .quantity(i.getQuantity())
                        .pricePerUnit(i.getPricePerUnit())
                        .lineTotal(round(i.getPricePerUnit() * i.getQuantity()))
                        .build())
                .collect(Collectors.toList());

        double subtotal = itemResponses.stream()
                .mapToDouble(CartItemResponse::getLineTotal).sum();
        double tax   = round(subtotal * TAX_RATE);
        double total = round(subtotal + tax);

        return CartResponse.builder()
                .cartId(cart.getId())
                .userId(cart.getUserId())
                .items(itemResponses)
                .subtotal(round(subtotal))
                .estimatedTax(tax)
                .total(total)
                .createdAt(cart.getCreatedAt())
                .build();
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
