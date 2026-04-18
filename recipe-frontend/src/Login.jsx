import React, { useState } from "react";
import { useNavigate } from "react-router-dom";

function Login() {
  const navigate = useNavigate();

  const [formData, setFormData] = useState({
    email: "",
    password: "",
  });

  // handle input change
  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    });
  };

  // handle login
  const handleLogin = (e) => {
    e.preventDefault();

    const { email, password } = formData;

    // basic validation
    if (!email || !password) {
      alert("Please fill all fields");
      return;
    }

    // fake authentication (you can replace with API later)
    const user = {
      email,
    };

    localStorage.setItem("user", JSON.stringify(user));

    // redirect after login
    navigate("/orders");
  };

  return (
    <div style={{ padding: "20px" }}>
      <h2>Login</h2>

      <form onSubmit={handleLogin}>
        <div>
          <input
            type="email"
            name="email"
            placeholder="Enter Email"
            value={formData.email}
            onChange={handleChange}
          />
        </div>

        <div style={{ marginTop: "10px" }}>
          <input
            type="password"
            name="password"
            placeholder="Enter Password"
            value={formData.password}
            onChange={handleChange}
          />
        </div>

        <div style={{ marginTop: "15px" }}>
          <button type="submit">Login</button>
        </div>
      </form>

      <div style={{ marginTop: "10px" }}>
        <button onClick={() => navigate("/register")}>
          Go to Register
        </button>
      </div>
    </div>
  );
}

export default Login;