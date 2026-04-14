import { useState, useEffect, useCallback, createContext, useContext } from "react";

// ── Fonts ──────────────────────────────────────────────────────────────────────
const FontStyle = () => (
  <style>{`
    @import url('https://fonts.googleapis.com/css2?family=Playfair+Display:wght@400;600;700&family=DM+Sans:wght@300;400;500;600&display=swap');

    *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }

    :root {
      --cream:   #FAF7F2;
      --warm:    #F5EFE4;
      --sand:    #E8D5B7;
      --terr:    #C17C54;
      --terr2:   #A8603A;
      --forest:  #2D4A3E;
      --forest2: #1E3329;
      --charcoal:#2C2825;
      --muted:   #7A6F68;
      --white:   #FFFFFF;
      --danger:  #C0392B;
      --radius:  12px;
      --shadow:  0 4px 24px rgba(44,40,37,.10);
      --font-display: 'Playfair Display', Georgia, serif;
      --font-body:    'DM Sans', system-ui, sans-serif;
    }

    body {
      background: var(--cream);
      color: var(--charcoal);
      font-family: var(--font-body);
      font-size: 15px;
      line-height: 1.6;
      min-height: 100vh;
    }

    button { cursor: pointer; font-family: var(--font-body); }
    input, select, textarea { font-family: var(--font-body); }
    a { text-decoration: none; color: inherit; }

    /* Scrollbar */
    ::-webkit-scrollbar { width: 6px; }
    ::-webkit-scrollbar-track { background: var(--warm); }
    ::-webkit-scrollbar-thumb { background: var(--sand); border-radius: 3px; }

    /* Animations */
    @keyframes fadeUp {
      from { opacity: 0; transform: translateY(18px); }
      to   { opacity: 1; transform: translateY(0); }
    }
    @keyframes spin {
      to { transform: rotate(360deg); }
    }
    @keyframes pulse {
      0%,100% { opacity:1; } 50% { opacity:.5; }
    }
    .fade-up { animation: fadeUp .45s ease both; }
    .fade-up-2 { animation: fadeUp .45s .1s ease both; }
    .fade-up-3 { animation: fadeUp .45s .2s ease both; }
  `}</style>
);

// ── API Layer ──────────────────────────────────────────────────────────────────
const API = "http://localhost:8080/api";

async function req(method, path, body, token) {
  const headers = { "Content-Type": "application/json" };
  if (token) headers["Authorization"] = `Bearer ${token}`;
  const res = await fetch(`${API}${path}`, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({ message: res.statusText }));
    throw new Error(err.message || "Request failed");
  }
  return res.status === 204 ? null : res.json();
}

// ── Mock data for demo (replace with real API calls) ──────────────────────────
const MOCK_RECIPES = [
  { id: 1, name: "Butter Chicken", category: "Non-Veg", price: 349, cookTime: "35 min", rating: 4.8, reviews: 1240, description: "Tender chicken in a rich, creamy tomato sauce with aromatic spices.", ingredients: ["Chicken", "Butter", "Cream", "Tomato", "Spices"], serves: 2, image: "🍛", inStock: true },
  { id: 2, name: "Paneer Tikka Masala", category: "Veg", price: 299, cookTime: "30 min", rating: 4.7, reviews: 987, description: "Smoky grilled paneer in a vibrant spiced tomato gravy.", ingredients: ["Paneer", "Tomato", "Cream", "Spices", "Bell Pepper"], serves: 2, image: "🧆", inStock: true },
  { id: 3, name: "Dal Makhani", category: "Veg", price: 249, cookTime: "45 min", rating: 4.6, reviews: 834, description: "Slow-cooked black lentils in a buttery, smoky sauce.", ingredients: ["Black Lentils", "Butter", "Cream", "Tomato"], serves: 2, image: "🫘", inStock: true },
  { id: 4, name: "Biryani Royal", category: "Non-Veg", price: 449, cookTime: "50 min", rating: 4.9, reviews: 2100, description: "Fragrant basmati rice layered with marinated chicken and saffron.", ingredients: ["Basmati Rice", "Chicken", "Saffron", "Whole Spices", "Fried Onion"], serves: 3, image: "🍚", inStock: true },
  { id: 5, name: "Chole Bhature", category: "Veg", price: 199, cookTime: "25 min", rating: 4.5, reviews: 672, description: "Tangy spiced chickpeas with pillowy fried bread.", ingredients: ["Chickpeas", "Flour", "Spices", "Onion", "Tomato"], serves: 2, image: "🫓", inStock: false },
  { id: 6, name: "Prawn Masala", category: "Non-Veg", price: 499, cookTime: "30 min", rating: 4.7, reviews: 445, description: "Succulent prawns in a fiery coastal masala with coconut.", ingredients: ["Prawns", "Coconut", "Tomato", "Spices", "Curry Leaves"], serves: 2, image: "🦐", inStock: true },
  { id: 7, name: "Palak Paneer", category: "Veg", price: 279, cookTime: "30 min", rating: 4.6, reviews: 756, description: "Cottage cheese cubes in a smooth, spiced spinach gravy.", ingredients: ["Paneer", "Spinach", "Cream", "Spices", "Onion"], serves: 2, image: "🥬", inStock: true },
  { id: 8, name: "Rogan Josh", category: "Non-Veg", price: 399, cookTime: "55 min", rating: 4.8, reviews: 623, description: "Kashmiri slow-cooked lamb in bold spiced red gravy.", ingredients: ["Lamb", "Kashmiri Chilli", "Yoghurt", "Whole Spices"], serves: 2, image: "🍖", inStock: true },
];

