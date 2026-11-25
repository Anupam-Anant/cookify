package com.cookify.config;

import com.cookify.category.RecipeCategory;
import com.cookify.category.RecipeCategoryRepository;
import com.cookify.rating.RecipeRatingRepository;
import com.cookify.recipe.Recipe;
import com.cookify.recipe.RecipeRepository;
import com.cookify.subscription.SubscriptionRepository;
import com.cookify.user.User;
import com.cookify.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises {@link DevDataSeeder} directly (constructed with the real, autowired JPA
 * repositories under {@code @DataJpaTest}, bypassing the {@code dev}-profile component scan,
 * same pattern as the other {@code @DataJpaTest} classes in this suite) rather than booting
 * the whole app with the dev profile active.
 */
@DataJpaTest
class DevDataSeederTest {

    @Autowired UserRepository users;
    @Autowired RecipeRepository recipes;
    @Autowired RecipeCategoryRepository categories;
    @Autowired RecipeRatingRepository ratings;
    @Autowired SubscriptionRepository subscriptions;

    DevDataSeeder seeder;
    Path uploadDir;

    @BeforeEach
    void setUp() {
        uploadDir = Path.of("build", "test-seed-uploads");
        seeder = new DevDataSeeder(users, recipes, categories, ratings, subscriptions, new BCryptPasswordEncoder(),
            uploadDir.toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        if (Files.exists(uploadDir)) {
            try (var paths = Files.walk(uploadDir)) {
                paths.sorted(Comparator.reverseOrder()).forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException ignored) {
                        // best-effort cleanup
                    }
                });
            }
        }
    }

    @Test
    void seedsUsersAndSampleRecipesOnEmptyDatabase() throws Exception {
        seeder.run();

        assertThat(users.count()).isGreaterThanOrEqualTo(2);
        User aryan = users.findByUsername("aryan").orElseThrow();
        assertThat(aryan.isTwoFactorEnabled()).isFalse();
        assertThat(aryan.getAccountStatus().name()).isEqualTo("ACTIVE");
        assertThat(new BCryptPasswordEncoder().matches(DevDataSeeder.DEMO_PASSWORD, aryan.getPasswordHash())).isTrue();

        List<String> names = recipes.findAll().stream().map(Recipe::getRecipeName).toList();
        assertThat(names).containsExactlyInAnyOrder(
            "Smash Burger Tacos", "Chicken Biryani", "Idli dosa", "Chicken Curry");

        Recipe tacos = recipes.findAll().stream()
            .filter(r -> r.getRecipeName().equals("Smash Burger Tacos")).findFirst().orElseThrow();
        assertThat(tacos.dietaryLabel()).isEqualTo("Non-Veg");
        assertThat(tacos.getIngredient()).isNotBlank();
        assertThat(tacos.getRecipeMethod()).isNotBlank();
        assertThat(tacos.getCost()).isNotNull();
        assertThat(tacos.getSpeed()).isNotNull();
        assertThat(tacos.getDifficulty()).isNotNull();

        Recipe idliDosa = recipes.findAll().stream()
            .filter(r -> r.getRecipeName().equals("Idli dosa")).findFirst().orElseThrow();
        assertThat(idliDosa.dietaryLabel()).isEqualTo("Veg");

        // Every seeded recipe has a matching category row (veg flag, cooking time, calories, protein).
        for (Recipe r : recipes.findAll()) {
            RecipeCategory category = categories.findById(r.getId()).orElseThrow();
            assertThat(category.isVegNonveg()).isEqualTo(r.dietaryLabel().equals("Veg"));
            assertThat(category.getCookingTime()).isNotNull();
            assertThat(category.getCalory()).isNotNull();
            assertThat(category.getProtin()).isNotNull();
        }

        // A few ratings exist so Explore cards show "N ratings | Avg X".
        assertThat(ratings.count()).isGreaterThan(0);

        // One subscription exists.
        assertThat(subscriptions.count()).isEqualTo(1);
    }

    @Test
    void seedsRealPhotosForTheFourNamedRecipesSoExploreCardsRenderImages() throws Exception {
        seeder.run();

        Map<String, String> expectedImageByRecipeName = Map.of(
            "Chicken Biryani", "biryani.jpg",
            "Smash Burger Tacos", "tacos.jpg",
            "Idli dosa", "dosa.jpg",
            "Chicken Curry", "chicken-curry.jpg");

        for (Recipe r : recipes.findAll()) {
            String expectedFileName = expectedImageByRecipeName.get(r.getRecipeName());
            assertThat(expectedFileName).as("no expected image mapped for %s", r.getRecipeName()).isNotNull();
            assertThat(r.getImagePath()).isEqualTo(expectedFileName);
            assertThat(Files.exists(uploadDir.resolve(r.getImagePath())))
                .as("seeded image file missing for %s", r.getRecipeName())
                .isTrue();
        }
    }

    @Test
    void runTwiceDoesNotDuplicateSeedData() throws Exception {
        seeder.run();
        long userCountAfterFirst = users.count();
        long recipeCountAfterFirst = recipes.count();
        long ratingCountAfterFirst = ratings.count();
        long subscriptionCountAfterFirst = subscriptions.count();

        seeder.run();

        assertThat(users.count()).isEqualTo(userCountAfterFirst);
        assertThat(recipes.count()).isEqualTo(recipeCountAfterFirst);
        assertThat(ratings.count()).isEqualTo(ratingCountAfterFirst);
        assertThat(subscriptions.count()).isEqualTo(subscriptionCountAfterFirst);
    }

    @Test
    void doesNotSeedWhenAUserAlreadyExists() throws Exception {
        User existing = new User();
        existing.setUsername("realuser");
        existing.setEmail("realuser@example.com");
        existing.setPhone("+919000000000");
        existing.setPasswordHash("$2a$dummy");
        users.save(existing);

        seeder.run();

        assertThat(users.count()).isEqualTo(1);
        assertThat(recipes.count()).isZero();
    }
}
