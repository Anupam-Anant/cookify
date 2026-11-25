package com.cookify.recipe;

import com.cookify.rating.RatingService;
import com.cookify.rating.RatingSummary;
import com.cookify.search.RecipeFilter;
import com.cookify.search.RecipeSearchService;
import com.cookify.user.User;
import com.cookify.user.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;
import java.util.Map;

@Controller
public class ExploreController {

    private final RecipeSearchService searchService;
    private final RatingService ratingService;
    private final UserRepository users;

    public ExploreController(RecipeSearchService searchService, RatingService ratingService, UserRepository users) {
        this.searchService = searchService;
        this.ratingService = ratingService;
        this.users = users;
    }

    @GetMapping("/explore")
    public String explore(@ModelAttribute("filter") RecipeFilter filter, Model model) {
        List<Recipe> recipes = searchService.search(filter);
        Map<Long, RatingSummary> ratingSummaries = ratingService.summaries(recipes.stream().map(Recipe::getId).toList());
        List<RecipeCard> cards = recipes.stream()
            .map(r -> toCard(r, ratingSummaries))
            .toList();
        model.addAttribute("recipes", cards);
        model.addAttribute("filter", filter);
        return "explore";
    }

    private RecipeCard toCard(Recipe recipe, Map<Long, RatingSummary> ratingSummaries) {
        String creatorName = users.findById(recipe.getRecipeCreatorId())
            .map(User::getUsername)
            .orElse("Unknown Chef");
        RatingSummary ratingSummary = ratingSummaries.getOrDefault(recipe.getId(), new RatingSummary(0.0, 0));
        return new RecipeCard(recipe, creatorName, ratingSummary);
    }

    /** Simple view-model pairing a Recipe with its creator's display name and rating summary. */
    public static final class RecipeCard {
        private final Recipe recipe;
        private final String creatorName;
        private final RatingSummary ratingSummary;

        public RecipeCard(Recipe recipe, String creatorName, RatingSummary ratingSummary) {
            this.recipe = recipe;
            this.creatorName = creatorName;
            this.ratingSummary = ratingSummary;
        }

        public Recipe getRecipe() { return recipe; }
        public String getCreatorName() { return creatorName; }
        public RatingSummary getRatingSummary() { return ratingSummary; }
    }
}