const MOCK_ORDERS = [
  { id: "ORD-001", date: "2025-04-10", items: ["Butter Chicken", "Dal Makhani"], total: 598, status: "Delivered" },
  { id: "ORD-002", date: "2025-04-12", items: ["Biryani Royal"], total: 449, status: "Out for delivery" },
];

// ── Auth Context ───────────────────────────────────────────────────────────────
const AuthCtx = createContext(null);
function useAuth() { return useContext(AuthCtx); }

function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try { return JSON.parse(localStorage.getItem("user")); } catch { return null; }
  });
  const [token, setToken] = useState(() => localStorage.getItem("token") || null);

  const login = (userData, tok) => {
    setUser(userData); setToken(tok);
    localStorage.setItem("user", JSON.stringify(userData));
    localStorage.setItem("token", tok);
  };
  const logout = () => {
    setUser(null); setToken(null);
    localStorage.removeItem("user"); localStorage.removeItem("token");
  };

  return <AuthCtx.Provider value={{ user, token, login, logout }}>{children}</AuthCtx.Provider>;
}

// ── Cart Context ───────────────────────────────────────────────────────────────
const CartCtx = createContext(null);
function useCart() { return useContext(CartCtx); }

function CartProvider({ children }) {
  const [items, setItems] = useState([]);

  const add = (recipe) => setItems(prev => {
    const ex = prev.find(i => i.id === recipe.id);
    if (ex) return prev.map(i => i.id === recipe.id ? { ...i, qty: i.qty + 1 } : i);
    return [...prev, { ...recipe, qty: 1 }];
  });
  const remove = (id) => setItems(prev => prev.filter(i => i.id !== id));
  const update = (id, qty) => {
    if (qty <= 0) return remove(id);
    setItems(prev => prev.map(i => i.id === id ? { ...i, qty } : i));
  };
  const clear = () => setItems([]);
  const total = items.reduce((s, i) => s + i.price * i.qty, 0);
  const count = items.reduce((s, i) => s + i.qty, 0);

  return <CartCtx.Provider value={{ items, add, remove, update, clear, total, count }}>{children}</CartCtx.Provider>;
}

// ── Shared UI Components ───────────────────────────────────────────────────────
function Btn({ children, onClick, variant = "primary", size = "md", disabled, style }) {
  const base = {
    border: "none", borderRadius: 8, fontWeight: 500, transition: "all .2s",
    display: "inline-flex", alignItems: "center", gap: 6,
    padding: size === "sm" ? "6px 14px" : size === "lg" ? "14px 32px" : "10px 20px",
    fontSize: size === "sm" ? 13 : size === "lg" ? 16 : 14,
    opacity: disabled ? 0.55 : 1, cursor: disabled ? "not-allowed" : "pointer",
  };
  const variants = {
    primary:  { background: "var(--terr)",   color: "#fff" },
    outline:  { background: "transparent",   color: "var(--terr)",   border: "1.5px solid var(--terr)" },
    forest:   { background: "var(--forest)", color: "#fff" },
    ghost:    { background: "transparent",   color: "var(--muted)" },
    danger:   { background: "var(--danger)", color: "#fff" },
  };
  return (
    <button style={{ ...base, ...variants[variant], ...style }} onClick={onClick} disabled={disabled}
      onMouseEnter={e => { if (!disabled) e.currentTarget.style.opacity = "0.85"; }}
      onMouseLeave={e => { e.currentTarget.style.opacity = "1"; }}>
      {children}
    </button>
  );
}

function Input({ label, type = "text", value, onChange, placeholder, required }) {
  return (
    <div style={{ marginBottom: 16 }}>
      {label && <label style={{ display: "block", fontWeight: 500, fontSize: 13, marginBottom: 6, color: "var(--charcoal)" }}>{label}</label>}
      <input
        type={type} value={value} onChange={e => onChange(e.target.value)}
        placeholder={placeholder} required={required}
        style={{
          width: "100%", padding: "10px 14px", borderRadius: 8,
          border: "1.5px solid var(--sand)", background: "var(--white)",
          fontSize: 14, color: "var(--charcoal)", outline: "none",
          transition: "border .2s",
        }}
        onFocus={e => e.target.style.borderColor = "var(--terr)"}
        onBlur={e => e.target.style.borderColor = "var(--sand)"}
      />
    </div>
  );
}

function Badge({ children, color = "var(--terr)" }) {
  return (
    <span style={{
      background: color + "18", color, borderRadius: 20, fontSize: 11,
      padding: "2px 10px", fontWeight: 600, letterSpacing: .3,
    }}>{children}</span>
  );
}

function Stars({ rating }) {
  return (
    <span style={{ fontSize: 12, color: "#e67e22", letterSpacing: 1 }}>
      {"★".repeat(Math.round(rating))}{"☆".repeat(5 - Math.round(rating))}
    </span>
  );
}

function Spinner() {
  return <div style={{ width: 20, height: 20, border: "2.5px solid #fff3", borderTopColor: "#fff", borderRadius: "50%", animation: "spin .7s linear infinite" }} />;
}

function Toast({ msg, onClose }) {
  useEffect(() => { const t = setTimeout(onClose, 3000); return () => clearTimeout(t); }, []);
  return (
    <div style={{
      position: "fixed", bottom: 28, right: 28, background: "var(--forest)",
      color: "#fff", padding: "12px 20px", borderRadius: 10, zIndex: 9999,
      fontWeight: 500, fontSize: 14, boxShadow: "0 4px 20px rgba(0,0,0,.2)",
      animation: "fadeUp .3s ease both",
    }}>{msg}</div>
  );
}

