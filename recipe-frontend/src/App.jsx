import React, { useState, useEffect, useCallback, createContext, useContext } from "react";
import { Routes, Route, Navigate, useNavigate, useParams } from "react-router-dom";
import AdminApp from "./admin/AdminApp";

// Import your pages
import Home from "./Home";
import Recipes from "./Recipes";
import Cart from "./Cart";
import Orders from "./Orders";
import Login from "./Login";
import Register from "./Register";
import RecipeDetail from "./RecipeDetail";

// ─────────────────────────────────────────────
// CONTEXTS
// ─────────────────────────────────────────────
const AuthCtx = createContext(null);
const CartCtx = createContext(null);
const ToastCtx = createContext(null);

export const useAuth = () => useContext(AuthCtx);
export const useCart = () => useContext(CartCtx);
export const useToast = () => useContext(ToastCtx);

// ─────────────────────────────────────────────
// AUTH PROVIDER
// ─────────────────────────────────────────────
function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try { return JSON.parse(localStorage.getItem("rce_u")); } catch { return null; }
  });
  const [token, setToken] = useState(() => localStorage.getItem("rce_t"));

  const login = (u, t) => {
    setUser(u);
    setToken(t);
    localStorage.setItem("rce_u", JSON.stringify(u));
    localStorage.setItem("rce_t", t);
  };

  const logout = () => {
    setUser(null);
    setToken(null);
    localStorage.removeItem("rce_u");
    localStorage.removeItem("rce_t");
  };

  return (
    <AuthCtx.Provider value={{ user, token, login, logout }}>
      {children}
    </AuthCtx.Provider>
  );
}

// ─────────────────────────────────────────────
// CART PROVIDER
// ─────────────────────────────────────────────
function CartProvider({ children }) {
  const [items, setItems] = useState([]);

  const add = (r) => {
    setItems(prev => {
      const exists = prev.find(i => i.id === r.id);
      if (exists) return prev.map(i => i.id === r.id ? { ...i, qty: i.qty + 1 } : i);
      return [...prev, { ...r, qty: 1 }];
    });
  };

  const remove = (id) => setItems(prev => prev.filter(i => i.id !== id));
  const clear = () => setItems([]);

  const total = items.reduce((s, i) => s + i.price * i.qty, 0);
  const count = items.reduce((s, i) => s + i.qty, 0);

  return (
    <CartCtx.Provider value={{ items, add, remove, clear, total, count }}>
      {children}
    </CartCtx.Provider>
  );
}

// ─────────────────────────────────────────────
// ADMIN PROTECTED ROUTE
// ─────────────────────────────────────────────
function AdminRoute({ children }) {
  const { user } = useAuth();

  if (!user || user.role !== "ADMIN") {
    return <Navigate to="/login" />;
  }

  return children;
}

// ─────────────────────────────────────────────
// NAVBAR
// ─────────────────────────────────────────────
function Nav() {
  const navigate = useNavigate();
  const { user, logout } = useAuth();
  const { count } = useCart();

  return (
    <nav style={{ padding: "10px 20px", borderBottom: "1px solid #ccc" }}>
      <button onClick={() => navigate("/")}>Home</button>
      <button onClick={() => navigate("/recipes")}>Recipes</button>
      <button onClick={() => navigate("/cart")}>Cart ({count})</button>

      {user && (
        <button onClick={() => navigate("/orders")}>Orders</button>
      )}

      <button onClick={() => navigate("/admin")}>Admin</button>

      <span style={{ marginLeft: "auto" }} />

      {user ? (
        <button onClick={logout}>Logout</button>
      ) : (
        <>
          <button onClick={() => navigate("/login")}>Login</button>
          <button onClick={() => navigate("/register")}>Register</button>
        </>
      )}
    </nav>
  );
}

// ─────────────────────────────────────────────
// MAIN APP
// ─────────────────────────────────────────────
export default function App() {
  return (
    <AuthProvider>
      <CartProvider>
        <div style={{ minHeight: "100vh", display: "flex", flexDirection: "column" }}>
          
          <Nav />

          <main style={{ flex: 1 }}>
            <Routes>
              <Route path="/" element={<Home />} />
              <Route path="/recipes" element={<Recipes />} />
              <Route path="/cart" element={<Cart />} />
              <Route path="/orders" element={<Orders />} />
              <Route path="/login" element={<Login />} />
              <Route path="/register" element={<Register />} />
              <Route path="/recipe/:id" element={<RecipeDetail />} />

              {/* ADMIN */}
              <Route
                path="/admin"
                element={
                  <AdminRoute>
                    <AdminApp />
                  </AdminRoute>
                }
              />
            </Routes>
          </main>
        </div>
    </CartProvider>
    </AuthProvider>
  );
}