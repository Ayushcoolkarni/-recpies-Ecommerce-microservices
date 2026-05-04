import React, { useState, useRef } from "react";
import { useNavigate, Link } from "react-router-dom";
import { api } from "./api";

/**
 * OTP login flow:
 *   Step 1 — user enters email → POST /auth/otp/send
 *   Step 2 — user enters 6-digit code → POST /auth/otp/verify → JWT stored
 *
 * Add to your router in App.jsx:
 *   <Route path="/login/otp" element={<OtpLogin />} />
 */
export default function OtpLogin() {
  const navigate = useNavigate();
  const [step,    setStep]    = useState("email");   // "email" | "otp"
  const [email,   setEmail]   = useState("");
  const [otp,     setOtp]     = useState(["", "", "", "", "", ""]);
  const [loading, setLoading] = useState(false);
  const [toast,   setToast]   = useState(null);
  const [resendCountdown, setResendCountdown] = useState(0);
  const inputRefs = useRef([]);

  const showToast = (msg, type = "success") => {
    setToast({ msg, type });
    setTimeout(() => setToast(null), 3000);
  };

  // ── Step 1: send OTP ───────────────────────────────────────────────────────
  const handleSendOtp = async () => {
    if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      showToast("Enter a valid email address", "error");
      return;
    }
    setLoading(true);
    try {
      await api.sendOtp({ email });
      setStep("otp");
      showToast("OTP sent! Check your inbox 📧");
      startResendCountdown();
    } catch (e) {
      showToast(e.message || "Failed to send OTP", "error");
    } finally {
      setLoading(false);
    }
  };

  // ── Step 2: verify OTP ─────────────────────────────────────────────────────
  const handleVerifyOtp = async () => {
    const code = otp.join("");
    if (code.length !== 6) { showToast("Enter the full 6-digit OTP", "error"); return; }
    setLoading(true);
    try {
      const res = await api.verifyOtp({ email, otp: code });
      if (!res?.accessToken) { showToast("Invalid or expired OTP", "error"); return; }
      localStorage.setItem("rce_t", res.accessToken);
      localStorage.setItem("rce_r", res.refreshToken);
      localStorage.setItem("rce_u", JSON.stringify({
        userId: res.userId,
        email:  res.email,
        name:   res.name,
        role:   res.role,
      }));
      showToast("Logged in! 🎉");
      setTimeout(() => navigate("/recipes"), 800);
    } catch (e) {
      showToast(e.message || "OTP verification failed", "error");
    } finally {
      setLoading(false);
    }
  };

  // ── OTP digit input handlers ───────────────────────────────────────────────
  const handleOtpChange = (value, idx) => {
    if (!/^\d?$/.test(value)) return;             // digits only
    const next = [...otp];
    next[idx] = value;
    setOtp(next);
    if (value && idx < 5) inputRefs.current[idx + 1]?.focus();
    if (next.every(d => d !== "") && next.join("").length === 6) {
      // Auto-submit when all 6 digits filled
      setTimeout(() => handleVerifyOtpWithCode(next.join("")), 100);
    }
  };

  const handleOtpKeyDown = (e, idx) => {
    if (e.key === "Backspace" && !otp[idx] && idx > 0) {
      inputRefs.current[idx - 1]?.focus();
    }
  };

  const handleVerifyOtpWithCode = async (code) => {
    if (code.length !== 6) return;
    setLoading(true);
    try {
      const res = await api.verifyOtp({ email, otp: code });
      if (!res?.accessToken) { showToast("Invalid or expired OTP", "error"); return; }
      localStorage.setItem("rce_t", res.accessToken);
      localStorage.setItem("rce_r", res.refreshToken);
      localStorage.setItem("rce_u", JSON.stringify({
        userId: res.userId, email: res.email, name: res.name, role: res.role,
      }));
      showToast("Logged in! 🎉");
      setTimeout(() => navigate("/recipes"), 800);
    } catch (e) {
      showToast(e.message || "OTP verification failed", "error");
    } finally {
      setLoading(false);
    }
  };

  // ── Resend countdown ───────────────────────────────────────────────────────
  const startResendCountdown = () => {
    setResendCountdown(30);
    const interval = setInterval(() => {
      setResendCountdown(c => {
        if (c <= 1) { clearInterval(interval); return 0; }
        return c - 1;
      });
    }, 1000);
  };

  const handleResend = async () => {
    if (resendCountdown > 0) return;
    setLoading(true);
    try {
      await api.sendOtp({ email });
      setOtp(["", "", "", "", "", ""]);
      inputRefs.current[0]?.focus();
      showToast("OTP resent! Check your inbox 📧");
      startResendCountdown();
    } catch (e) {
      showToast(e.message || "Failed to resend OTP", "error");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ minHeight: "100vh", background: "#FAFAF8", display: "flex", alignItems: "center", justifyContent: "center", padding: 24 }}>
      <style>{`
        @import url('https://fonts.googleapis.com/css2?family=Syne:wght@400;600;700;800&family=DM+Sans:wght@300;400;500&display=swap');
        @keyframes fadeUp{from{opacity:0;transform:translateY(20px)}to{opacity:1;transform:translateY(0)}}
        @keyframes slideIn{from{opacity:0;transform:translateY(10px)}to{opacity:1;transform:translateY(0)}}
        .otp-input:focus { border-color: #E23744 !important; box-shadow: 0 0 0 3px rgba(226,55,68,0.15) !important; }
        .otp-input { caret-color: #E23744; }
      `}</style>

      {toast && (
        <div style={{ position: "fixed", bottom: 24, right: 24, zIndex: 9999, background: toast.type === "error" ? "#E23744" : "#1DB954", color: "#fff", padding: "12px 22px", borderRadius: 12, fontFamily: "'Syne',sans-serif", fontWeight: 600, fontSize: 14, boxShadow: "0 8px 32px rgba(0,0,0,0.2)", animation: "slideIn 0.3s ease" }}>
          {toast.msg}
        </div>
      )}

      <div style={{ width: "100%", maxWidth: 420, animation: "fadeUp 0.45s ease" }}>
        {/* Logo */}
        <div style={{ textAlign: "center", marginBottom: 32 }}>
          <div style={{ fontSize: 48, marginBottom: 8 }}>🍳</div>
          <h1 style={{ fontFamily: "'Syne',sans-serif", fontWeight: 800, fontSize: 28, color: "#1A1A1A", margin: 0 }}>RasoiKit</h1>
          <p style={{ fontFamily: "'DM Sans',sans-serif", color: "#999", fontSize: 14, marginTop: 4 }}>Fresh ingredients, perfectly measured</p>
        </div>

        <div style={{ background: "#fff", borderRadius: 20, padding: "32px 28px", boxShadow: "0 8px 40px rgba(0,0,0,0.08)" }}>

          {/* ── Step 1: Email input ── */}
          {step === "email" && (
            <>
              <h2 style={{ fontFamily: "'Syne',sans-serif", fontWeight: 800, fontSize: 22, color: "#1A1A1A", marginBottom: 6 }}>Login with OTP</h2>
              <p style={{ fontFamily: "'DM Sans',sans-serif", color: "#888", fontSize: 14, marginBottom: 28 }}>We'll send a 6-digit code to your email. No password needed.</p>

              <label style={{ fontFamily: "'Syne',sans-serif", fontWeight: 700, fontSize: 12, color: "#555", letterSpacing: 0.5, display: "block", marginBottom: 6 }}>EMAIL ADDRESS</label>
              <input
                type="email"
                value={email}
                onChange={e => setEmail(e.target.value)}
                onKeyDown={e => e.key === "Enter" && handleSendOtp()}
                placeholder="you@example.com"
                style={{ width: "100%", border: "1.5px solid #E0E0E0", borderRadius: 12, padding: "14px 16px", fontFamily: "'DM Sans',sans-serif", fontSize: 15, outline: "none", boxSizing: "border-box", transition: "border 0.2s", marginBottom: 20 }}
                onFocus={e => e.target.style.borderColor = "#E23744"}
                onBlur={e  => e.target.style.borderColor = "#E0E0E0"}
              />

              <button
                onClick={handleSendOtp}
                disabled={loading}
                style={{ width: "100%", background: loading ? "#ccc" : "linear-gradient(135deg,#E23744,#FF6B35)", border: "none", borderRadius: 13, padding: "15px", color: "#fff", fontFamily: "'Syne',sans-serif", fontWeight: 800, fontSize: 16, cursor: loading ? "not-allowed" : "pointer", boxShadow: loading ? "none" : "0 8px 24px rgba(226,55,68,0.3)", transition: "all 0.2s", marginBottom: 20 }}>
                {loading ? "Sending…" : "Send OTP →"}
              </button>

              <p style={{ fontFamily: "'DM Sans',sans-serif", fontSize: 13, color: "#aaa", textAlign: "center" }}>
                Prefer password?{" "}
                <Link to="/login" style={{ color: "#E23744", fontWeight: 600, textDecoration: "none" }}>Login here</Link>
              </p>
            </>
          )}

          {/* ── Step 2: OTP input ── */}
          {step === "otp" && (
            <>
              <button onClick={() => { setStep("email"); setOtp(["","","","","",""]); }}
                style={{ background: "none", border: "none", cursor: "pointer", fontFamily: "'DM Sans',sans-serif", fontSize: 13, color: "#aaa", padding: 0, marginBottom: 20 }}>
                ← Back
              </button>

              <h2 style={{ fontFamily: "'Syne',sans-serif", fontWeight: 800, fontSize: 22, color: "#1A1A1A", marginBottom: 6 }}>Enter OTP</h2>
              <p style={{ fontFamily: "'DM Sans',sans-serif", color: "#888", fontSize: 14, marginBottom: 28 }}>
                Sent to <strong style={{ color: "#1A1A1A" }}>{email}</strong>. Expires in 5 minutes.
              </p>

              {/* 6-digit OTP boxes */}
              <div style={{ display: "flex", gap: 10, justifyContent: "center", marginBottom: 28 }}>
                {otp.map((digit, idx) => (
                  <input
                    key={idx}
                    ref={el => inputRefs.current[idx] = el}
                    className="otp-input"
                    type="text"
                    inputMode="numeric"
                    maxLength={1}
                    value={digit}
                    onChange={e => handleOtpChange(e.target.value, idx)}
                    onKeyDown={e => handleOtpKeyDown(e, idx)}
                    style={{
                      width: 48, height: 56, textAlign: "center", border: "1.5px solid #E0E0E0",
                      borderRadius: 12, fontFamily: "'Syne',sans-serif", fontWeight: 800, fontSize: 22,
                      outline: "none", transition: "border 0.2s, box-shadow 0.2s",
                      background: digit ? "#FFF0F1" : "#fff", color: "#E23744",
                    }}
                  />
                ))}
              </div>

              <button
                onClick={handleVerifyOtp}
                disabled={loading || otp.join("").length < 6}
                style={{ width: "100%", background: (loading || otp.join("").length < 6) ? "#ccc" : "linear-gradient(135deg,#E23744,#FF6B35)", border: "none", borderRadius: 13, padding: "15px", color: "#fff", fontFamily: "'Syne',sans-serif", fontWeight: 800, fontSize: 16, cursor: (loading || otp.join("").length < 6) ? "not-allowed" : "pointer", boxShadow: loading ? "none" : "0 8px 24px rgba(226,55,68,0.3)", transition: "all 0.2s", marginBottom: 16 }}>
                {loading ? "Verifying…" : "Verify OTP ✓"}
              </button>

              <p style={{ fontFamily: "'DM Sans',sans-serif", fontSize: 13, color: "#aaa", textAlign: "center" }}>
                Didn't receive it?{" "}
                <button onClick={handleResend} disabled={resendCountdown > 0}
                  style={{ background: "none", border: "none", cursor: resendCountdown > 0 ? "default" : "pointer", fontFamily: "'DM Sans',sans-serif", fontSize: 13, color: resendCountdown > 0 ? "#ccc" : "#E23744", fontWeight: 600, padding: 0 }}>
                  {resendCountdown > 0 ? `Resend in ${resendCountdown}s` : "Resend OTP"}
                </button>
              </p>
            </>
          )}
        </div>

        <p style={{ fontFamily: "'DM Sans',sans-serif", fontSize: 12, color: "#ccc", textAlign: "center", marginTop: 20 }}>
          New here? OTP login auto-creates your account.
        </p>
      </div>
    </div>
  );
}