// ── Navigation ─────────────────────────────────────────────────────────────────
function Nav({ page, setPage }) {
  const { user, logout } = useAuth();
  const { count } = useCart();
  const [mobileOpen, setMobileOpen] = useState(false);

  return (
    <nav style={{
      position: "sticky", top: 0, zIndex: 100,
      background: "rgba(250,247,242,.97)", backdropFilter: "blur(12px)",
      borderBottom: "1px solid var(--sand)", padding: "0 24px",
    }}>
      <div style={{ maxWidth: 1200, margin: "0 auto", height: 64, display: "flex", alignItems: "center", justifyContent: "space-between" }}>
        {/* Logo */}
        <div onClick={() => setPage("home")} style={{ cursor: "pointer", display: "flex", alignItems: "center", gap: 10 }}>
          <span style={{ fontSize: 26 }}>🌿</span>
          <span style={{ fontFamily: "var(--font-display)", fontSize: 20, fontWeight: 700, color: "var(--forest)" }}>RecipeEcom</span>
        </div>

        {/* Desktop links */}
        <div style={{ display: "flex", alignItems: "center", gap: 4 }}>
          {["home","recipes"].map(p => (
            <button key={p} onClick={() => setPage(p)} style={{
              background: page === p ? "var(--warm)" : "transparent",
              border: "none", borderRadius: 8, padding: "6px 14px",
              fontWeight: page === p ? 600 : 400, color: page === p ? "var(--terr)" : "var(--charcoal)",
              fontSize: 14, cursor: "pointer", textTransform: "capitalize", transition: "all .2s",
            }}>{p === "home" ? "Home" : "Recipes"}</button>
          ))}
          {user && (
            <button onClick={() => setPage("orders")} style={{
              background: page === "orders" ? "var(--warm)" : "transparent",
              border: "none", borderRadius: 8, padding: "6px 14px",
              fontWeight: page === "orders" ? 600 : 400, color: page === "orders" ? "var(--terr)" : "var(--charcoal)",
              fontSize: 14, cursor: "pointer", transition: "all .2s",
            }}>My Orders</button>
          )}

          {/* Cart */}
          <button onClick={() => setPage("cart")} style={{
            position: "relative", background: page === "cart" ? "var(--warm)" : "transparent",
            border: "none", borderRadius: 8, padding: "6px 14px",
            color: "var(--charcoal)", fontSize: 22, cursor: "pointer", transition: "all .2s",
          }}>
            🛒
            {count > 0 && (
              <span style={{
                position: "absolute", top: 2, right: 4, background: "var(--terr)",
                color: "#fff", borderRadius: "50%", fontSize: 10, fontWeight: 700,
                width: 17, height: 17, display: "flex", alignItems: "center", justifyContent: "center",
              }}>{count}</span>
            )}
          </button>

          {/* Auth */}
          {user ? (
            <div style={{ display: "flex", alignItems: "center", gap: 8, marginLeft: 8 }}>
              <span style={{ fontSize: 13, color: "var(--muted)" }}>Hi, {user.name?.split(" ")[0]}</span>
              <Btn variant="outline" size="sm" onClick={logout}>Logout</Btn>
            </div>
          ) : (
            <div style={{ display: "flex", gap: 8, marginLeft: 8 }}>
              <Btn variant="ghost" size="sm" onClick={() => setPage("login")}>Login</Btn>
              <Btn size="sm" onClick={() => setPage("register")}>Sign up</Btn>
            </div>
          )}
        </div>
      </div>
    </nav>
  );
}

