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
curl -s -X POST $HOST/auth/register -H "Content-Type: application/json" \
  -d '{"name":"Ayush","email":"a@test.com","password":"pass123","phone":"9999999999"}'

TOKEN=$(curl -s -X POST $HOST/auth/login -H "Content-Type: application/json" \
  -d '{"email":"a@test.com","password":"pass123"}' | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")

AUTH="Authorization: Bearer $TOKEN"

# 2. Create ingredient + product
curl -s -X POST $HOST/products -H "$AUTH" -H "Content-Type: application/json" \
  -d '{"name":"Tomato","unit":"pcs","pricePerUnit":2.5,"stockQuantity":100,"category":"VEGETABLE"}'

curl -s -X POST $HOST/ingredients -H "$AUTH" -H "Content-Type: application/json" \
  -d '{"name":"Tomato","unit":"pcs","productId":1}'

# 3. Create recipe
curl -s -X POST $HOST/recipes -H "$AUTH" -H "Content-Type: application/json" \
  -d '{"name":"Tomato Pasta","category":"DINNER","defaultServings":2,"ingredientIds":[1],"quantities":[3.0]}'

# 4. Add address
curl -s -X POST $HOST/users/1/addresses -H "$AUTH" -H "Content-Type: application/json" \
  -d '{"street":"123 MG Road","city":"Bengaluru","state":"Karnataka","pincode":"560001","country":"India"}'

# 5. Add to cart + checkout
curl -s -X POST $HOST/cart/1/items -H "$AUTH" -H "Content-Type: application/json" \
  -d '{"productId":1,"ingredientName":"Tomato","quantity":3,"pricePerUnit":2.5}'

curl -s -X POST "$HOST/cart/1/checkout?addressId=1" -H "$AUTH"

# 6. Pay (SAGA mode — no Stripe key needed)
curl -s -X POST $HOST/payments -H "$AUTH" -H "Content-Type: application/json" \
  -d '{"orderId":1,"userId":1,"amount":7.5}'

# After payment: order auto-confirms, stock deducted, email sent
```

## Enable Stripe (optional)
Add to docker-compose.yml payment-service environment:
```yaml
- STRIPE_API_KEY=sk_live_...
```
The StripeConfig bean activates automatically when key is present.
