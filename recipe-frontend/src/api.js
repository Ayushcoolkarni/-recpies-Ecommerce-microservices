// ─────────────────────────────────────────────────────────────────────────────
//  api.js — centralised HTTP client for RasoiKit frontend
//
//  Base URL points to the API Gateway.
//  • Local dev  : http://localhost:8080
//  • Docker     : REACT_APP_API_URL env var overrides (set in docker-compose)
//
//  All authenticated calls pass the JWT as:   Authorization: Bearer <token>
//  The gateway also forwards:                 X-User-Id: <userId>
// ─────────────────────────────────────────────────────────────────────────────

const BASE = process.env.REACT_APP_API_URL || "http://localhost:8080";

// ── Internal helpers ──────────────────────────────────────────────────────────

async function request(method, path, body, token, userId) {
  const headers = { "Content-Type": "application/json" };
  if (token)  headers["Authorization"] = `Bearer ${token}`;
  if (userId) headers["X-User-Id"]     = String(userId);

  const res = await fetch(`${BASE}${path}`, {
    method,
    headers,
    body: body != null ? JSON.stringify(body) : undefined,
  });

  if (!res.ok) {
    let msg = `Request failed (${res.status})`;
    try {
      const err = await res.json();
      msg = err.message || err.error || msg;
    } catch (_) { /* non-JSON error body */ }
    throw new Error(msg);
  }

  // 204 No Content → return null
  if (res.status === 204) return null;
  return res.json();
}

const get    = (path, token, userId)       => request("GET",    path, null, token, userId);
const post   = (path, body, token, userId) => request("POST",   path, body, token, userId);
const put    = (path, body, token, userId) => request("PUT",    path, body, token, userId);
const patch  = (path, body, token, userId) => request("PATCH",  path, body, token, userId);
const del    = (path, token, userId)       => request("DELETE", path, null, token, userId);

// ─────────────────────────────────────────────────────────────────────────────
//  AUTH   /auth/*
// ─────────────────────────────────────────────────────────────────────────────