// ── Home Page ──────────────────────────────────────────────────────────────────
function HomePage({ setPage }) {
  const cats = [
    { icon: "🍛", label: "Curries", count: 24 },
    { icon: "🍚", label: "Rice & Biryani", count: 12 },
    { icon: "🥗", label: "Salads", count: 8 },
    { icon: "🍰", label: "Desserts", count: 16 },
    { icon: "🫓", label: "Breads", count: 9 },
    { icon: "🍲", label: "Soups", count: 7 },
  ];

  const featured = MOCK_RECIPES.filter(r => r.rating >= 4.7).slice(0, 3);

  return (
    <div>
      {/* Hero */}
      <div style={{
        background: "linear-gradient(135deg, var(--forest) 0%, var(--forest2) 100%)",
        padding: "80px 24px 60px", textAlign: "center", color: "#fff",
        position: "relative", overflow: "hidden",
      }}>
        <div style={{ position: "absolute", inset: 0, opacity: .04, backgroundImage: "radial-gradient(circle, #fff 1px, transparent 1px)", backgroundSize: "32px 32px" }} />
        <div style={{ position: "relative", maxWidth: 680, margin: "0 auto" }}>
          <p className="fade-up" style={{ fontSize: 13, letterSpacing: 3, fontWeight: 500, color: "var(--sand)", textTransform: "uppercase", marginBottom: 16 }}>Fresh Ingredients, Delivered</p>
          <h1 className="fade-up-2" style={{ fontFamily: "var(--font-display)", fontSize: "clamp(32px,5vw,56px)", fontWeight: 700, lineHeight: 1.15, marginBottom: 20 }}>
            Restaurant-Quality<br />Recipes at Home
          </h1>
          <p className="fade-up-3" style={{ fontSize: 17, opacity: .8, marginBottom: 36, lineHeight: 1.7 }}>
            Order chef-curated recipe kits with pre-measured ingredients. Cook something extraordinary tonight.
          </p>
          <div style={{ display: "flex", gap: 12, justifyContent: "center", flexWrap: "wrap" }}>
            <Btn size="lg" onClick={() => setPage("recipes")} style={{ background: "var(--terr)" }}>Browse Recipes →</Btn>
            <Btn size="lg" variant="outline" onClick={() => setPage("recipes")} style={{ borderColor: "rgba(255,255,255,.4)", color: "#fff" }}>How it works</Btn>
          </div>
        </div>
      </div>

      {/* Stats bar */}
      <div style={{ background: "var(--warm)", borderBottom: "1px solid var(--sand)" }}>
        <div style={{ maxWidth: 1200, margin: "0 auto", padding: "20px 24px", display: "flex", gap: 40, justifyContent: "center", flexWrap: "wrap" }}>
          {[["🍽️","200+ Recipes","Chef-curated"], ["⚡","30-60 min","Cook time"], ["🚚","Free delivery","Above ₹499"], ["⭐","4.8/5","Average rating"]].map(([icon, label, sub]) => (
            <div key={label} style={{ textAlign: "center" }}>
              <div style={{ fontSize: 22, marginBottom: 2 }}>{icon}</div>
              <div style={{ fontWeight: 600, fontSize: 15 }}>{label}</div>
              <div style={{ fontSize: 12, color: "var(--muted)" }}>{sub}</div>
            </div>
          ))}
        </div>
      </div>

      <div style={{ maxWidth: 1200, margin: "0 auto", padding: "56px 24px" }}>
        {/* Categories */}
        <h2 style={{ fontFamily: "var(--font-display)", fontSize: 28, fontWeight: 700, marginBottom: 24 }}>Browse by Category</h2>
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(160px, 1fr))", gap: 14, marginBottom: 60 }}>
          {cats.map(c => (
            <div key={c.label} onClick={() => setPage("recipes")}
              style={{
                background: "var(--white)", border: "1.5px solid var(--sand)", borderRadius: 12,
                padding: "20px 16px", textAlign: "center", cursor: "pointer", transition: "all .2s",
              }}
              onMouseEnter={e => { e.currentTarget.style.borderColor = "var(--terr)"; e.currentTarget.style.transform = "translateY(-2px)"; }}
              onMouseLeave={e => { e.currentTarget.style.borderColor = "var(--sand)"; e.currentTarget.style.transform = "none"; }}>
              <div style={{ fontSize: 32, marginBottom: 8 }}>{c.icon}</div>
              <div style={{ fontWeight: 600, fontSize: 14 }}>{c.label}</div>
              <div style={{ fontSize: 12, color: "var(--muted)" }}>{c.count} recipes</div>
            </div>
          ))}
        </div>

        {/* Featured */}
        <h2 style={{ fontFamily: "var(--font-display)", fontSize: 28, fontWeight: 700, marginBottom: 24 }}>Most Loved Recipes</h2>
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(300px, 1fr))", gap: 20 }}>
          {featured.map(r => <RecipeCard key={r.id} recipe={r} setPage={setPage} />)}
        </div>
      </div>
    </div>
  );
}

// ── Recipe Card ────────────────────────────────────────────────────────────────
function RecipeCard({ recipe: r, setPage }) {
  const { add } = useCart();
  const [added, setAdded] = useState(false);

  const handleAdd = (e) => {
    e.stopPropagation();
    if (!r.inStock) return;
    add(r);
    setAdded(true);
    setTimeout(() => setAdded(false), 1500);
  };

  return (
    <div style={{
      background: "var(--white)", borderRadius: 14, overflow: "hidden",
      border: "1.5px solid var(--sand)", transition: "all .25s",
      display: "flex", flexDirection: "column",
    }}
      onMouseEnter={e => { e.currentTarget.style.boxShadow = "0 8px 32px rgba(44,40,37,.12)"; e.currentTarget.style.transform = "translateY(-3px)"; }}
      onMouseLeave={e => { e.currentTarget.style.boxShadow = "none"; e.currentTarget.style.transform = "none"; }}>

      {/* Thumbnail */}
      <div style={{
        height: 140, background: "var(--warm)", display: "flex", alignItems: "center",
        justifyContent: "center", fontSize: 64, cursor: "pointer",
      }} onClick={() => setPage("recipe-" + r.id)}>
        {r.image}
        {!r.inStock && (
          <div style={{
            position: "absolute", background: "rgba(0,0,0,.5)", color: "#fff",
            padding: "4px 12px", borderRadius: 20, fontSize: 12, fontWeight: 600,
          }}>Out of stock</div>
        )}
      </div>

      <div style={{ padding: "16px 18px 18px", flex: 1, display: "flex", flexDirection: "column" }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: 6 }}>
          <h3 style={{ fontFamily: "var(--font-display)", fontSize: 17, fontWeight: 700, cursor: "pointer" }}
            onClick={() => setPage("recipe-" + r.id)}>{r.name}</h3>
          <Badge color={r.category === "Veg" ? "var(--forest)" : "var(--terr)"}>{r.category}</Badge>
        </div>

        <div style={{ display: "flex", alignItems: "center", gap: 6, marginBottom: 10 }}>
          <Stars rating={r.rating} />
          <span style={{ fontSize: 12, color: "var(--muted)" }}>{r.rating} ({r.reviews.toLocaleString()})</span>
          <span style={{ fontSize: 12, color: "var(--muted)", marginLeft: "auto" }}>⏱ {r.cookTime}</span>
        </div>

        <p style={{ fontSize: 13, color: "var(--muted)", lineHeight: 1.55, marginBottom: 16, flex: 1 }}>{r.description}</p>

        <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
          <span style={{ fontFamily: "var(--font-display)", fontSize: 20, fontWeight: 700, color: "var(--forest)" }}>₹{r.price}</span>
          <Btn size="sm" onClick={handleAdd} disabled={!r.inStock}>
            {added ? "✓ Added" : r.inStock ? "+ Add" : "Unavailable"}
          </Btn>
        </div>
      </div>
    </div>
  );
}

