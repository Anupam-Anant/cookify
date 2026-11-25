package com.cookify.search;

import com.cookify.category.RecipeCategory;
import com.cookify.recipe.NonVegRecipe;
import com.cookify.recipe.Recipe;
import com.cookify.recipe.VegRecipe;
import com.cookify.user.User;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Null-safe {@link Specification} builders for {@link Recipe}, one per {@link RecipeFilter}
 * dimension, composed by {@link #build(RecipeFilter)}. Veg/non-veg uses the {@code recipe_type}
 * single-table discriminator (via {@code Root#type()}); cookingTime/calories live on the
 * separate {@code recipe_category} table (no JPA association on {@code Recipe}, so a subquery
 * on {@code recipe_id} is used instead of a join).
 */
public final class RecipeSpecifications {

    private RecipeSpecifications() {}

    public static Specification<Recipe> build(RecipeFilter filter) {
        List<Specification<Recipe>> specs = new ArrayList<>();
        addIfPresent(specs, matchesQuery(filter));
        addIfPresent(specs, isVeg(filter.getVeg()));
        addIfPresent(specs, maxCost(filter.getMaxCost()));
        addIfPresent(specs, maxCookingTime(filter.getMaxCookingTime()));
        addIfPresent(specs, minSpeed(filter.getMinSpeed()));
        addIfPresent(specs, minDifficulty(filter.getMinDifficulty()));
        addIfPresent(specs, maxCalories(filter.getMaxCalories()));
        addIfPresent(specs, ingredientLike(filter.getIngredient()));

        if (specs.isEmpty()) {
            return (root, cq, cb) -> cb.conjunction();
        }
        Specification<Recipe> result = specs.get(0);
        for (int i = 1; i < specs.size(); i++) {
            result = result.and(specs.get(i));
        }
        return result;
    }

    private static void addIfPresent(List<Specification<Recipe>> specs, Specification<Recipe> spec) {
        if (spec != null) {
            specs.add(spec);
        }
    }

    /**
     * Scoped by the Explore page's "search in" checkboxes ({@link RecipeFilter#matchTitle()} /
     * {@code matchCreator()} / {@code matchType()}): each contributes its predicate to the OR
     * only when its checkbox is in scope. When no checkbox was submitted at all (first visit, or
     * every box unchecked), all three are in scope — same behavior as before the checkboxes
     * existed.
     */
    static Specification<Recipe> matchesQuery(RecipeFilter filter) {
        String query = filter.getQuery();
        if (query == null || query.isBlank()) {
            return null;
        }
        boolean inTitle = filter.matchTitle();
        boolean inCreator = filter.matchCreator();
        boolean inType = filter.matchType();
        String like = "%" + query.trim().toLowerCase() + "%";
        String normalized = query.trim().toLowerCase().replace("-", "").replace(" ", "");
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (inTitle) {
                predicates.add(cb.like(cb.lower(root.get("recipeName")), like));
            }
            if (inCreator) {
                Subquery<Long> creatorMatch = cq.subquery(Long.class);
                Root<User> userRoot = creatorMatch.from(User.class);
                creatorMatch.select(userRoot.get("id"));
                creatorMatch.where(cb.like(cb.lower(userRoot.get("username")), like));
                predicates.add(root.get("recipeCreatorId").in(creatorMatch));
            }
            if (inType) {
                if ("veg".equals(normalized)) {
                    predicates.add(cb.equal(root.type(), VegRecipe.class));
                }
                if ("nonveg".equals(normalized)) {
                    predicates.add(cb.equal(root.type(), NonVegRecipe.class));
                }
            }
            if (predicates.isEmpty()) {
                return cb.disjunction();
            }
            return cb.or(predicates.toArray(new Predicate[0]));
        };
    }

    static Specification<Recipe> isVeg(Boolean veg) {
        if (veg == null) {
            return null;
        }
        return (root, cq, cb) -> veg
            ? cb.equal(root.type(), VegRecipe.class)
            : cb.equal(root.type(), NonVegRecipe.class);
    }

    static Specification<Recipe> maxCost(Integer maxCost) {
        if (maxCost == null) {
            return null;
        }
        return (root, cq, cb) -> cb.lessThanOrEqualTo(root.get("cost"), maxCost);
    }

    static Specification<Recipe> minSpeed(Integer minSpeed) {
        if (minSpeed == null) {
            return null;
        }
        return (root, cq, cb) -> cb.greaterThanOrEqualTo(root.get("speed"), minSpeed);
    }

    static Specification<Recipe> minDifficulty(Integer minDifficulty) {
        if (minDifficulty == null) {
            return null;
        }
        return (root, cq, cb) -> cb.greaterThanOrEqualTo(root.get("difficulty"), minDifficulty);
    }

    static Specification<Recipe> ingredientLike(String ingredient) {
        if (ingredient == null || ingredient.isBlank()) {
            return null;
        }
        String like = "%" + ingredient.trim().toLowerCase() + "%";
        return (root, cq, cb) -> cb.like(cb.lower(root.get("ingredient")), like);
    }

    static Specification<Recipe> maxCookingTime(Integer maxCookingTime) {
        if (maxCookingTime == null) {
            return null;
        }
        return (root, cq, cb) -> {
            Subquery<Long> sub = cq.subquery(Long.class);
            Root<RecipeCategory> catRoot = sub.from(RecipeCategory.class);
            sub.select(catRoot.get("recipeId"));
            sub.where(cb.lessThanOrEqualTo(catRoot.get("cookingTime"), maxCookingTime));
            return root.get("id").in(sub);
        };
    }

    static Specification<Recipe> maxCalories(Integer maxCalories) {
        if (maxCalories == null) {
            return null;
        }
        return (root, cq, cb) -> {
            Subquery<Long> sub = cq.subquery(Long.class);
            Root<RecipeCategory> catRoot = sub.from(RecipeCategory.class);
            sub.select(catRoot.get("recipeId"));
            sub.where(cb.lessThanOrEqualTo(catRoot.get("calory"), maxCalories));
            return root.get("id").in(sub);
        };
    }
}
