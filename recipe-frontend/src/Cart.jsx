import React, { useEffect } from "react";
import { useNavigate } from "react-router-dom";

function Cart() {
  const navigate = useNavigate();

  // get user from localStorage
  const user = JSON.parse(localStorage.getItem("user"));

  // redirect if not logged in
  useEffect(() => {
    if (!user) {
      navigate("/login");
    }
  }, [user, navigate]);

  return (
    <div>
      <h2>Cart Page</h2>

      <button onClick={() => navigate("/orders")}>
        Go to Orders
      </button>

      <button onClick={() => navigate("/recipes")}>
        Go to Recipes
      </button>
    </div>
  );
}

export default Cart;