// ── Recipes Page ───────────────────────────────────────────────────────────────
function RecipesPage({ setPage }) {
  const [filter, setFilter] = useState("All");
  const [sort, setSort] = useState("popular");
  const [search, setSearch] = useState("");

  const filters = ["All", "Veg", "Non-Veg"];
  const sorts = { popular: "Most Popular", price_asc: "Price: Low to High", price_desc: "Price: High to Low", rating: "Highest Rated" };

  let recipes = MOCK_RECIPES
    .filter(r => filter === "All" || r.category === filter)
    .filter(r => !search || r.name.toLowerCase().includes(search.toLowerCase()) || r.description.toLowerCase().includes(search.toLowerCase()));

  if (sort === "popular")    recipes.sort((a,b) => b.reviews - a.reviews);
  if (sort === "price_asc")  recipes.sort((a,b) => a.price - b.price);
  if (sort === "price_desc") recipes.sort((a,b) => b.price - a.price);
  if (sort === "rating")     recipes.sort((a,b) => b.rating - a.rating);

  return (
    <div style={{ maxWidth: 1200, margin: "0 auto", padding: "40px 24px" }}>
      <h1 className="fade-up" style={{ fontFamily: "var(--font-display)", fontSize: 34, fontWeight: 700, marginBottom: 8 }}>Our Recipes</h1>
      <p style={{ color: "var(--muted)", marginBottom: 32 }}>Pre-measured ingredients. Step-by-step guides. Delivered to your door.</p>

      {/* Controls */}
      <div style={{ display: "flex", gap: 12, marginBottom: 32, flexWrap: "wrap", alignItems: "center" }}>
        <input value={search} onChange={e => setSearch(e.target.value)} placeholder="Search recipes..."
          style={{
            flex: 1, minWidth: 180, padding: "9px 14px", borderRadius: 8,
            border: "1.5px solid var(--sand)", fontSize: 14, background: "var(--white)", outline: "none",
          }} />
        <div style={{ display: "flex", gap: 6 }}>
          {filters.map(f => (
            <button key={f} onClick={() => setFilter(f)} style={{
              padding: "8px 16px", borderRadius: 8, border: "1.5px solid",
              borderColor: filter === f ? "var(--terr)" : "var(--sand)",
              background: filter === f ? "var(--terr)" : "var(--white)",
              color: filter === f ? "#fff" : "var(--charcoal)",
              fontWeight: 500, fontSize: 13, cursor: "pointer", transition: "all .2s",
            }}>{f}</button>
          ))}
        </div>
        <select value={sort} onChange={e => setSort(e.target.value)} style={{
          padding: "9px 14px", borderRadius: 8, border: "1.5px solid var(--sand)",
          fontSize: 13, background: "var(--white)", cursor: "pointer",
        }}>
          {Object.entries(sorts).map(([val, label]) => <option key={val} value={val}>{label}</option>)}
        </select>
      </div>

      <p style={{ fontSize: 13, color: "var(--muted)", marginBottom: 20 }}>{recipes.length} recipes found</p>

      {recipes.length === 0 ? (
        <div style={{ textAlign: "center", padding: "60px 0", color: "var(--muted)" }}>
          <div style={{ fontSize: 48, marginBottom: 16 }}>🔍</div>
          <p style={{ fontWeight: 500 }}>No recipes match your search.</p>
        </div>
      ) : (
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(290px, 1fr))", gap: 20 }}>
          {recipes.map(r => <RecipeCard key={r.id} recipe={r} setPage={setPage} />)}
        </div>
      )}
    </div>
  );
}

