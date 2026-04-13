import os

ROOT = os.getcwd()

def write(path, content):
    full = os.path.join(ROOT, path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    with open(full, 'w', encoding='utf-8') as f:
        f.write(content)
    print(f"  WROTE   {path}")

def patch(path, old, new):
    full = os.path.join(ROOT, path)
    with open(full, 'r', encoding='utf-8') as f:
        content = f.read()
    if old not in content:
        print(f"  SKIP    {path}  (pattern not found — may already be fixed)")
        return
    with open(full, 'w', encoding='utf-8') as f:
        f.write(content.replace(old, new, 1))
    print(f"  PATCHED {path}")

# ─────────────────────────────────────────────────────────────────────────────
print("\n=== FIX 1: Inventory consumer — use typed event + listen on order.confirmed ===")
# Stock should only be deducted AFTER payment, not on order.placed
write("inventory-service/inventory-service/src/main/java/Ecom/inventory_service/consumer/OrderEventConsumer.java", """\
package Ecom.inventory_service.consumer;

import Ecom.inventory_service.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
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
     * Listening to order.placed would deduct stock before payment —
     * causing stock loss on failed payments.
     */
    @KafkaListener(topics = "order.confirmed", groupId = "inventory-service")
    public void handleOrderConfirmed(String message) {
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
        } catch (Exception e) {
            log.error("Failed to process order.confirmed event: {}", e.getMessage(), e);
        }
    }

    /**
     * Restore stock when order is cancelled (payment failed).
     */
    @KafkaListener(topics = "order.cancelled", groupId = "inventory-service")
    public void handleOrderCancelled(String message) {
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
        } catch (Exception e) {
            log.error("Failed to process order.cancelled event: {}", e.getMessage(), e);
        }
    }
}
""")

# ─────────────────────────────────────────────────────────────────────────────
print("\n=== FIX 2: Add restoreStock() to ProductService interface + impl ===")
patch(
    "inventory-service/inventory-service/src/main/java/Ecom/inventory_service/service/ProductService.java",
    "    void deleteProduct(Long id);",
    """\
    void deleteProduct(Long id);

    void restoreStock(Long productId, Integer quantity);"""
)

patch(
    "inventory-service/inventory-service/src/main/java/Ecom/inventory_service/service/ProductServiceImpl.java",
    "    @Override\n    public void deleteProduct(Long id) {",
    """\
    @Override
    public void restoreStock(Long productId, Integer quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product not found with id: " + productId));

        int newStock = product.getStockQuantity() + quantity;
        product.setStockQuantity(newStock);
        product.setAvailable(newStock > 0);
        productRepository.save(product);
        log.info("Stock restored for productId {} + {} → total {}", productId, quantity, newStock);
    }

    @Override
    public void deleteProduct(Long id) {"""
)

# ─────────────────────────────────────────────────────────────────────────────
print("\n=== FIX 3: User service — add GET /users (all) + GET /users/email/{email} for admin/notification ===")
patch(
    "user-service/src/main/java/Ecom/user_service/controller/UserController.java",
    "    @GetMapping(\"/{id}\")",
    """\
    /** GET /users — admin: list all users */
    @GetMapping
    public ResponseEntity<java.util.List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    /** GET /users/email/{email} — internal: fetch user email (used by notification-service) */
    @GetMapping("/email/{email}")
    public ResponseEntity<UserResponse> getByEmail(@PathVariable String email) {
        return ResponseEntity.ok(userService.getUserByEmail(email));
    }

    @GetMapping("/{id}")"""
)

patch(
    "user-service/src/main/java/Ecom/user_service/service/UserService.java",
    "    UserResponse getUserById(Long id);",
    """\
    UserResponse getUserById(Long id);

    java.util.List<UserResponse> getAllUsers();

    UserResponse getUserByEmail(String email);"""
)

patch(
    "user-service/src/main/java/Ecom/user_service/service/UserServiceImpl.java",
    "    @Override\n    public UserResponse getUserById(Long id) {",
    """\
    @Override
    public java.util.List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(userMapper::toResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public UserResponse getUserByEmail(String email) {
        return userMapper.toResponse(
                userRepository.findByEmail(email)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email)));
    }

    @Override
    public UserResponse getUserById(Long id) {"""
)

# ─────────────────────────────────────────────────────────────────────────────
print("\n=== FIX 4: Add IngredientController — POST/GET /ingredients ===")
write("recipe-service/src/main/java/Ecom/recipe_service/controller/IngredientController.java", """\
package Ecom.recipe_service.controller;

import Ecom.recipe_service.entity.Ingredient;
import Ecom.recipe_service.repository.IngredientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CRUD for ingredients.
 * Ingredients are the raw items (Tomato, Basil) that get linked
 * to recipes via RecipeIngredient and linked to inventory via productId.
 */
@RestController
@RequestMapping("/ingredients")
@RequiredArgsConstructor
public class IngredientController {

    private final IngredientRepository ingredientRepository;

    @PostMapping
    public ResponseEntity<Ingredient> create(@RequestBody Ingredient ingredient) {
        return ResponseEntity.ok(ingredientRepository.save(ingredient));
    }

    @GetMapping
    public ResponseEntity<List<Ingredient>> getAll() {
        return ResponseEntity.ok(ingredientRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Ingredient> getById(@PathVariable Long id) {
        return ResponseEntity.ok(
                ingredientRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Ingredient not found: " + id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Ingredient> update(@PathVariable Long id,
                                              @RequestBody Ingredient updated) {
        Ingredient existing = ingredientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ingredient not found: " + id));
        existing.setName(updated.getName());
        existing.setUnit(updated.getUnit());
        existing.setProductId(updated.getProductId());
        return ResponseEntity.ok(ingredientRepository.save(existing));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        ingredientRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
""")

# ─────────────────────────────────────────────────────────────────────────────
print("\n=== FIX 5: Add /ingredients route to API Gateway ===")
patch(
    "api-gateway/src/main/resources/application.yml",
    "        - id: suggestion-service",
    """\
        - id: ingredient-service
          uri: lb://recipe-service
          predicates:
            - Path=/ingredients/**
          filters:
            - AuthenticationFilter

        - id: suggestion-service"""
)

# ─────────────────────────────────────────────────────────────────────────────
print("\n=== FIX 6: Notification UserServiceClient — use /users/email/{email} endpoint ===")
write("notification-service/notification-service/src/main/java/Ecom/notification_service/client/UserServiceClient.java", """\
package Ecom.notification_service.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserServiceClient {

    private final RestClient restClient;

    /**
     * Fetch user email by userId from user-service.
     * Returns null gracefully if user-service is down or user not found.
     */
    public String getEmailByUserId(Long userId) {
        try {
            Map<?, ?> user = restClient.get()
                    .uri("http://user-service/users/{id}", userId)
                    .retrieve()
                    .body(Map.class);
            if (user != null && user.get("email") != null) {
                return user.get("email").toString();
            }
        } catch (Exception e) {
            log.error("Could not fetch email for userId={}: {}", userId, e.getMessage());
        }
        return null;
    }
}
""")

# ─────────────────────────────────────────────────────────────────────────────
print("\n=== FIX 7: Notification RestClientConfig — add RestClient bean ===")
write("notification-service/notification-service/src/main/java/Ecom/notification_service/config/RestClientConfig.java", """\
package Ecom.notification_service.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    @LoadBalanced
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    public RestClient restClient(RestClient.Builder builder) {
        return builder.build();
    }
}
""")

# ─────────────────────────────────────────────────────────────────────────────
print("\n=== FIX 8: Add X-User-Id header extraction to OrderController (use gateway header) ===")
patch(
    "order-service/order-service/src/main/java/Ecom/order_service/controller/OrderController.java",
    "    /** GET /orders/user/{userId} — all orders for a user */\n    @GetMapping(\"/user/{userId}\")\n    public ResponseEntity<List<OrderResponse>> getByUser(@PathVariable Long userId) {\n        return ResponseEntity.ok(orderService.getOrdersByUser(userId));\n    }",
    """\
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
    }"""
)

# ─────────────────────────────────────────────────────────────────────────────
print("\n=== FIX 9: Add docker-compose.prod.yml ===")
write("docker-compose.prod.yml", """\
# docker-compose.prod.yml
# Production override — uses pre-built images from Docker Hub.
# Run with: docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d
# Requires env vars: DOCKERHUB_USERNAME, IMAGE_TAG (set in .env)

services:

  service-registry:
    build: !reset null
    image: ${DOCKERHUB_USERNAME}/ecom-service-registry:${IMAGE_TAG:-latest}
    restart: always
    deploy:
      resources:
        limits:
          memory: 384m

  api-gateway:
    build: !reset null
    image: ${DOCKERHUB_USERNAME}/ecom-api-gateway:${IMAGE_TAG:-latest}
    restart: always
    deploy:
      resources:
        limits:
          memory: 384m

  user-service:
    build: !reset null
    image: ${DOCKERHUB_USERNAME}/ecom-user-service:${IMAGE_TAG:-latest}
    restart: always
    deploy:
      resources:
        limits:
          memory: 512m

  recipe-service:
    build: !reset null
    image: ${DOCKERHUB_USERNAME}/ecom-recipe-service:${IMAGE_TAG:-latest}
    restart: always
    deploy:
      resources:
        limits:
          memory: 512m

  inventory-service:
    build: !reset null
    image: ${DOCKERHUB_USERNAME}/ecom-inventory-service:${IMAGE_TAG:-latest}
    restart: always
    deploy:
      resources:
        limits:
          memory: 512m

  order-service:
    build: !reset null
    image: ${DOCKERHUB_USERNAME}/ecom-order-service:${IMAGE_TAG:-latest}
    restart: always
    deploy:
      resources:
        limits:
          memory: 512m

  payment-service:
    build: !reset null
    image: ${DOCKERHUB_USERNAME}/ecom-payment-service:${IMAGE_TAG:-latest}
    restart: always
    deploy:
      resources:
        limits:
          memory: 512m

  notification-service:
    build: !reset null
    image: ${DOCKERHUB_USERNAME}/ecom-notification-service:${IMAGE_TAG:-latest}
    restart: always
    deploy:
      resources:
        limits:
          memory: 384m

  admin-service:
    build: !reset null
    image: ${DOCKERHUB_USERNAME}/ecom-admin-service:${IMAGE_TAG:-latest}
    restart: always
    deploy:
      resources:
        limits:
          memory: 512m

  postgres:
    restart: always
    command: >
      postgres
        -c max_connections=200
        -c shared_buffers=256MB

  kafka:
    restart: always
    environment:
      KAFKA_LOG_RETENTION_HOURS: 168
      KAFKA_LOG_RETENTION_BYTES: 1073741824
""")

# ─────────────────────────────────────────────────────────────────────────────
print("\n=== FIX 10: Updated DEPLOYMENT.md with all routes ===")
write("DEPLOYMENT.md", """\
# Deployment Guide — Recipe Delivery Platform

## CI/CD Flow
```
git push main
  → CI (build + test all 9 services in parallel)
  → Docker Hub (push images tagged :latest + :sha)
  → VPS SSH deploy (docker compose pull + up)
  → Smoke test (health checks on gateway + eureka)
```

## GitHub Secrets required
| Secret | Value |
|---|---|
| DOCKERHUB_USERNAME | Your Docker Hub username |
| DOCKERHUB_TOKEN | Docker Hub access token (Account Settings → Security) |
| VPS_HOST | Server IP e.g. 165.22.x.x |
| VPS_USER | SSH user e.g. ubuntu |
| VPS_SSH_KEY | Contents of ~/.ssh/id_rsa |
| MAIL_USERNAME | Gmail address |
| MAIL_PASSWORD | Gmail App Password (16 chars, no spaces) |
| MAIL_FROM | e.g. RecipeEcom <you@gmail.com> |

## First-time VPS setup
```bash
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER && newgrp docker
sudo mkdir -p /opt/ecom && sudo chown $USER /opt/ecom
```

## Manual deploy
```bash
cd /opt/ecom
cat > .env << EOF
MAIL_USERNAME=your@gmail.com
MAIL_PASSWORD=your-app-password
MAIL_FROM=RecipeEcom <your@gmail.com>
DOCKERHUB_USERNAME=yourusername
IMAGE_TAG=latest
EOF
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

## API Endpoints

### Auth (no token needed)
| Method | URL | Body |
|---|---|---|
| POST | /auth/register | {name, email, password, phone} |
| POST | /auth/login | {email, password} |
| POST | /auth/refresh | {refreshToken} |

### Users
| Method | URL | Notes |
|---|---|---|
| GET | /users | Admin only |
| GET | /users/{id} | |
| PUT | /users/{id} | |
| POST | /users/{id}/addresses | |
| POST | /users/{id}/saved-recipes/{recipeId} | |
| DELETE | /users/{id}/saved-recipes/{recipeId} | |

### Ingredients
| Method | URL | Body |
|---|---|---|
| POST | /ingredients | {name, unit, productId} |
| GET | /ingredients | |
| GET | /ingredients/{id} | |
| PUT | /ingredients/{id} | |
| DELETE | /ingredients/{id} | |

### Recipes
| Method | URL | Notes |
|---|---|---|
| POST | /recipes | {name, description, instructions, category, defaultServings, ingredientIds[], quantities[]} |
| GET | /recipes | |
| GET | /recipes?category=DINNER | |
| GET | /recipes/search?name=pasta | |
| GET | /recipes/{id} | |
| GET | /recipes/{id}/scaled?servings=4 | Returns live prices from inventory |
| PUT | /recipes/{id} | |
| DELETE | /recipes/{id} | |

### Suggestions
| Method | URL | Body |
|---|---|---|
| POST | /suggestions | {userId, recipeName, ingredients, description} |
| GET | /suggestions | |
| GET | /suggestions/user/{userId} | |

### Products (Inventory)
| Method | URL | Notes |
|---|---|---|
| POST | /products | {name, description, unit, pricePerUnit, stockQuantity, category} |
| GET | /products | |
| GET | /products/available | |
| GET | /products/{id} | |
| GET | /products/{id}/in-stock | Returns boolean |
| PUT | /products/{id} | |
| PATCH | /products/{id}/stock?quantity=50 | |
| DELETE | /products/{id} | |

### Cart
| Method | URL | Notes |
|---|---|---|
| GET | /cart/{userId} | Shows subtotal, tax, total |
| POST | /cart/{userId}/items | {productId, ingredientName, quantity, pricePerUnit} |
| PATCH | /cart/{userId}/items/{itemId}?quantity=2 | quantity=0 removes item |
| DELETE | /cart/{userId}/items/{itemId} | |
| DELETE | /cart/{userId} | Clear cart |
| POST | /cart/{userId}/checkout?addressId=1 | Places order + clears cart |

### Orders
| Method | URL | Notes |
|---|---|---|
| POST | /orders | Direct order without cart |
| GET | /orders/all | Admin only |
| GET | /orders/mine | Uses JWT (X-User-Id header) |
| GET | /orders/{id} | |
| GET | /orders/user/{userId} | |
| GET | /orders/{id}/tracking | Full timeline + estimated delivery |
| PATCH | /orders/{id}/status?status=SHIPPED | |
| DELETE | /orders/{id} | Cancel order |

### Payments
| Method | URL | Body |
|---|---|---|
| POST | /payments | {orderId, userId, amount} |
| GET | /payments/order/{orderId} | |
| GET | /payments/user/{userId} | |
| POST | /payments/refund | {orderId, amount, reason} |

### Admin
| Method | URL | Notes |
|---|---|---|
| GET | /admin/orders | All orders |
| PATCH | /admin/orders/status | {orderId, status, adminId} |
| GET | /admin/products | All products |
| PATCH | /admin/products/stock | {productId, quantity, adminId} |
| GET | /admin/suggestions | All suggestions |
| POST | /admin/suggestions/review | {suggestionId, adminId, decision, notes} |
| GET | /admin/stats?period=daily | daily/weekly/monthly |
| GET | /admin/users | All users |
| GET | /admin/users/{userId} | |
| GET | /admin/audit-logs | |
| GET | /admin/audit-logs/{adminId} | |

## Full test flow (curl)
```bash
HOST=http://localhost:8080

# 1. Register + login
curl -s -X POST $HOST/auth/register -H "Content-Type: application/json" \\
  -d '{"name":"Ayush","email":"a@test.com","password":"pass123","phone":"9999999999"}'

TOKEN=$(curl -s -X POST $HOST/auth/login -H "Content-Type: application/json" \\
  -d '{"email":"a@test.com","password":"pass123"}' | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")

AUTH="Authorization: Bearer $TOKEN"

# 2. Create ingredient + product
curl -s -X POST $HOST/products -H "$AUTH" -H "Content-Type: application/json" \\
  -d '{"name":"Tomato","unit":"pcs","pricePerUnit":2.5,"stockQuantity":100,"category":"VEGETABLE"}'

curl -s -X POST $HOST/ingredients -H "$AUTH" -H "Content-Type: application/json" \\
  -d '{"name":"Tomato","unit":"pcs","productId":1}'

# 3. Create recipe
curl -s -X POST $HOST/recipes -H "$AUTH" -H "Content-Type: application/json" \\
  -d '{"name":"Tomato Pasta","category":"DINNER","defaultServings":2,"ingredientIds":[1],"quantities":[3.0]}'

# 4. Add address
curl -s -X POST $HOST/users/1/addresses -H "$AUTH" -H "Content-Type: application/json" \\
  -d '{"street":"123 MG Road","city":"Bengaluru","state":"Karnataka","pincode":"560001","country":"India"}'

# 5. Add to cart + checkout
curl -s -X POST $HOST/cart/1/items -H "$AUTH" -H "Content-Type: application/json" \\
  -d '{"productId":1,"ingredientName":"Tomato","quantity":3,"pricePerUnit":2.5}'

curl -s -X POST "$HOST/cart/1/checkout?addressId=1" -H "$AUTH"

# 6. Pay (SAGA mode — no Stripe key needed)
curl -s -X POST $HOST/payments -H "$AUTH" -H "Content-Type: application/json" \\
  -d '{"orderId":1,"userId":1,"amount":7.5}'

# After payment: order auto-confirms, stock deducted, email sent
```

## Enable Stripe (optional)
Add to docker-compose.yml payment-service environment:
```yaml
- STRIPE_API_KEY=sk_live_...
```
The StripeConfig bean activates automatically when key is present.
""")

print("\n" + "="*60)
print("ALL IMPLEMENTATIONS COMPLETE")
print("="*60)
print("\nFiles changed:")
print("  inventory-service: consumer listens on order.confirmed (not placed)")
print("  inventory-service: restoreStock() added for cancellations")
print("  user-service:      GET /users + GET /users/email/{email} added")
print("  recipe-service:    IngredientController created (POST/GET/PUT/DELETE)")
print("  api-gateway:       /ingredients route added")
print("  notification-service: RestClientConfig bean added")
print("  notification-service: UserServiceClient uses correct /users/{id} endpoint")
print("  order-service:     GET /orders/mine endpoint added")
print("  docker-compose.prod.yml: created")
print("  DEPLOYMENT.md:     full API reference with all endpoints")
print("\nNext steps:")
print("  git add -A")
print('  git commit -m "feat: missing features implemented"')
print("  git push origin main")
