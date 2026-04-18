import { useParams, useNavigate } from "react-router-dom";

function RecipeDetail() {
  const { id } = useParams();
  const navigate = useNavigate();

  return (
    <div>
      <button onClick={() => navigate("/recipes")}>
        ← Back
      </button>
      <h2>Recipe {id}</h2>
    </div>
  );
}

export default RecipeDetail;