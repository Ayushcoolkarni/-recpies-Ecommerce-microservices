import React from "react";
import { useNavigate } from "react-router-dom";

function Register() {
  const navigate = useNavigate();

  const handleRegister = () => {
    // fake register
    localStorage.setItem("user", JSON.stringify({ name: "Ayush" }));

    navigate("/orders");
  };

  return (
    <div>
      <h2>Register Page</h2>

      <button onClick={handleRegister}>
        Register
      </button>

      <button onClick={() => navigate("/login")}>
        Go to Login
      </button>
    </div>
  );
}

export default Register;