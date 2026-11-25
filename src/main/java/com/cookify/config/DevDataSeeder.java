package com.cookify.config;

import com.cookify.category.RecipeCategory;
import com.cookify.category.RecipeCategoryRepository;
import com.cookify.rating.RecipeRating;
import com.cookify.rating.RecipeRatingRepository;
import com.cookify.recipe.Recipe;
import com.cookify.recipe.RecipeFactory;
import com.cookify.recipe.RecipeRepository;
import com.cookify.subscription.Subscription;
import com.cookify.subscription.SubscriptionRepository;
import com.cookify.user.AccountStatus;
import com.cookify.user.Role;
import com.cookify.user.User;
import com.cookify.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Seeds a small, demo-ready dataset mirroring the COOKify sample screen: a creator user
 * ("aryan") plus a couple more users, four named recipes with ingredients/method/cost/
 * speed/difficulty plus a matching {@link RecipeCategory} (veg flag, cooking time, calories,
 * protein), a handful of ratings so Explore cards show "N ratings | Avg X", and one
 * subscription.
 *
 * <p>Runs ONLY under the {@code dev} profile (never {@code prod}/{@code test}), and only when
 * the database has no users yet ({@link UserRepository#count()} {@code == 0}), so it is
 * idempotent and never duplicates data across restarts or once a real user has signed up.
 *
 * <p>Persists via the repositories directly rather than {@code RecipeService}/
 * {@code SubscriptionService}, which would otherwise fire subscriber-notification emails
 * during seeding.
 */
@Component
@Profile("dev")
public class DevDataSeeder implements CommandLineRunner {

    /** Known password for every seeded demo user (BCrypt-encoded before storage). */
    public static final String DEMO_PASSWORD = "Cookify-Demo123";

    private static final Logger LOG = LoggerFactory.getLogger(DevDataSeeder.class);

    private final UserRepository users;
    private final RecipeRepository recipes;
    private final RecipeCategoryRepository categories;
    private final RecipeRatingRepository ratings;
    private final SubscriptionRepository subscriptions;
    private final PasswordEncoder encoder;
    private final String uploadDir;

    public DevDataSeeder(UserRepository users, RecipeRepository recipes, RecipeCategoryRepository categories,
                          RecipeRatingRepository ratings, SubscriptionRepository subscriptions,
                          PasswordEncoder encoder,
                          @Value("${cookify.storage.upload-dir}") String uploadDir) {
        this.users = users;
        this.recipes = recipes;
        this.categories = categories;
        this.ratings = ratings;
        this.subscriptions = subscriptions;
        this.encoder = encoder;
        this.uploadDir = uploadDir;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (users.count() > 0) {
            return; // already seeded (or a real user signed up) -- never duplicate or clobber.
        }

        User aryan = seedUser("aryan", "aryan@cookify.local", "+910000000001");
        User priya = seedUser("priya", "priya@cookify.local", "+910000000002");
        User rahul = seedUser("rahul", "rahul@cookify.local", "+910000000003");

        Recipe tacos = seedRecipe(false, "Smash Burger Tacos", aryan.getId(),
            "Ground beef, taco shells, cheddar cheese, lettuce, special sauce",
            "Step 1: Form thin beef patties.\n"
                + "Step 2: Smash and sear on a hot griddle until crisp-edged.\n"
                + "Step 3: Assemble in taco shells with cheese, lettuce, and special sauce.",
            250, 4, 2, 20, 550, 35, 120);
        tacos.setImagePath(seedImage("tacos.jpg"));
        recipes.save(tacos);

        Recipe biryani = seedRecipe(false, "Chicken Biryani", aryan.getId(),
            "Basmati rice, chicken, yogurt, biryani masala, fried onions, saffron",
            "Step 1: Marinate chicken in yogurt and spices.\n"
                + "Step 2: Parboil basmati rice with whole spices.\n"
                + "Step 3: Layer chicken and rice, then dum-cook on low heat.",
            300, 2, 4, 60, 650, 30, 300);
        biryani.setImagePath(seedImage("biryani.jpg"));
        recipes.save(biryani);

        Recipe idliDosa = seedRecipe(true, "Idli dosa", aryan.getId(),
            "Idli rice, urad dal, fenugreek seeds, salt",
            "Step 1: Soak rice and dal separately for several hours.\n"
                + "Step 2: Grind to a smooth batter and ferment overnight.\n"
                + "Step 3: Steam idlis or pan-cook dosas on a hot griddle.",
            80, 3, 2, 30, 250, 8, 80);
        idliDosa.setImagePath(seedImage("dosa.jpg"));
        recipes.save(idliDosa);

        Recipe chickenCurry = seedRecipe(false, "Chicken Curry", aryan.getId(),
            "Chicken, onions, tomatoes, ginger-garlic paste, curry spices",
            "Step 1: Saute onions until golden brown.\n"
                + "Step 2: Add tomatoes and spices, cook into a thick masala.\n"
                + "Step 3: Add chicken and simmer until tender.",
            220, 3, 3, 45, 450, 32, 150);
        chickenCurry.setImagePath(seedImage("chicken-curry.jpg"));
        recipes.save(chickenCurry);

        seedRating(tacos.getId(), priya.getId(), 5, "Crispy and loaded with flavor!");
        seedRating(tacos.getId(), rahul.getId(), 4, null);
        seedRating(biryani.getId(), priya.getId(), 5, "Best biryani I've made at home.");
        seedRating(idliDosa.getId(), rahul.getId(), 4, null);
        seedRating(chickenCurry.getId(), priya.getId(), 3, null);
        seedRating(chickenCurry.getId(), rahul.getId(), 5, "Comfort food done right.");

        Subscription subscription = new Subscription();
        subscription.setSubscriberId(priya.getId());
        subscription.setCreatorId(aryan.getId());
        subscriptions.save(subscription);
    }

    private User seedUser(String username, String email, String phone) {
        User u = new User();
        u.setUsername(username);
        u.setEmail(email);
        u.setPhone(phone);
        u.setPasswordHash(encoder.encode(DEMO_PASSWORD));
        u.setRole(Role.USER);
        u.setAccountStatus(AccountStatus.ACTIVE);
        u.setTwoFactorEnabled(false);
        return users.save(u);
    }

    private Recipe seedRecipe(boolean veg, String name, Long creatorId, String ingredient, String method,
                               int cost, int speed, int difficulty, int cookingTime, int calories, int protein,
                               long views) {
        Recipe recipe = RecipeFactory.create(veg);
        recipe.setRecipeName(name);
        recipe.setIngredient(ingredient);
        recipe.setRecipeMethod(method);
        recipe.setRecipeCreatorId(creatorId);
        recipe.setCost(cost);
        recipe.setSpeed(speed);
        recipe.setDifficulty(difficulty);
        recipe.setViews(views);
        Recipe saved = recipes.save(recipe);

        RecipeCategory category = new RecipeCategory();
        category.setRecipeId(saved.getId());
        category.setVegNonveg(veg);
        category.setCookingTime(cookingTime);
        category.setCalory(calories);
        category.setProtin(protein);
        categories.save(category);

        return saved;
    }

    /**
     * Copies a bundled demo photo (from {@code src/main/resources/static/seed/}) into the
     * configured upload directory so it is reachable at {@code /uploads/<fileName>}, exactly
     * like a user-uploaded recipe photo.
     *
     * @return {@code fileName} on success, or {@code null} if the bundled resource is missing
     *     or the copy fails -- seeding must never crash startup over a missing demo photo.
     */
    private String seedImage(String fileName) {
        try {
            ClassPathResource source = new ClassPathResource("static/seed/" + fileName);
            if (!source.exists()) {
                LOG.warn("Seed image not found on classpath: static/seed/{}", fileName);
                return null;
            }
            Path targetDir = Path.of(uploadDir);
            Files.createDirectories(targetDir);
            try (InputStream in = source.getInputStream()) {
                Files.copy(in, targetDir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            }
            return fileName;
        } catch (IOException e) {
            LOG.warn("Failed to seed demo image: {}", fileName, e);
            return null;
        }
    }

    private void seedRating(Long recipeId, Long userId, int stars, String comment) {
        RecipeRating rating = new RecipeRating();
        rating.setRecipeId(recipeId);
        rating.setUserId(userId);
        switch (stars) {
            case 1 -> rating.setRate1(1);
            case 2 -> rating.setRate2(1);
            case 3 -> rating.setRate3(1);
            case 4 -> rating.setRate4(1);
            case 5 -> rating.setRate5(1);
            default -> throw new IllegalArgumentException("stars must be between 1 and 5, got " + stars);
        }
        if (comment != null) {
            rating.setComments(comment);
        }
        ratings.save(rating);
    }
}
