package com.cookify.search;

import com.cookify.rating.RatingService;
import com.cookify.rating.RatingSummary;
import com.cookify.recipe.Recipe;
import com.cookify.recipe.RecipeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Applies {@link RecipeFilter} via {@link RecipeSpecifications}, then orders the resulting
 * candidates per {@code filter.sort}:
 * <ul>
 *   <li>{@code newest} — createdAt descending.</li>
 *   <li>{@code rating} — average rating descending ({@link RatingService#summary}).</li>
 *   <li>{@code popular} (default) — weighted score over views, average rating, and recency,
 *       computed in-service after the candidates are fetched.</li>
 * </ul>
 */
@Service
public class RecipeSearchService {

    private final RecipeRepository recipes;
    private final RatingService ratingService;

    public RecipeSearchService(RecipeRepository recipes, RatingService ratingService) {
        this.recipes = recipes;
        this.ratingService = ratingService;
    }

    @Transactional(readOnly = true)
    public List<Recipe> search(RecipeFilter filter) {
        List<Recipe> candidates = recipes.findAll(RecipeSpecifications.build(filter));
        String sort = filter.getSort() == null ? "popular" : filter.getSort().toLowerCase();

        return switch (sort) {
            case "newest" -> candidates.stream()
                .sorted(Comparator.comparing(Recipe::getCreatedAt).reversed())
                .toList();
            case "rating" -> {
                Map<Long, RatingSummary> summaries = ratingService.summaries(ids(candidates));
                yield candidates.stream()
                    .sorted(Comparator.comparingDouble((Recipe r) -> summaries.get(r.getId()).average())
                        .reversed())
                    .toList();
            }
            default -> {
                Map<Long, RatingSummary> summaries = ratingService.summaries(ids(candidates));
                yield candidates.stream()
                    .sorted(Comparator.comparingDouble((Recipe r) -> popularityScore(r, summaries)).reversed())
                    .toList();
            }
        };
    }

    private List<Long> ids(List<Recipe> candidates) {
        return candidates.stream().map(Recipe::getId).toList();
    }

    private double popularityScore(Recipe recipe, Map<Long, RatingSummary> summaries) {
        RatingSummary summary = summaries.get(recipe.getId());
        double viewsScore = Math.log(recipe.getViews() + 1);
        double ratingScore = summary.average();
        long daysOld = Math.max(0, ChronoUnit.DAYS.between(recipe.getCreatedAt(), LocalDateTime.now()));
        double recencyScore = 1.0 / (1.0 + daysOld);
        return (0.5 * viewsScore) + (0.3 * ratingScore) + (0.2 * recencyScore);
    }
}