// ── Recipe Detail Page ─────────────────────────────────────────────────────────
function RecipeDetailPage({ id, setPage }) {
  const { add } = useCart();
  const recipe = MOCK_RECIPES.find(r => r.id === parseInt(id));
  if (!recipe) return <div style={{ padding: 40, textAlign: "center" }}>Recipe not found. <Btn onClick={() => setPage("recipes")}>Back</Btn></div>;
  const r = recipe;

  return (
    <div style={{ maxWidth: 900, margin: "0 auto", padding: "40px 24px" }}>
      <button onClick={() => setPage("recipes")} style={{ background: "none", border: "none", color: "var(--muted)", fontSize: 14, cursor: "pointer", marginBottom: 24 }}>← Back to recipes</button>

      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 40, alignItems: "start" }}>
        {/* Left */}
        <div>
          <div style={{
            background: "var(--warm)", borderRadius: 16, height: 280,
            display: "flex", alignItems: "center", justifyContent: "center", fontSize: 96, marginBottom: 24
          }}>{r.image}</div>
          <div style={{ display: "flex", gap: 12, flexWrap: "wrap" }}>
            <div style={{ flex: 1, background: "var(--white)", border: "1px solid var(--sand)", borderRadius: 10, padding: "14px 16px", textAlign: "center" }}>
              <div style={{ fontSize: 20, marginBottom: 4 }}>⏱</div>
              <div style={{ fontWeight: 600, fontSize: 14 }}>{r.cookTime}</div>
              <div style={{ fontSize: 12, color: "var(--muted)" }}>Cook time</div>
            </div>
            <div style={{ flex: 1, background: "var(--white)", border: "1px solid var(--sand)", borderRadius: 10, padding: "14px 16px", textAlign: "center" }}>
              <div style={{ fontSize: 20, marginBottom: 4 }}>👥</div>
              <div style={{ fontWeight: 600, fontSize: 14 }}>{r.serves}</div>
              <div style={{ fontSize: 12, color: "var(--muted)" }}>Serves</div>
            </div>
            <div style={{ flex: 1, background: "var(--white)", border: "1px solid var(--sand)", borderRadius: 10, padding: "14px 16px", textAlign: "center" }}>
              <div style={{ fontSize: 20, marginBottom: 4 }}>⭐</div>
              <div style={{ fontWeight: 600, fontSize: 14 }}>{r.rating}</div>
              <div style={{ fontSize: 12, color: "var(--muted)" }}>{r.reviews.toLocaleString()} reviews</div>
            </div>
          </div>
        </div>

        {/* Right */}
        <div>
          <Badge color={r.category === "Veg" ? "var(--forest)" : "var(--terr)"}>{r.category}</Badge>
          <h1 style={{ fontFamily: "var(--font-display)", fontSize: 34, fontWeight: 700, margin: "10px 0 14px" }}>{r.name}</h1>
          <Stars rating={r.rating} />
          <p style={{ color: "var(--muted)", margin: "14px 0 24px", lineHeight: 1.7 }}>{r.description}</p>

          <h3 style={{ fontWeight: 600, marginBottom: 12, fontSize: 15 }}>What's in the kit</h3>
          <div style={{ display: "flex", gap: 8, flexWrap: "wrap", marginBottom: 28 }}>
            {r.ingredients.map(ing => (
              <span key={ing} style={{ background: "var(--warm)", borderRadius: 20, padding: "4px 14px", fontSize: 13, border: "1px solid var(--sand)" }}>{ing}</span>
            ))}
          </div>

          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", padding: "20px 0", borderTop: "1px solid var(--sand)", borderBottom: "1px solid var(--sand)", marginBottom: 20 }}>
            <div>
              <span style={{ fontSize: 13, color: "var(--muted)" }}>Kit price</span>
              <div style={{ fontFamily: "var(--font-display)", fontSize: 32, fontWeight: 700, color: "var(--forest)" }}>₹{r.price}</div>
            </div>
            <Badge color={r.inStock ? "var(--forest)" : "var(--muted)"}>{r.inStock ? "In stock" : "Out of stock"}</Badge>
          </div>

          <Btn size="lg" onClick={() => add(r)} disabled={!r.inStock} style={{ width: "100%", justifyContent: "center" }}>
            {r.inStock ? "🛒 Add to cart" : "Currently unavailable"}
          </Btn>
        </div>
      </div>
    </div>
  );
}

// ── Cart Page ──────────────────────────────────────────────────────────────────
function CartPage({ setPage }) {
  const { items, remove, update, total, clear } = useCart();
  const { user } = useAuth();
  const [placing, setPlacing] = useState(false);
  const [toast, setToast] = useState(null);

  const handleCheckout = async () => {
    if (!user) { setPage("login"); return; }
    setPlacing(true);
    await new Promise(r => setTimeout(r, 1500)); // simulate API call
    clear();
    setPlacing(false);
    setToast("Order placed successfully! 🎉");
    setTimeout(() => { setPage("orders"); }, 2000);
  };

  if (items.length === 0) return (
    <div style={{ maxWidth: 600, margin: "80px auto", textAlign: "center", padding: "0 24px" }}>
      <div style={{ fontSize: 72, marginBottom: 20 }}>🛒</div>
      <h2 style={{ fontFamily: "var(--font-display)", fontSize: 28, marginBottom: 12 }}>Your cart is empty</h2>
      <p style={{ color: "var(--muted)", marginBottom: 28 }}>Add some delicious recipe kits to get started.</p>
      <Btn size="lg" onClick={() => setPage("recipes")}>Browse Recipes</Btn>
    </div>
  );

  return (
    <div style={{ maxWidth: 900, margin: "0 auto", padding: "40px 24px" }}>
      {toast && <Toast msg={toast} onClose={() => setToast(null)} />}
      <h1 style={{ fontFamily: "var(--font-display)", fontSize: 32, fontWeight: 700, marginBottom: 32 }}>Your Cart</h1>

      <div style={{ display: "grid", gridTemplateColumns: "1fr 320px", gap: 32, alignItems: "start" }}>
        {/* Items */}
        <div style={{ display: "flex", flexDirection: "column", gap: 14 }}>
          {items.map(item => (
            <div key={item.id} style={{
              background: "var(--white)", border: "1.5px solid var(--sand)", borderRadius: 12,
              padding: "16px 20px", display: "flex", alignItems: "center", gap: 16,
            }}>
              <div style={{ fontSize: 40, minWidth: 52, textAlign: "center" }}>{item.image}</div>
              <div style={{ flex: 1 }}>
                <div style={{ fontWeight: 600, fontSize: 16 }}>{item.name}</div>
                <div style={{ fontSize: 13, color: "var(--muted)" }}>₹{item.price} per kit · serves {item.serves}</div>
              </div>
              {/* Qty */}
              <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                <button onClick={() => update(item.id, item.qty - 1)} style={{ width: 28, height: 28, borderRadius: "50%", border: "1.5px solid var(--sand)", background: "var(--warm)", cursor: "pointer", fontSize: 16, display: "flex", alignItems: "center", justifyContent: "center" }}>−</button>
                <span style={{ fontWeight: 600, minWidth: 20, textAlign: "center" }}>{item.qty}</span>
                <button onClick={() => update(item.id, item.qty + 1)} style={{ width: 28, height: 28, borderRadius: "50%", border: "1.5px solid var(--sand)", background: "var(--warm)", cursor: "pointer", fontSize: 16, display: "flex", alignItems: "center", justifyContent: "center" }}>+</button>
              </div>
              <div style={{ fontWeight: 700, fontSize: 16, minWidth: 60, textAlign: "right" }}>₹{item.price * item.qty}</div>
              <button onClick={() => remove(item.id)} style={{ background: "none", border: "none", color: "var(--muted)", cursor: "pointer", fontSize: 18, padding: 4 }}>✕</button>
            </div>
          ))}
        </div>

        {/* Summary */}
        <div style={{ background: "var(--white)", border: "1.5px solid var(--sand)", borderRadius: 14, padding: "24px 22px", position: "sticky", top: 80 }}>
          <h3 style={{ fontFamily: "var(--font-display)", fontSize: 20, marginBottom: 20 }}>Order Summary</h3>
          <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 10, fontSize: 14 }}>
            <span style={{ color: "var(--muted)" }}>Subtotal</span><span>₹{total}</span>
          </div>
          <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 10, fontSize: 14 }}>
            <span style={{ color: "var(--muted)" }}>Delivery</span>
            <span style={{ color: "var(--forest)", fontWeight: 500 }}>{total >= 499 ? "Free" : "₹49"}</span>
          </div>
          {total < 499 && <p style={{ fontSize: 12, color: "var(--muted)", marginBottom: 10 }}>Add ₹{499 - total} more for free delivery</p>}
          <div style={{ borderTop: "1px solid var(--sand)", paddingTop: 14, marginTop: 14, display: "flex", justifyContent: "space-between", fontWeight: 700, fontSize: 18, marginBottom: 20 }}>
            <span>Total</span>
            <span style={{ color: "var(--forest)" }}>₹{total < 499 ? total + 49 : total}</span>
          </div>
          <Btn size="lg" onClick={handleCheckout} disabled={placing} style={{ width: "100%", justifyContent: "center" }}>
            {placing ? <><Spinner /> Placing order…</> : user ? "Place Order" : "Login to Checkout"}
          </Btn>
          <button onClick={() => setPage("recipes")} style={{ width: "100%", marginTop: 10, background: "none", border: "none", color: "var(--muted)", fontSize: 13, cursor: "pointer" }}>← Continue shopping</button>
        </div>
      </div>
    </div>
  );
}