export const api = {

  // POST /auth/register   { name, email, password, phone }
  register: (form) => post("/auth/register", form),

  // POST /auth/login      { email, password }  → { accessToken, userId, name, email, role }
  login: (form) => post("/auth/login", form),

  // POST /auth/refresh    { refreshToken }
  refreshToken: (refreshToken) => post("/auth/refresh", { refreshToken }),

  // ─── USERS   /users/* ────────────────────────────────────────────────────

  // GET  /users            (admin only)
  getUsers: (token) => get("/users", token),

  // GET  /users/:id
  getUser: (id, token) => get(`/users/${id}`, token),

  // PUT  /users/:id        { name, phone, … }
  updateUser: (id, body, token) => put(`/users/${id}`, body, token),

  // POST /users/:id/addresses  { street, city, state, pincode }
  addAddress: (id, address, token) => post(`/users/${id}/addresses`, address, token),

  // POST /users/:id/saved-recipes/:recipeId
  saveRecipe: (userId, recipeId, token) =>
    post(`/users/${userId}/saved-recipes/${recipeId}`, null, token),

  // DELETE /users/:id/saved-recipes/:recipeId
  unsaveRecipe: (userId, recipeId, token) =>
    del(`/users/${userId}/saved-recipes/${recipeId}`, token),

  // ─── RECIPES   /recipes/* ────────────────────────────────────────────────

  // GET  /recipes                   → array of recipe objects
  getRecipes: (category) => {
    const q = category ? `?category=${encodeURIComponent(category)}` : "";
    return get(`/recipes${q}`);
  },

  // GET  /recipes/search?name=pasta
  searchRecipes: (name) =>
    get(`/recipes/search?name=${encodeURIComponent(name)}`),

  // GET  /recipes/:id
  getRecipe: (id) => get(`/recipes/${id}`),

  // GET  /recipes/:id/scaled?servings=4   → live prices from inventory
  getScaledRecipe: (id, servings) =>
    get(`/recipes/${id}/scaled?servings=${servings}`),

  // POST /recipes   (admin)
  createRecipe: (body, token) => post("/recipes", body, token),

  // PUT  /recipes/:id   (admin)
  updateRecipe: (id, body, token) => put(`/recipes/${id}`, body, token),

  // DELETE /recipes/:id   (admin)
  deleteRecipe: (id, token) => del(`/recipes/${id}`, token),

  // ─── INGREDIENTS   /ingredients/* ────────────────────────────────────────

  // GET  /ingredients
  getIngredients: (token) => get("/ingredients", token),

  // GET  /ingredients/:id
  getIngredient: (id, token) => get(`/ingredients/${id}`, token),

  // POST /ingredients   { name, unit, productId }
  createIngredient: (body, token) => post("/ingredients", body, token),

  // PUT  /ingredients/:id
  updateIngredient: (id, body, token) => put(`/ingredients/${id}`, body, token),

  // DELETE /ingredients/:id
  deleteIngredient: (id, token) => del(`/ingredients/${id}`, token),

  // ─── PRODUCTS / INVENTORY   /products/* ──────────────────────────────────

  // GET  /products
  getProducts: (token) => get("/products", token),

  // GET  /products/available
  getAvailableProducts: () => get("/products/available"),

  // GET  /products/:id
  getProduct: (id, token) => get(`/products/${id}`, token),

  // GET  /products/:id/in-stock   → boolean
  isInStock: (id) => get(`/products/${id}/in-stock`),

  // POST /products   { name, description, unit, pricePerUnit, stockQuantity, category }
  createProduct: (body, token) => post("/products", body, token),

  // PUT  /products/:id
  updateProduct: (id, body, token) => put(`/products/${id}`, body, token),

  // PATCH /products/:id/stock?quantity=50
  updateStock: (id, quantity, token) =>
    patch(`/products/${id}/stock?quantity=${quantity}`, null, token),

  // DELETE /products/:id
  deleteProduct: (id, token) => del(`/products/${id}`, token),

  // ─── CART   /cart/* ───────────────────────────────────────────────────────

  // GET  /cart/:userId
  getCart: (userId, token) => get(`/cart/${userId}`, token),

  // POST /cart/:userId/items   { productId, ingredientName, quantity, pricePerUnit }
  addToCart: (userId, item, token) => post(`/cart/${userId}/items`, item, token),

  // PATCH /cart/:userId/items/:itemId?quantity=2   (quantity=0 removes item)
  updateCartItem: (userId, itemId, quantity, token) =>
    patch(`/cart/${userId}/items/${itemId}?quantity=${quantity}`, null, token),

  // DELETE /cart/:userId/items/:itemId
  removeCartItem: (userId, itemId, token) =>
    del(`/cart/${userId}/items/${itemId}`, token),

  // DELETE /cart/:userId   — clear entire cart
  clearCart: (userId, token) => del(`/cart/${userId}`, token),

  // POST /cart/:userId/checkout?addressId=1   → places order + clears cart
  checkout: (userId, addressId, token) =>
    post(`/cart/${userId}/checkout?addressId=${addressId}`, null, token),

  // ─── ORDERS   /orders/* ───────────────────────────────────────────────────

  // POST /orders   { userId, items:[{productId,quantity}], addressId, paymentMethod }
  placeOrder: (body, token) => post("/orders", body, token),

  // GET  /orders/mine         (uses JWT → X-User-Id header)
  getMyOrders: (token, userId) => get("/orders/mine", token, userId),

  // GET  /orders/user/:userId
  getOrdersByUser: (userId, token) => get(`/orders/user/${userId}`, token),

  // GET  /orders/:id
  getOrder: (id, token) => get(`/orders/${id}`, token),

  // GET  /orders/:id/tracking   → full timeline + estimated delivery
  trackOrder: (id, token) => get(`/orders/${id}/tracking`, token),

  // PATCH /orders/:id/status?status=SHIPPED   (admin)
  updateOrderStatus: (id, status, token) =>
    patch(`/orders/${id}/status?status=${status}`, null, token),

  // DELETE /orders/:id   — cancel order
  cancelOrder: (id, token) => del(`/orders/${id}`, token),

  // GET  /orders/all   (admin)
  getAllOrders: (token) => get("/orders/all", token),

  // ─── PAYMENTS   /payments/* ───────────────────────────────────────────────

  // POST /payments   { orderId, userId, amount }
  createPayment: (body, token) => post("/payments", body, token),

  // GET  /payments/order/:orderId
  getPaymentByOrder: (orderId, token) => get(`/payments/order/${orderId}`, token),

  // GET  /payments/user/:userId
  getPaymentsByUser: (userId, token) => get(`/payments/user/${userId}`, token),

  // POST /payments/refund   { orderId, amount, reason }
  refundPayment: (body, token) => post("/payments/refund", body, token),

  // ─── SUGGESTIONS   /suggestions/* ────────────────────────────────────────

  // POST /suggestions   { userId, recipeName, ingredients, description }
  createSuggestion: (body, token) => post("/suggestions", body, token),

  // GET  /suggestions
  getSuggestions: (token) => get("/suggestions", token),

  // GET  /suggestions/user/:userId
  getMySuggestions: (userId, token) => get(`/suggestions/user/${userId}`, token),

  // ─── ADMIN   /admin/* ─────────────────────────────────────────────────────

  // GET  /admin/orders
  adminGetOrders: (token) => get("/admin/orders", token),

  // PATCH /admin/orders/status   { orderId, status, adminId }
  adminUpdateOrderStatus: (body, token) => patch("/admin/orders/status", body, token),

  // GET  /admin/products
  adminGetProducts: (token) => get("/admin/products", token),

  // PATCH /admin/products/stock   { productId, quantity, adminId }
  adminUpdateStock: (body, token) => patch("/admin/products/stock", body, token),

  // GET  /admin/suggestions
  adminGetSuggestions: (token) => get("/admin/suggestions", token),

  // POST /admin/suggestions/review   { suggestionId, adminId, decision, notes }
  adminReviewSuggestion: (body, token) => post("/admin/suggestions/review", body, token),

  // GET  /admin/stats?period=daily   (daily | weekly | monthly)
  adminGetStats: (period, token) => get(`/admin/stats?period=${period}`, token),

  // GET  /admin/users
  adminGetUsers: (token) => get("/admin/users", token),
};
