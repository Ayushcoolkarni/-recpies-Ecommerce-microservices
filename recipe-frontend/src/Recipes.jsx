import RCard from "./RCard/RCard";

function Recipes() {
  const recipes = [
    { id: 1, name: "Pizza" },
    { id: 2, name: "Burger" }
  ];

  return (
    <div>
      <h1>Recipes</h1>

      {recipes.map(r => (
        <RCard key={r.id} recipe={r} />
      ))}
    </div>
  );
}

export default Recipes;