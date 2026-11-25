package com.cookify.rating;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RatingService {

    private final RecipeRatingRepository ratings;

    public RatingService(RecipeRatingRepository ratings) {
        this.ratings = ratings;
    }

    @Transactional
    public void rate(Long recipeId, Long userId, int stars) {
        if (stars < 1 || stars > 5) {
            throw new IllegalArgumentException("stars must be between 1 and 5, got " + stars);
        }
        RecipeRating rating = ratings.findByRecipeIdAndUserId(recipeId, userId)
            .orElseGet(() -> {
                RecipeRating r = new RecipeRating();
                r.setRecipeId(recipeId);
                r.setUserId(userId);
                return r;
            });
        rating.setRate1(stars == 1 ? 1 : 0);
        rating.setRate2(stars == 2 ? 1 : 0);
        rating.setRate3(stars == 3 ? 1 : 0);
        rating.setRate4(stars == 4 ? 1 : 0);
        rating.setRate5(stars == 5 ? 1 : 0);
        ratings.save(rating);
    }

    @Transactional(readOnly = true)
    public RatingSummary summary(Long recipeId) {
        return summarize(ratings.findByRecipeId(recipeId));
    }

    /**
     * Batch equivalent of {@link #summary(Long)}: fetches every rating row for the given
     * recipes in a single query, then groups them in memory, so callers rendering a list of
     * recipes (Explore, search) don't issue one rating query per recipe.
     */
    @Transactional(readOnly = true)
    public Map<Long, RatingSummary> summaries(Collection<Long> recipeIds) {
        Map<Long, List<RecipeRating>> byRecipe = new HashMap<>();
        for (RecipeRating r : ratings.findByRecipeIdIn(recipeIds)) {
            byRecipe.computeIfAbsent(r.getRecipeId(), id -> new ArrayList<>()).add(r);
        }
        Map<Long, RatingSummary> result = new HashMap<>();
        for (Long recipeId : recipeIds) {
            result.put(recipeId, summarize(byRecipe.getOrDefault(recipeId, List.of())));
        }
        return result;
    }

    private RatingSummary summarize(List<RecipeRating> rows) {
        long weightedSum = 0;
        int count = 0;
        for (RecipeRating r : rows) {
            int star = starOf(r);
            if (star > 0) {
                weightedSum += star;
                count++;
            }
        }
        if (count == 0) {
            return new RatingSummary(0.0, 0);
        }
        double average = BigDecimal.valueOf((double) weightedSum / count)
            .setScale(1, RoundingMode.HALF_UP)
            .doubleValue();
        return new RatingSummary(average, count);
    }

    private int starOf(RecipeRating r) {
        if (r.getRate1() == 1) return 1;
        if (r.getRate2() == 1) return 2;
        if (r.getRate3() == 1) return 3;
        if (r.getRate4() == 1) return 4;
        if (r.getRate5() == 1) return 5;
        return 0;
    }
}