// ── Orders Page ────────────────────────────────────────────────────────────────
function OrdersPage() {
  const statusColor = { Delivered: "var(--forest)", "Out for delivery": "var(--terr)", Processing: "var(--muted)", Cancelled: "var(--danger)" };

  return (
    <div style={{ maxWidth: 760, margin: "0 auto", padding: "40px 24px" }}>
      <h1 style={{ fontFamily: "var(--font-display)", fontSize: 32, fontWeight: 700, marginBottom: 32 }}>My Orders</h1>
      {MOCK_ORDERS.length === 0 ? (
        <div style={{ textAlign: "center", padding: 60, color: "var(--muted)" }}>
          <div style={{ fontSize: 48, marginBottom: 16 }}>📦</div>
          <p>No orders yet. Start cooking!</p>
        </div>
      ) : (
        <div style={{ display: "flex", flexDirection: "column", gap: 16 }}>
          {MOCK_ORDERS.map(o => (
            <div key={o.id} style={{ background: "var(--white)", border: "1.5px solid var(--sand)", borderRadius: 14, padding: "20px 24px" }}>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 12 }}>
                <div>
                  <span style={{ fontWeight: 700, fontSize: 15 }}>{o.id}</span>
                  <span style={{ fontSize: 13, color: "var(--muted)", marginLeft: 12 }}>{o.date}</span>
                </div>
                <Badge color={statusColor[o.status] || "var(--muted)"}>{o.status}</Badge>
              </div>
              <div style={{ fontSize: 14, color: "var(--muted)", marginBottom: 14 }}>
                {o.items.join(", ")}
              </div>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                <span style={{ fontWeight: 700, fontSize: 17 }}>₹{o.total}</span>
                {o.status === "Out for delivery" && (
                  <Badge color="var(--terr)">🚚 Tracking available</Badge>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

// ── Auth Pages ─────────────────────────────────────────────────────────────────
function LoginPage({ setPage }) {
  const { login } = useAuth();
  const [email, setEmail] = useState("");
  const [pass, setPass] = useState("");
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState(null);

  const handleLogin = async () => {
    if (!email || !pass) { setErr("Please fill in all fields."); return; }
    setLoading(true); setErr(null);
    try {
      // Demo mode — in production: const data = await req("POST", "/users/login", { email, password: pass });
      await new Promise(r => setTimeout(r, 800));
      login({ name: email.split("@")[0], email }, "demo-token-xyz");
      setPage("home");
    } catch (e) {
      setErr(e.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ maxWidth: 420, margin: "60px auto", padding: "0 24px" }}>
      <div style={{ background: "var(--white)", border: "1.5px solid var(--sand)", borderRadius: 16, padding: "40px 36px" }}>
        <div style={{ textAlign: "center", marginBottom: 32 }}>
          <div style={{ fontSize: 36, marginBottom: 10 }}>🌿</div>
          <h2 style={{ fontFamily: "var(--font-display)", fontSize: 26, fontWeight: 700 }}>Welcome back</h2>
          <p style={{ color: "var(--muted)", fontSize: 14 }}>Sign in to your account</p>
        </div>
        {err && <div style={{ background: "#fde8e8", color: "var(--danger)", padding: "10px 14px", borderRadius: 8, fontSize: 13, marginBottom: 16 }}>{err}</div>}
        <Input label="Email" type="email" value={email} onChange={setEmail} placeholder="you@example.com" />
        <Input label="Password" type="password" value={pass} onChange={setPass} placeholder="••••••••" />
        <Btn size="lg" onClick={handleLogin} disabled={loading} style={{ width: "100%", justifyContent: "center", marginTop: 8 }}>
          {loading ? <><Spinner /> Signing in…</> : "Sign in"}
        </Btn>
        <p style={{ textAlign: "center", marginTop: 20, fontSize: 13, color: "var(--muted)" }}>
          No account?{" "}
          <button onClick={() => setPage("register")} style={{ background: "none", border: "none", color: "var(--terr)", fontWeight: 600, cursor: "pointer", fontSize: 13 }}>Create one</button>
        </p>
      </div>
    </div>
  );
}

function RegisterPage({ setPage }) {
  const { login } = useAuth();
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [pass, setPass] = useState("");
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState(null);

  const handleRegister = async () => {
    if (!name || !email || !pass) { setErr("Please fill in all fields."); return; }
    if (pass.length < 8) { setErr("Password must be at least 8 characters."); return; }
    setLoading(true); setErr(null);
    try {
      await new Promise(r => setTimeout(r, 900));
      login({ name, email }, "demo-token-xyz");
      setPage("home");
    } catch (e) {
      setErr(e.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ maxWidth: 420, margin: "60px auto", padding: "0 24px" }}>
      <div style={{ background: "var(--white)", border: "1.5px solid var(--sand)", borderRadius: 16, padding: "40px 36px" }}>
        <div style={{ textAlign: "center", marginBottom: 32 }}>
          <div style={{ fontSize: 36, marginBottom: 10 }}>🌿</div>
          <h2 style={{ fontFamily: "var(--font-display)", fontSize: 26, fontWeight: 700 }}>Create account</h2>
          <p style={{ color: "var(--muted)", fontSize: 14 }}>Start cooking great food at home</p>
        </div>
        {err && <div style={{ background: "#fde8e8", color: "var(--danger)", padding: "10px 14px", borderRadius: 8, fontSize: 13, marginBottom: 16 }}>{err}</div>}
        <Input label="Full name" value={name} onChange={setName} placeholder="Ayush Kulkarni" />
        <Input label="Email" type="email" value={email} onChange={setEmail} placeholder="you@example.com" />
        <Input label="Password" type="password" value={pass} onChange={setPass} placeholder="Min. 8 characters" />
        <Btn size="lg" onClick={handleRegister} disabled={loading} style={{ width: "100%", justifyContent: "center", marginTop: 8 }}>
          {loading ? <><Spinner /> Creating account…</> : "Create account"}
        </Btn>
        <p style={{ textAlign: "center", marginTop: 20, fontSize: 13, color: "var(--muted)" }}>
          Already have one?{" "}
          <button onClick={() => setPage("login")} style={{ background: "none", border: "none", color: "var(--terr)", fontWeight: 600, cursor: "pointer", fontSize: 13 }}>Sign in</button>
        </p>
      </div>
    </div>
  );
}

// ── Footer ─────────────────────────────────────────────────────────────────────
function Footer() {
  return (
    <footer style={{ background: "var(--forest2)", color: "rgba(255,255,255,.75)", marginTop: 80, padding: "48px 24px 32px" }}>
      <div style={{ maxWidth: 1200, margin: "0 auto" }}>
        <div style={{ display: "flex", justifyContent: "space-between", flexWrap: "wrap", gap: 32, marginBottom: 40 }}>
          <div>
            <div style={{ fontFamily: "var(--font-display)", fontSize: 22, color: "#fff", marginBottom: 10 }}>🌿 RecipeEcom</div>
            <p style={{ fontSize: 14, maxWidth: 260, lineHeight: 1.7 }}>Chef-curated recipe kits with pre-measured ingredients, delivered to your door.</p>
          </div>
          {[["Company", ["About us", "Careers", "Blog"]], ["Support", ["FAQ", "Contact", "Returns"]], ["Legal", ["Privacy", "Terms", "Cookies"]]].map(([section, links]) => (
            <div key={section}>
              <div style={{ fontWeight: 600, color: "#fff", marginBottom: 12, fontSize: 14 }}>{section}</div>
              {links.map(l => <div key={l} style={{ fontSize: 14, marginBottom: 8, cursor: "pointer" }}>{l}</div>)}
            </div>
          ))}
        </div>
        <div style={{ borderTop: "1px solid rgba(255,255,255,.1)", paddingTop: 24, fontSize: 13, textAlign: "center" }}>
          © 2025 RecipeEcom · Made with 🌿 in Bengaluru
        </div>
      </div>
    </footer>
  );
}

// ── Root App ───────────────────────────────────────────────────────────────────
export default function App() {
  const [page, setPage] = useState("home");

  const renderPage = () => {
    if (page === "home")     return <HomePage setPage={setPage} />;
    if (page === "recipes")  return <RecipesPage setPage={setPage} />;
    if (page === "cart")     return <CartPage setPage={setPage} />;
    if (page === "orders")   return <OrdersPage />;
    if (page === "login")    return <LoginPage setPage={setPage} />;
    if (page === "register") return <RegisterPage setPage={setPage} />;
    if (page.startsWith("recipe-")) {
      const id = page.replace("recipe-", "");
      return <RecipeDetailPage id={id} setPage={setPage} />;
    }
    return <HomePage setPage={setPage} />;
  };

  return (
    <AuthProvider>
      <CartProvider>
        <FontStyle />
        <div style={{ minHeight: "100vh", display: "flex", flexDirection: "column" }}>
          <Nav page={page} setPage={setPage} />
          <main style={{ flex: 1 }}>{renderPage()}</main>
          <Footer />
        </div>
      </CartProvider>
    </AuthProvider>
  );
}
