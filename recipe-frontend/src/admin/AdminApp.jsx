import { useState, useEffect, useCallback, createContext, useContext } from "react";

// ─────────────────────────────────────────────────────────────────────────────
// CONFIG  — admin service runs on 8087, gateway proxies /api/admin/*
// All admin endpoints go through the gateway with a valid ADMIN-role JWT
// ─────────────────────────────────────────────────────────────────────────────
const API = "http://localhost:8080/api";

// ─────────────────────────────────────────────────────────────────────────────
// API CLIENT
// ─────────────────────────────────────────────────────────────────────────────
class ApiErr extends Error {
  constructor(msg, status) { super(msg); this.status = status; }
}

async function api(method, path, body, token) {
  if (!path) return null;
  const h = { "Content-Type": "application/json" };
  if (token) h["Authorization"] = `Bearer ${token}`;
  let res;
  try {
    res = await fetch(`${API}${path}`, {
      method, headers: h,
      body: body ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new ApiErr("Cannot reach server. Is the backend running?", 0);
  }
  if (res.status === 204) return null;
  const d = await res.json().catch(() => ({}));
  if (!res.ok) throw new ApiErr(d.message || d.error || `HTTP ${res.status}`, res.status);
  return d;
}

const GET    = (p, t)    => api("GET",    p, null, t);
const POST   = (p, b, t) => api("POST",   p, b,    t);
const PUT    = (p, b, t) => api("PUT",    p, b,    t);
const DELETE = (p, t)    => api("DELETE", p, null, t);

// ─────────────────────────────────────────────────────────────────────────────
// NORMALISE  — handles varied Spring Boot DTO field names
// ─────────────────────────────────────────────────────────────────────────────
const nr = r => ({
  id:          r.id ?? r.recipeId,
  name:        r.name ?? r.title ?? r.recipeName ?? "Untitled",
  category:    r.category ?? (r.vegetarian ? "Veg" : "Non-Veg") ?? "–",
  price:       Number(r.price ?? r.kitPrice ?? 0),
  cookTime:    r.cookTime ?? r.cookingTime ?? "–",
  description: r.description ?? "",
  ingredients: Array.isArray(r.ingredients) ? r.ingredients : [],
  serves:      r.serves ?? r.servings ?? 2,
  inStock:     r.inStock ?? r.available ?? true,
  image:       r.image ?? r.imageUrl ?? "🍽️",
  rating:      Number(r.rating ?? r.averageRating ?? 0),
});

const no = o => ({
  id:       o.id ?? o.orderId,
  userId:   o.userId ?? o.user_id ?? "–",
  date:     o.createdAt ?? o.orderDate ?? o.date ?? "",
  items:    Array.isArray(o.items) ? o.items : [],
  total:    Number(o.totalAmount ?? o.total ?? 0),
  status:   o.status ?? o.orderStatus ?? "PENDING",
  delivery: Number(o.deliveryFee ?? 0),
});

const nu = u => ({
  id:      u.id ?? u.userId,
  name:    u.name ?? u.username ?? "–",
  email:   u.email ?? "–",
  role:    u.role ?? u.userRole ?? "USER",
  joined:  u.createdAt ?? u.joinedAt ?? u.date ?? "",
  active:  u.active ?? u.enabled ?? true,
});

const ni = i => ({
  id:        i.id ?? i.inventoryId,
  recipeId:  i.recipeId ?? i.recipe_id ?? "–",
  name:      i.recipeName ?? i.name ?? "–",
  quantity:  Number(i.quantity ?? i.qty ?? i.stock ?? 0),
  threshold: Number(i.threshold ?? i.minStock ?? i.lowStockThreshold ?? 5),
  inStock:   i.inStock ?? (Number(i.quantity ?? i.qty ?? 0) > 0),
});

// ─────────────────────────────────────────────────────────────────────────────
// CONTEXTS
// ─────────────────────────────────────────────────────────────────────────────
const AuthCtx  = createContext(null);
const ToastCtx = createContext(null);
const useAuth  = () => useContext(AuthCtx);
const useToast = () => useContext(ToastCtx);

function AuthProvider({ children }) {
  const [user,  setUser]  = useState(() => { try { return JSON.parse(localStorage.getItem("adm_u")); } catch { return null; } });
  const [token, setToken] = useState(() => localStorage.getItem("adm_t") || null);
  const login  = useCallback((u, t) => { setUser(u); setToken(t); localStorage.setItem("adm_u", JSON.stringify(u)); localStorage.setItem("adm_t", t); }, []);
  const logout = useCallback(()     => { setUser(null); setToken(null); localStorage.removeItem("adm_u"); localStorage.removeItem("adm_t"); }, []);
  return <AuthCtx.Provider value={{ user, token, login, logout }}>{children}</AuthCtx.Provider>;
}

function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);
  const show = useCallback((msg, type = "success") => {
    const id = Date.now();
    setToasts(p => [...p, { id, msg, type }]);
    setTimeout(() => setToasts(p => p.filter(t => t.id !== id)), 3800);
  }, []);
  const bg = { success: "#1a7a4a", error: "#c0392b", info: "#1a5fa8", warn: "#b7770d" };
  return (
    <ToastCtx.Provider value={{ show }}>
      {children}
      <div style={{ position: "fixed", bottom: 24, right: 24, zIndex: 9999, display: "flex", flexDirection: "column", gap: 10 }}>
        {toasts.map(t => (
          <div key={t.id} style={{
            background: bg[t.type] || bg.success, color: "#fff",
            padding: "11px 18px", borderRadius: 9, fontWeight: 500, fontSize: 14,
            boxShadow: "0 4px 20px rgba(0,0,0,.35)", animation: "slideIn .3s ease both", maxWidth: 340,
          }}>{t.msg}</div>
        ))}
      </div>
    </ToastCtx.Provider>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// DATA HOOK
// ─────────────────────────────────────────────────────────────────────────────
function useData(path, token) {
  const [data,    setData]    = useState(null);
  const [loading, setLoading] = useState(!!path);
  const [error,   setError]   = useState(null);

  const load = useCallback(async () => {
    if (!path) return;
    setLoading(true); setError(null);
    try   { setData(await GET(path, token)); }
    catch (e) { setError(e.message); }
    finally   { setLoading(false); }
  }, [path, token]);

  useEffect(() => { load(); }, [load]);
  return { data, loading, error, reload: load, setData };
}

// ─────────────────────────────────────────────────────────────────────────────
// GLOBAL STYLES
// ─────────────────────────────────────────────────────────────────────────────
const GS = () => (
  <style>{`
    @import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=Playfair+Display:wght@700&display=swap');
    *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
    :root {
      --bg:      #0f1117;
      --surface: #181c27;
      --card:    #1e2333;
      --border:  #2a3045;
      --border2: #343d56;
      --text:    #e8ecf4;
      --muted:   #7a859e;
      --accent:  #4f8ef7;
      --accent2: #3a7af0;
      --green:   #22c55e;
      --red:     #ef4444;
      --orange:  #f59e0b;
      --purple:  #a78bfa;
      --terr:    #e07c4a;
    }
    body { background: var(--bg); color: var(--text); font-family: 'Inter', system-ui, sans-serif; font-size: 14px; line-height: 1.5; }
    button { cursor: pointer; font-family: inherit; }
    input, select, textarea { font-family: inherit; }
    ::-webkit-scrollbar { width: 5px; }
    ::-webkit-scrollbar-track { background: var(--bg); }
    ::-webkit-scrollbar-thumb { background: var(--border2); border-radius: 3px; }
    @keyframes slideIn { from { opacity:0; transform:translateX(12px); } to { opacity:1; transform:none; } }
    @keyframes fadeUp  { from { opacity:0; transform:translateY(10px); } to { opacity:1; transform:none; } }
    @keyframes spin    { to   { transform: rotate(360deg); } }
    @keyframes shimmer { 0%   { background-position: -500px 0; } 100% { background-position: 500px 0; } }
    .fu  { animation: fadeUp .35s ease both; }
    .fu2 { animation: fadeUp .35s .06s ease both; }
    .sk  {
      background: linear-gradient(90deg, var(--card) 25%, var(--border) 50%, var(--card) 75%);
      background-size: 500px 100%;
      animation: shimmer 1.5s infinite;
      border-radius: 6px;
    }
  `}</style>
);

// ─────────────────────────────────────────────────────────────────────────────
// PRIMITIVES
// ─────────────────────────────────────────────────────────────────────────────
function Btn({ children, onClick, variant = "primary", size = "md", disabled, full, style }) {
  const pad = { sm: "5px 12px", md: "8px 16px", lg: "11px 22px" }[size];
  const fs  = { sm: 12, md: 13, lg: 14 }[size];
  const v   = {
    primary: { background: "var(--accent)",  color: "#fff" },
    ghost:   { background: "transparent",    color: "var(--muted)", border: "1px solid var(--border2)" },
    danger:  { background: "var(--red)",     color: "#fff" },
    success: { background: "var(--green)",   color: "#fff" },
    orange:  { background: "var(--orange)",  color: "#000" },
  };
  return (
    <button onClick={onClick} disabled={disabled}
      style={{ ...v[variant], padding: pad, fontSize: fs, borderRadius: 7, border: "none", fontWeight: 500,
        display: "inline-flex", alignItems: "center", gap: 5, opacity: disabled ? .45 : 1,
        cursor: disabled ? "not-allowed" : "pointer", transition: "opacity .15s, transform .1s",
        width: full ? "100%" : undefined, justifyContent: full ? "center" : undefined, ...style }}
      onMouseEnter={e => { if (!disabled) e.currentTarget.style.opacity = ".82"; }}
      onMouseLeave={e => { e.currentTarget.style.opacity = "1"; }}>
      {children}
    </button>
  );
}

function Inp({ label, error, type = "text", ...rest }) {
  return (
    <div style={{ marginBottom: 14 }}>
      {label && <label style={{ display: "block", fontWeight: 500, fontSize: 12, marginBottom: 5, color: "var(--muted)", textTransform: "uppercase", letterSpacing: .5 }}>{label}</label>}
      <input type={type} {...rest}
        style={{ width: "100%", padding: "9px 12px", borderRadius: 7, fontSize: 13,
          border: `1px solid ${error ? "var(--red)" : "var(--border2)"}`,
          background: "var(--bg)", color: "var(--text)", outline: "none", transition: "border-color .15s" }}
        onFocus={e => { if (!error) e.target.style.borderColor = "var(--accent)"; }}
        onBlur  ={e => { if (!error) e.target.style.borderColor = "var(--border2)"; }}
      />
      {error && <p style={{ fontSize: 11, color: "var(--red)", marginTop: 3 }}>{error}</p>}
    </div>
  );
}

function Sel({ label, value, onChange, children }) {
  return (
    <div style={{ marginBottom: 14 }}>
      {label && <label style={{ display: "block", fontWeight: 500, fontSize: 12, marginBottom: 5, color: "var(--muted)", textTransform: "uppercase", letterSpacing: .5 }}>{label}</label>}
      <select value={value} onChange={e => onChange(e.target.value)}
        style={{ width: "100%", padding: "9px 12px", borderRadius: 7, fontSize: 13, border: "1px solid var(--border2)", background: "var(--bg)", color: "var(--text)", outline: "none", cursor: "pointer" }}>
        {children}
      </select>
    </div>
  );
}

function Spin({ size = 16, color = "var(--accent)" }) {
  return <div style={{ width: size, height: size, border: `2px solid ${color}30`, borderTopColor: color, borderRadius: "50%", animation: "spin .65s linear infinite", flexShrink: 0 }} />;
}

function Badge({ children, color = "var(--accent)" }) {
  return <span style={{ background: color + "25", color, borderRadius: 5, fontSize: 11, padding: "2px 8px", fontWeight: 600 }}>{children}</span>;
}

function ErrBox({ msg, onRetry }) {
  return (
    <div style={{ background: "#c0392b18", border: "1px solid #c0392b40", borderRadius: 9, padding: "12px 16px", display: "flex", alignItems: "center", justifyContent: "space-between", gap: 12 }}>
      <span style={{ color: "var(--red)", fontSize: 13 }}>⚠ {msg}</span>
      {onRetry && <Btn size="sm" variant="danger" onClick={onRetry}>Retry</Btn>}
    </div>
  );
}

function SkRow() {
  return (
    <tr>
      {[80, 140, 80, 100, 80, 70].map((w, i) => (
        <td key={i} style={{ padding: "12px 14px" }}><div className="sk" style={{ height: 13, width: w }} /></td>
      ))}
    </tr>
  );
}

function StatCard({ icon, label, value, sub, color = "var(--accent)" }) {
  return (
    <div className="fu" style={{ background: "var(--card)", border: "1px solid var(--border)", borderRadius: 12, padding: "20px 22px" }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: 14 }}>
        <span style={{ fontSize: 22 }}>{icon}</span>
        <span style={{ background: color + "20", color, borderRadius: 7, padding: "3px 9px", fontSize: 11, fontWeight: 600 }}>{sub}</span>
      </div>
      <div style={{ fontSize: 28, fontWeight: 700, color: "var(--text)", marginBottom: 3 }}>{value}</div>
      <div style={{ fontSize: 12, color: "var(--muted)" }}>{label}</div>
    </div>
  );
}

// Confirmation modal
function Confirm({ msg, onConfirm, onCancel }) {
  return (
    <div style={{ position: "fixed", inset: 0, background: "rgba(0,0,0,.65)", zIndex: 1000, display: "flex", alignItems: "center", justifyContent: "center" }}>
      <div style={{ background: "var(--card)", border: "1px solid var(--border)", borderRadius: 12, padding: "28px 32px", maxWidth: 380, width: "90%", textAlign: "center" }}>
        <div style={{ fontSize: 36, marginBottom: 14 }}>⚠️</div>
        <p style={{ fontSize: 14, color: "var(--text)", marginBottom: 22, lineHeight: 1.6 }}>{msg}</p>
        <div style={{ display: "flex", gap: 10, justifyContent: "center" }}>
          <Btn variant="ghost" onClick={onCancel}>Cancel</Btn>
          <Btn variant="danger" onClick={onConfirm}>Confirm</Btn>
        </div>
      </div>
    </div>
  );
}

// Modal wrapper
function Modal({ title, onClose, children, width = 520 }) {
  return (
    <div style={{ position: "fixed", inset: 0, background: "rgba(0,0,0,.7)", zIndex: 1000, display: "flex", alignItems: "center", justifyContent: "center", padding: 24 }}>
      <div style={{ background: "var(--card)", border: "1px solid var(--border)", borderRadius: 14, width: "100%", maxWidth: width, maxHeight: "90vh", overflowY: "auto" }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", padding: "20px 24px", borderBottom: "1px solid var(--border)" }}>
          <h3 style={{ fontSize: 16, fontWeight: 600 }}>{title}</h3>
          <button onClick={onClose} style={{ background: "none", border: "none", color: "var(--muted)", fontSize: 20, cursor: "pointer", lineHeight: 1 }}>×</button>
        </div>
        <div style={{ padding: "22px 24px" }}>{children}</div>
      </div>
    </div>
  );
}

// Table wrapper
function Table({ headers, children, loading, rows = 6 }) {
  return (
    <div style={{ overflowX: "auto" }}>
      <table style={{ width: "100%", borderCollapse: "collapse" }}>
        <thead>
          <tr style={{ borderBottom: "1px solid var(--border)" }}>
            {headers.map(h => (
              <th key={h} style={{ padding: "10px 14px", textAlign: "left", fontSize: 11, fontWeight: 600, color: "var(--muted)", textTransform: "uppercase", letterSpacing: .5, whiteSpace: "nowrap" }}>{h}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {loading ? Array.from({ length: rows }).map((_, i) => <SkRow key={i} />) : children}
        </tbody>
      </table>
    </div>
  );
}

function TR({ children, onClick }) {
  return (
    <tr onClick={onClick} style={{ borderBottom: "1px solid var(--border)", transition: "background .15s", cursor: onClick ? "pointer" : undefined }}
      onMouseEnter={e => e.currentTarget.style.background = "var(--border)20"}
      onMouseLeave={e => e.currentTarget.style.background = "transparent"}>
      {children}
    </tr>
  );
}

function TD({ children, mono }) {
  return <td style={{ padding: "11px 14px", fontSize: 13, color: mono ? "var(--muted)" : "var(--text)", fontFamily: mono ? "monospace" : undefined }}>{children}</td>;
}

// ─────────────────────────────────────────────────────────────────────────────
// SIDEBAR
// ─────────────────────────────────────────────────────────────────────────────
const NAV = [
  { id: "dashboard", icon: "◈", label: "Dashboard" },
  { id: "recipes",   icon: "🍽", label: "Recipes" },
  { id: "orders",    icon: "📦", label: "Orders" },
  { id: "inventory", icon: "📊", label: "Inventory" },
  { id: "users",     icon: "👥", label: "Users" },
];

function Sidebar({ active, setActive }) {
  const { user, logout } = useAuth();
  return (
    <div style={{
      width: 220, background: "var(--surface)", borderRight: "1px solid var(--border)",
      display: "flex", flexDirection: "column", height: "100vh", position: "sticky", top: 0, flexShrink: 0,
    }}>
      {/* Logo */}
      <div style={{ padding: "22px 20px 18px", borderBottom: "1px solid var(--border)" }}>
        <div style={{ display: "flex", alignItems: "center", gap: 9 }}>
          <span style={{ fontSize: 20 }}>🌿</span>
          <div>
            <div style={{ fontFamily: "'Playfair Display', serif", fontSize: 15, fontWeight: 700, color: "var(--text)" }}>RecipeEcom</div>
            <div style={{ fontSize: 10, color: "var(--accent)", fontWeight: 600, letterSpacing: .5, textTransform: "uppercase" }}>Admin</div>
          </div>
        </div>
      </div>

      {/* Nav */}
      <nav style={{ flex: 1, padding: "12px 10px", overflowY: "auto" }}>
        {NAV.map(n => (
          <button key={n.id} onClick={() => setActive(n.id)} style={{
            width: "100%", display: "flex", alignItems: "center", gap: 10,
            padding: "9px 12px", borderRadius: 8, border: "none", textAlign: "left",
            background: active === n.id ? "var(--accent)18" : "transparent",
            color: active === n.id ? "var(--accent)" : "var(--muted)",
            fontWeight: active === n.id ? 600 : 400, fontSize: 13,
            marginBottom: 2, transition: "all .15s",
          }}
            onMouseEnter={e => { if (active !== n.id) e.currentTarget.style.background = "var(--border)40"; e.currentTarget.style.color = "var(--text)"; }}
            onMouseLeave={e => { e.currentTarget.style.background = active === n.id ? "var(--accent)18" : "transparent"; e.currentTarget.style.color = active === n.id ? "var(--accent)" : "var(--muted)"; }}>
            <span style={{ fontSize: 16, width: 20, textAlign: "center" }}>{n.icon}</span>
            {n.label}
          </button>
        ))}
      </nav>

      {/* User */}
      <div style={{ padding: "14px 16px", borderTop: "1px solid var(--border)" }}>
        <div style={{ fontSize: 12, color: "var(--muted)", marginBottom: 2 }}>{user?.email || "admin"}</div>
        <div style={{ fontSize: 11, color: "var(--accent)", marginBottom: 10 }}>{user?.role || "ADMIN"}</div>
        <Btn variant="ghost" size="sm" full onClick={logout}>Sign out</Btn>
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// HEADER
// ─────────────────────────────────────────────────────────────────────────────
function Header({ title, action }) {
  return (
    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 26 }}>
      <h1 style={{ fontSize: 22, fontWeight: 700, color: "var(--text)" }}>{title}</h1>
      {action}
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// DASHBOARD  — stats overview
// ─────────────────────────────────────────────────────────────────────────────
function Dashboard() {
  const { token } = useAuth();
  const { data: recipes,   loading: lr } = useData("/recipes?size=1",                token);
  const { data: orders,    loading: lo } = useData("/orders?size=100",               token);
  const { data: users,     loading: lu } = useData("/users?size=1",                  token);
  const { data: inventory, loading: li } = useData("/inventory",                     token);

  const totalRecipes   = recipes?.totalElements   ?? (Array.isArray(recipes)   ? recipes.length   : "–");
  const totalUsers     = users?.totalElements     ?? (Array.isArray(users)     ? users.length     : "–");
  const orderList      = orders?.content          ?? (Array.isArray(orders)    ? orders           : []);
  const inventoryList  = inventory?.content       ?? (Array.isArray(inventory) ? inventory        : []);

  const revenue        = orderList.reduce((s, o) => s + Number(o.totalAmount ?? o.total ?? 0), 0);
  const pending        = orderList.filter(o => (o.status ?? "").toUpperCase() === "PENDING").length;
  const lowStock       = inventoryList.filter(i => Number(i.quantity ?? i.qty ?? 0) <= Number(i.threshold ?? i.lowStockThreshold ?? 5)).length;

  const recentOrders   = orderList.slice(0, 5).map(no);

  const SC = { PENDING: "var(--orange)", PROCESSING: "var(--accent)", DELIVERED: "var(--green)", OUT_FOR_DELIVERY: "var(--accent)", CANCELLED: "var(--red)" };

  return (
    <div>
      <Header title="Dashboard" />

      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(190px, 1fr))", gap: 16, marginBottom: 28 }}>
        <StatCard icon="🍽" label="Total Recipes"   value={lr ? "…" : totalRecipes} sub="catalog"     color="var(--accent)" />
        <StatCard icon="📦" label="Total Orders"    value={lo ? "…" : orderList.length} sub="all time" color="var(--purple)" />
        <StatCard icon="👥" label="Registered Users" value={lu ? "…" : totalUsers}  sub="accounts"    color="var(--green)"  />
        <StatCard icon="💰" label="Total Revenue"   value={lo ? "…" : `₹${revenue.toLocaleString("en-IN")}`} sub="gross" color="var(--terr)" />
        <StatCard icon="⏳" label="Pending Orders"  value={lo ? "…" : pending}      sub="need action" color="var(--orange)" />
        <StatCard icon="⚠️" label="Low Stock Items" value={li ? "…" : lowStock}     sub="inventory"   color="var(--red)"   />
      </div>

      {/* Recent orders */}
      <div style={{ background: "var(--card)", border: "1px solid var(--border)", borderRadius: 12, overflow: "hidden" }}>
        <div style={{ padding: "16px 20px", borderBottom: "1px solid var(--border)", fontWeight: 600, fontSize: 14 }}>Recent Orders</div>
        <Table headers={["Order ID", "User", "Items", "Total", "Status", "Date"]} loading={lo} rows={5}>
          {recentOrders.map(o => (
            <TR key={o.id}>
              <TD mono>#{o.id}</TD>
              <TD mono>{o.userId}</TD>
              <TD>{Array.isArray(o.items) ? o.items.length + " item(s)" : "–"}</TD>
              <TD>₹{o.total}</TD>
              <TD><Badge color={SC[(o.status ?? "").toUpperCase()] || "var(--muted)"}>{o.status}</Badge></TD>
              <TD mono>{o.date ? new Date(o.date).toLocaleDateString("en-IN", { day: "numeric", month: "short" }) : "–"}</TD>
            </TR>
          ))}
        </Table>
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// RECIPES  — CRUD
// ─────────────────────────────────────────────────────────────────────────────
const EMPTY_RECIPE = { name: "", category: "Veg", price: "", cookTime: "", description: "", serves: 2, inStock: true, image: "🍽️" };

function Recipes() {
  const { token } = useAuth();
  const { show }  = useToast();
  const { data, loading, error, reload } = useData("/recipes?size=50", token);
  const [modal,   setModal]   = useState(null);  // null | "add" | recipe object
  const [form,    setForm]    = useState(EMPTY_RECIPE);
  const [saving,  setSaving]  = useState(false);
  const [confirm, setConfirm] = useState(null);
  const [search,  setSearch]  = useState("");

  const recipes = (data?.content ?? data ?? []).map(nr);
  const filtered = recipes.filter(r => !search || r.name.toLowerCase().includes(search.toLowerCase()));

  const openAdd  = () => { setForm(EMPTY_RECIPE); setModal("add"); };
  const openEdit = r  => { setForm({ ...r, price: String(r.price), serves: String(r.serves) }); setModal(r); };

  const save = async () => {
    if (!form.name || !form.price) { show("Name and price are required.", "error"); return; }
    setSaving(true);
    try {
      const body = { ...form, price: Number(form.price), serves: Number(form.serves) };
      if (modal === "add") {
        await POST("/recipes", body, token);
        show("Recipe created ✓");
      } else {
        await PUT(`/recipes/${modal.id}`, body, token);
        show("Recipe updated ✓");
      }
      reload(); setModal(null);
    } catch (e) { show(e.message, "error"); }
    finally { setSaving(false); }
  };

  const del = async (id) => {
    try { await DELETE(`/recipes/${id}`, token); show("Recipe deleted."); reload(); }
    catch (e) { show(e.message, "error"); }
    finally { setConfirm(null); }
  };

  const f = (k, v) => setForm(p => ({ ...p, [k]: v }));

  return (
    <div>
      <Header title="Recipes"
        action={
          <div style={{ display: "flex", gap: 10, alignItems: "center" }}>
            <input value={search} onChange={e => setSearch(e.target.value)} placeholder="Search…"
              style={{ padding: "7px 12px", borderRadius: 7, border: "1px solid var(--border2)", background: "var(--bg)", color: "var(--text)", fontSize: 13, outline: "none", width: 200 }} />
            <Btn onClick={openAdd}>+ Add Recipe</Btn>
          </div>
        }
      />

      {error && <div style={{ marginBottom: 16 }}><ErrBox msg={error} onRetry={reload} /></div>}

      <div style={{ background: "var(--card)", border: "1px solid var(--border)", borderRadius: 12, overflow: "hidden" }}>
        <Table headers={["", "Name", "Category", "Price", "Cook Time", "Serves", "Stock", "Actions"]} loading={loading}>
          {filtered.map(r => (
            <TR key={r.id}>
              <TD><span style={{ fontSize: 22 }}>{r.image?.startsWith("http") ? "🍽" : r.image}</span></TD>
              <TD><span style={{ fontWeight: 500 }}>{r.name}</span></TD>
              <TD><Badge color={r.category === "Veg" ? "var(--green)" : "var(--terr)"}>{r.category}</Badge></TD>
              <TD>₹{r.price}</TD>
              <TD mono>{r.cookTime}</TD>
              <TD mono>{r.serves}</TD>
              <TD><Badge color={r.inStock ? "var(--green)" : "var(--red)"}>{r.inStock ? "In Stock" : "Out"}</Badge></TD>
              <TD>
                <div style={{ display: "flex", gap: 6 }}>
                  <Btn size="sm" variant="ghost" onClick={() => openEdit(r)}>Edit</Btn>
                  <Btn size="sm" variant="danger" onClick={() => setConfirm(r.id)}>Del</Btn>
                </div>
              </TD>
            </TR>
          ))}
        </Table>
        {!loading && filtered.length === 0 && (
          <div style={{ padding: "40px", textAlign: "center", color: "var(--muted)" }}>No recipes found.</div>
        )}
      </div>

      {/* Add / Edit modal */}
      {modal !== null && (
        <Modal title={modal === "add" ? "Add Recipe" : "Edit Recipe"} onClose={() => setModal(null)}>
          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "0 16px" }}>
            <div style={{ gridColumn: "1/-1" }}><Inp label="Name" value={form.name} onChange={e => f("name", e.target.value)} placeholder="Butter Chicken" /></div>
            <Sel label="Category" value={form.category} onChange={v => f("category", v)}>
              <option>Veg</option><option>Non-Veg</option>
            </Sel>
            <Inp label="Price (₹)" type="number" value={form.price} onChange={e => f("price", e.target.value)} placeholder="349" />
            <Inp label="Cook Time" value={form.cookTime} onChange={e => f("cookTime", e.target.value)} placeholder="35 min" />
            <Inp label="Serves" type="number" value={form.serves} onChange={e => f("serves", e.target.value)} placeholder="2" />
            <Inp label="Image (emoji or URL)" value={form.image} onChange={e => f("image", e.target.value)} placeholder="🍛" />
            <div style={{ gridColumn: "1/-1" }}>
              <label style={{ display: "block", fontWeight: 500, fontSize: 12, marginBottom: 5, color: "var(--muted)", textTransform: "uppercase", letterSpacing: .5 }}>Description</label>
              <textarea value={form.description} onChange={e => f("description", e.target.value)} rows={3}
                style={{ width: "100%", padding: "9px 12px", borderRadius: 7, border: "1px solid var(--border2)", background: "var(--bg)", color: "var(--text)", fontSize: 13, outline: "none", resize: "vertical" }} />
            </div>
            <div style={{ gridColumn: "1/-1", display: "flex", alignItems: "center", gap: 10, marginBottom: 14 }}>
              <input type="checkbox" id="instock" checked={!!form.inStock} onChange={e => f("inStock", e.target.checked)} />
              <label htmlFor="instock" style={{ fontSize: 13, cursor: "pointer" }}>In stock</label>
            </div>
          </div>
          <div style={{ display: "flex", gap: 10, justifyContent: "flex-end", paddingTop: 8, borderTop: "1px solid var(--border)" }}>
            <Btn variant="ghost" onClick={() => setModal(null)}>Cancel</Btn>
            <Btn onClick={save} disabled={saving}>{saving ? <><Spin size={13} color="#fff" /> Saving…</> : modal === "add" ? "Create Recipe" : "Save Changes"}</Btn>
          </div>
        </Modal>
      )}

      {confirm && <Confirm msg="Delete this recipe? This cannot be undone." onConfirm={() => del(confirm)} onCancel={() => setConfirm(null)} />}
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// ORDERS  — view + update status
// ─────────────────────────────────────────────────────────────────────────────
const ORDER_STATUSES = ["PENDING", "PROCESSING", "OUT_FOR_DELIVERY", "DELIVERED", "CANCELLED"];
const SC = { PENDING: "var(--orange)", PROCESSING: "var(--accent)", OUT_FOR_DELIVERY: "var(--purple)", DELIVERED: "var(--green)", CANCELLED: "var(--red)" };

function Orders() {
  const { token } = useAuth();
  const { show }  = useToast();
  const { data, loading, error, reload } = useData("/orders?size=100&sort=createdAt,desc", token);
  const [detail,  setDetail]  = useState(null);
  const [filter,  setFilter]  = useState("ALL");
  const [search,  setSearch]  = useState("");
  const [updating, setUpdating] = useState(null);

  const all    = (data?.content ?? data ?? []).map(no);
  const orders = all
    .filter(o => filter === "ALL" || o.status?.toUpperCase() === filter)
    .filter(o => !search || String(o.id).includes(search) || String(o.userId).includes(search));

  const updateStatus = async (id, status) => {
    setUpdating(id);
    try {
      // PATCH or PUT /orders/{id}/status — adjust to your controller
      await PUT(`/orders/${id}/status`, { status }, token);
      show(`Order #${id} → ${status}`);
      reload();
      if (detail?.id === id) setDetail(p => ({ ...p, status }));
    } catch (e) { show(e.message, "error"); }
    finally { setUpdating(null); }
  };

  return (
    <div>
      <Header title="Orders" action={
        <div style={{ display: "flex", gap: 8 }}>
          <input value={search} onChange={e => setSearch(e.target.value)} placeholder="Order ID or user…"
            style={{ padding: "7px 12px", borderRadius: 7, border: "1px solid var(--border2)", background: "var(--bg)", color: "var(--text)", fontSize: 13, outline: "none", width: 190 }} />
          <select value={filter} onChange={e => setFilter(e.target.value)}
            style={{ padding: "7px 12px", borderRadius: 7, border: "1px solid var(--border2)", background: "var(--bg)", color: "var(--text)", fontSize: 13, cursor: "pointer" }}>
            <option value="ALL">All statuses</option>
            {ORDER_STATUSES.map(s => <option key={s} value={s}>{s}</option>)}
          </select>
        </div>
      } />

      {error && <div style={{ marginBottom: 16 }}><ErrBox msg={error} onRetry={reload} /></div>}

      <div style={{ background: "var(--card)", border: "1px solid var(--border)", borderRadius: 12, overflow: "hidden" }}>
        <Table headers={["Order ID", "User ID", "Items", "Total", "Status", "Date", "Actions"]} loading={loading}>
          {orders.map(o => (
            <TR key={o.id} onClick={() => setDetail(o)}>
              <TD mono>#{o.id}</TD>
              <TD mono>{o.userId}</TD>
              <TD>{Array.isArray(o.items) ? o.items.length + " item(s)" : "–"}</TD>
              <TD>₹{o.total}</TD>
              <TD><Badge color={SC[o.status?.toUpperCase()] || "var(--muted)"}>{o.status}</Badge></TD>
              <TD mono>{o.date ? new Date(o.date).toLocaleDateString("en-IN", { day: "numeric", month: "short", year: "numeric" }) : "–"}</TD>
              <TD>
                <select value={o.status ?? ""} onClick={e => e.stopPropagation()}
                  onChange={e => updateStatus(o.id, e.target.value)}
                  disabled={updating === o.id}
                  style={{ padding: "4px 8px", borderRadius: 6, border: "1px solid var(--border2)", background: "var(--bg)", color: "var(--text)", fontSize: 12, cursor: "pointer" }}>
                  {ORDER_STATUSES.map(s => <option key={s} value={s}>{s}</option>)}
                </select>
              </TD>
            </TR>
          ))}
        </Table>
        {!loading && orders.length === 0 && <div style={{ padding: 40, textAlign: "center", color: "var(--muted)" }}>No orders found.</div>}
      </div>

      {/* Order detail modal */}
      {detail && (
        <Modal title={`Order #${detail.id}`} onClose={() => setDetail(null)}>
          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "12px 24px", marginBottom: 18 }}>
            {[["Order ID", `#${detail.id}`], ["User ID", detail.userId], ["Total", `₹${detail.total}`], ["Delivery Fee", `₹${detail.delivery}`], ["Status", detail.status], ["Date", detail.date ? new Date(detail.date).toLocaleString("en-IN") : "–"]].map(([l, v]) => (
              <div key={l}>
                <div style={{ fontSize: 11, color: "var(--muted)", marginBottom: 3, textTransform: "uppercase", letterSpacing: .5 }}>{l}</div>
                <div style={{ fontSize: 13, fontWeight: 500 }}>{v}</div>
              </div>
            ))}
          </div>
          <div>
            <div style={{ fontSize: 11, color: "var(--muted)", marginBottom: 8, textTransform: "uppercase", letterSpacing: .5 }}>Items</div>
            {Array.isArray(detail.items) && detail.items.length > 0 ? (
              <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
                {detail.items.map((item, i) => (
                  <div key={i} style={{ background: "var(--bg)", borderRadius: 8, padding: "10px 14px", display: "flex", justifyContent: "space-between" }}>
                    <span style={{ fontSize: 13 }}>{item.recipeName ?? item.name ?? `Item ${i + 1}`}</span>
                    <span style={{ fontSize: 13, color: "var(--muted)" }}>×{item.quantity ?? item.qty ?? 1} · ₹{item.unitPrice ?? item.price ?? 0}</span>
                  </div>
                ))}
              </div>
            ) : <p style={{ fontSize: 13, color: "var(--muted)" }}>Item details not available.</p>}
          </div>
          <div style={{ marginTop: 20, paddingTop: 16, borderTop: "1px solid var(--border)" }}>
            <div style={{ fontSize: 12, color: "var(--muted)", marginBottom: 8 }}>Update status</div>
            <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
              {ORDER_STATUSES.map(s => (
                <Btn key={s} size="sm"
                  variant={detail.status === s ? "primary" : "ghost"}
                  disabled={updating === detail.id}
                  onClick={() => updateStatus(detail.id, s)}
                  style={{ fontSize: 11 }}>
                  {s}
                </Btn>
              ))}
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// INVENTORY  — view stock levels + update quantity
// ─────────────────────────────────────────────────────────────────────────────
function Inventory() {
  const { token } = useAuth();
  const { show }  = useToast();
  const { data, loading, error, reload } = useData("/inventory", token);
  const [editing, setEditing] = useState(null);
  const [qty, setQty]     = useState("");
  const [saving, setSaving] = useState(false);
  const [search, setSearch] = useState("");

  const items = (data?.content ?? data ?? []).map(ni);
  const filtered = items.filter(i => !search || i.name.toLowerCase().includes(search.toLowerCase()));
  const lowStock = items.filter(i => i.quantity <= i.threshold);

  const openEdit = i => { setEditing(i); setQty(String(i.quantity)); };

  const save = async () => {
    if (!editing) return;
    setSaving(true);
    try {
      // PUT /inventory/{id} or /inventory/{id}/quantity — adjust to your InventoryService
      await PUT(`/inventory/${editing.id}`, { quantity: Number(qty), recipeId: editing.recipeId }, token);
      show(`Stock updated for ${editing.name}`);
      reload(); setEditing(null);
    } catch (e) { show(e.message, "error"); }
    finally { setSaving(false); }
  };

  return (
    <div>
      <Header title="Inventory" action={
        <input value={search} onChange={e => setSearch(e.target.value)} placeholder="Search items…"
          style={{ padding: "7px 12px", borderRadius: 7, border: "1px solid var(--border2)", background: "var(--bg)", color: "var(--text)", fontSize: 13, outline: "none", width: 200 }} />
      } />

      {error && <div style={{ marginBottom: 16 }}><ErrBox msg={error} onRetry={reload} /></div>}

      {/* Low stock alert */}
      {!loading && lowStock.length > 0 && (
        <div style={{ background: "#f59e0b18", border: "1px solid #f59e0b40", borderRadius: 10, padding: "12px 16px", marginBottom: 20, display: "flex", alignItems: "center", gap: 10 }}>
          <span style={{ fontSize: 18 }}>⚠️</span>
          <span style={{ color: "var(--orange)", fontSize: 13 }}>{lowStock.length} item{lowStock.length > 1 ? "s are" : " is"} running low on stock: {lowStock.map(i => i.name).join(", ")}</span>
        </div>
      )}

      <div style={{ background: "var(--card)", border: "1px solid var(--border)", borderRadius: 12, overflow: "hidden" }}>
        <Table headers={["Recipe ID", "Name", "Quantity", "Threshold", "Status", "Actions"]} loading={loading}>
          {filtered.map(i => {
            const isLow = i.quantity <= i.threshold;
            return (
              <TR key={i.id}>
                <TD mono>{i.recipeId}</TD>
                <TD><span style={{ fontWeight: 500 }}>{i.name}</span></TD>
                <TD>
                  <span style={{ fontWeight: 600, color: isLow ? "var(--red)" : i.quantity > i.threshold * 3 ? "var(--green)" : "var(--text)" }}>
                    {i.quantity}
                  </span>
                </TD>
                <TD mono>{i.threshold}</TD>
                <TD>
                  <Badge color={!i.inStock ? "var(--red)" : isLow ? "var(--orange)" : "var(--green)"}>
                    {!i.inStock ? "Out of Stock" : isLow ? "Low Stock" : "In Stock"}
                  </Badge>
                </TD>
                <TD><Btn size="sm" variant="ghost" onClick={() => openEdit(i)}>Update Stock</Btn></TD>
              </TR>
            );
          })}
        </Table>
        {!loading && filtered.length === 0 && <div style={{ padding: 40, textAlign: "center", color: "var(--muted)" }}>No inventory records found.</div>}
      </div>

      {editing && (
        <Modal title={`Update Stock — ${editing.name}`} onClose={() => setEditing(null)} width={380}>
          <p style={{ fontSize: 13, color: "var(--muted)", marginBottom: 18 }}>Current quantity: <strong style={{ color: "var(--text)" }}>{editing.quantity}</strong> · Low stock threshold: <strong style={{ color: "var(--text)" }}>{editing.threshold}</strong></p>
          <Inp label="New Quantity" type="number" value={qty} onChange={e => setQty(e.target.value)} placeholder="Enter new stock quantity" />
          <div style={{ display: "flex", gap: 8, justifyContent: "flex-end", paddingTop: 10, borderTop: "1px solid var(--border)" }}>
            <Btn variant="ghost" onClick={() => setEditing(null)}>Cancel</Btn>
            <Btn onClick={save} disabled={saving}>{saving ? <><Spin size={13} color="#fff" /> Saving…</> : "Update Stock"}</Btn>
          </div>
        </Modal>
      )}
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// USERS  — view + role management
// ─────────────────────────────────────────────────────────────────────────────
function Users() {
  const { token } = useAuth();
  const { show }  = useToast();
  const { data, loading, error, reload } = useData("/users?size=100", token);
  const [search,  setSearch]  = useState("");
  const [filter,  setFilter]  = useState("ALL");
  const [updating, setUpdating] = useState(null);
  const [confirm, setConfirm] = useState(null);

  const all   = (data?.content ?? data ?? []).map(nu);
  const users = all
    .filter(u => filter === "ALL" || u.role === filter)
    .filter(u => !search || u.name.toLowerCase().includes(search.toLowerCase()) || u.email.toLowerCase().includes(search.toLowerCase()));

  const updateRole = async (id, role) => {
    setUpdating(id);
    try {
      await PUT(`/users/${id}/role`, { role }, token);
      show(`Role updated to ${role}`);
      reload();
    } catch (e) { show(e.message, "error"); }
    finally { setUpdating(null); }
  };

  const deactivate = async (id) => {
    try {
      await PUT(`/users/${id}/status`, { active: false }, token);
      show("User deactivated.");
      reload();
    } catch (e) { show(e.message, "error"); }
    finally { setConfirm(null); }
  };

  return (
    <div>
      <Header title="Users" action={
        <div style={{ display: "flex", gap: 8 }}>
          <input value={search} onChange={e => setSearch(e.target.value)} placeholder="Search name or email…"
            style={{ padding: "7px 12px", borderRadius: 7, border: "1px solid var(--border2)", background: "var(--bg)", color: "var(--text)", fontSize: 13, outline: "none", width: 210 }} />
          <select value={filter} onChange={e => setFilter(e.target.value)}
            style={{ padding: "7px 12px", borderRadius: 7, border: "1px solid var(--border2)", background: "var(--bg)", color: "var(--text)", fontSize: 13, cursor: "pointer" }}>
            <option value="ALL">All roles</option>
            <option value="USER">USER</option>
            <option value="ADMIN">ADMIN</option>
          </select>
        </div>
      } />

      {error && <div style={{ marginBottom: 16 }}><ErrBox msg={error} onRetry={reload} /></div>}

      <div style={{ background: "var(--card)", border: "1px solid var(--border)", borderRadius: 12, overflow: "hidden" }}>
        <Table headers={["ID", "Name", "Email", "Role", "Status", "Joined", "Actions"]} loading={loading}>
          {users.map(u => (
            <TR key={u.id}>
              <TD mono>{u.id}</TD>
              <TD><span style={{ fontWeight: 500 }}>{u.name}</span></TD>
              <TD mono>{u.email}</TD>
              <TD>
                <select value={u.role} onClick={e => e.stopPropagation()}
                  onChange={e => updateRole(u.id, e.target.value)}
                  disabled={updating === u.id}
                  style={{ padding: "4px 8px", borderRadius: 6, border: "1px solid var(--border2)", background: "var(--bg)", color: u.role === "ADMIN" ? "var(--accent)" : "var(--text)", fontSize: 12, cursor: "pointer", fontWeight: u.role === "ADMIN" ? 600 : 400 }}>
                  <option value="USER">USER</option>
                  <option value="ADMIN">ADMIN</option>
                </select>
              </TD>
              <TD><Badge color={u.active ? "var(--green)" : "var(--muted)"}>{u.active ? "Active" : "Inactive"}</Badge></TD>
              <TD mono>{u.joined ? new Date(u.joined).toLocaleDateString("en-IN", { day: "numeric", month: "short", year: "2-digit" }) : "–"}</TD>
              <TD>
                {u.active && (
                  <Btn size="sm" variant="danger" onClick={() => setConfirm(u.id)}>Deactivate</Btn>
                )}
              </TD>
            </TR>
          ))}
        </Table>
        {!loading && users.length === 0 && <div style={{ padding: 40, textAlign: "center", color: "var(--muted)" }}>No users found.</div>}
      </div>

      {confirm && <Confirm msg="Deactivate this user? They will lose access." onConfirm={() => deactivate(confirm)} onCancel={() => setConfirm(null)} />}
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// LOGIN PAGE  — admin-specific, expects ADMIN role in JWT
// ─────────────────────────────────────────────────────────────────────────────
function Login() {
  const { login } = useAuth();
  const { show }  = useToast();
  const [email, setEmail] = useState("");
  const [pass,  setPass]  = useState("");
  const [loading, setL]   = useState(false);
  const [err, setErr]     = useState(null);

  const handle = async () => {
    if (!email || !pass) { setErr("Enter your admin credentials."); return; }
    setL(true); setErr(null);
    try {
      const data = await POST("/users/login", { email, password: pass });
      const tok  = data.token ?? data.accessToken ?? data.jwt;
      if (!tok) throw new ApiErr("No token received from server.", 0);
      const user = data.user ?? { id: data.id ?? data.userId, name: data.name, email: data.email ?? email, role: data.role ?? "ADMIN" };
      if ((user.role ?? "").toUpperCase() !== "ADMIN") throw new ApiErr("Access denied. This account does not have admin privileges.", 403);
      login(user, tok);
      show("Welcome, " + (user.name?.split(" ")[0] || "Admin") + " 👋");
    } catch (e) {
      setErr(e.status === 401 || e.status === 403 ? (e.status === 403 ? e.message : "Incorrect credentials.") : e.message);
    } finally { setL(false); }
  };

  return (
    <div style={{ minHeight: "100vh", display: "flex", alignItems: "center", justifyContent: "center", background: "var(--bg)", padding: 24 }}>
      <div style={{ width: "100%", maxWidth: 380 }}>
        <div style={{ textAlign: "center", marginBottom: 36 }}>
          <div style={{ fontSize: 40, marginBottom: 12 }}>🌿</div>
          <h1 style={{ fontFamily: "'Playfair Display', serif", fontSize: 26, fontWeight: 700, color: "var(--text)", marginBottom: 6 }}>RecipeEcom Admin</h1>
          <p style={{ color: "var(--muted)", fontSize: 14 }}>Sign in with your admin account</p>
        </div>
        <div style={{ background: "var(--card)", border: "1px solid var(--border)", borderRadius: 14, padding: "30px 28px" }}>
          {err && <div style={{ background: "#c0392b18", border: "1px solid #c0392b40", borderRadius: 8, padding: "10px 13px", fontSize: 13, color: "var(--red)", marginBottom: 16 }}>{err}</div>}
          <Inp label="Email" type="email" value={email} onChange={e => setEmail(e.target.value)} placeholder="admin@recipeecom.com" />
          <Inp label="Password" type="password" value={pass} onChange={e => setPass(e.target.value)} placeholder="••••••••" />
          <Btn size="lg" full onClick={handle} disabled={loading} style={{ marginTop: 6 }}>
            {loading ? <><Spin size={14} color="#fff" /> Signing in…</> : "Sign in to Admin"}
          </Btn>
        </div>
        <p style={{ textAlign: "center", marginTop: 20, fontSize: 12, color: "var(--muted)" }}>Admin access only · <a href="/" style={{ color: "var(--accent)" }}>Go to store →</a></p>
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// ROOT
// ─────────────────────────────────────────────────────────────────────────────
function AdminApp() {
  const { user } = useAuth();
  const [active, setActive] = useState("dashboard");

  if (!user) return <Login />;

  const pages = { dashboard: <Dashboard />, recipes: <Recipes />, orders: <Orders />, inventory: <Inventory />, users: <Users /> };

  return (
    <div style={{ display: "flex", height: "100vh", overflow: "hidden" }}>
      <Sidebar active={active} setActive={setActive} />
      <main style={{ flex: 1, overflowY: "auto", padding: "28px 32px", background: "var(--bg)" }}>
        {pages[active] ?? <Dashboard />}
      </main>
    </div>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <ToastProvider>
        <GS />
        <AdminApp />
      </ToastProvider>
    </AuthProvider>
  );
}
