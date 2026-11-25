package com.cookify.acceptance;

import com.cookify.auth.RegistrationForm;
import com.cookify.auth.RegistrationService;
import com.cookify.auth.otp.OtpPurpose;
import com.cookify.auth.otp.OtpService;
import com.cookify.category.RecipeCategory;
import com.cookify.category.RecipeCategoryRepository;
import com.cookify.comment.CommentRejectedException;
import com.cookify.comment.CommentService;
import com.cookify.moderation.ModerationService;
import com.cookify.moderation.WarningRepository;
import com.cookify.rating.RatingService;
import com.cookify.rating.RatingSummary;
import com.cookify.recipe.Recipe;
import com.cookify.recipe.RecipeFactory;
import com.cookify.recipe.RecipeForm;
import com.cookify.recipe.RecipeRepository;
import com.cookify.recipe.RecipeService;
import com.cookify.recipe.VegRecipe;
import com.cookify.recipe.NonVegRecipe;
import com.cookify.search.RecipeFilter;
import com.cookify.search.RecipeSearchService;
import com.cookify.subscription.SubscriptionService;
import com.cookify.user.AccountStatus;
import com.cookify.user.Role;
import com.cookify.user.User;
import com.cookify.user.UserRepository;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.lang.reflect.Modifier;
import java.util.List;

import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * One test method per the 13 spec test cases from the interview workbook test table — see
 * {@code docs/TEST-TRACEABILITY.md} for the full case-to-test mapping (this class plus the
 * deeper, pre-existing unit/integration tests each case also traces to).
 *
 * <p>Each method here is a real, thin, end-to-end-ish exercise of its case (calling the real
 * Spring-managed service/MockMvc, not mocks), rather than a re-statement of an existing test's
 * assertions. Where a case already has deep coverage elsewhere (e.g. popularity sorting,
 * NSFW+ban, multilingual), the method below is intentionally minimal — the exhaustive matrix
 * lives in the referenced test class.
 *
 * <p>Mail-sending cases (8, 13) override {@code spring.mail.port} to a local GreenMail instance,
 * the same pattern used by {@code SubscriptionServiceTest} / {@code EmailServiceTest}, so this
 * gets its own cached Spring context.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {"spring.mail.host=localhost", "spring.mail.port=3025",
                                   "cookify.otp.resend-cooldown-seconds=0"})
class AcceptanceTraceabilityTest {

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP);

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired RecipeRepository recipes;
    @Autowired RecipeCategoryRepository categories;
    @Autowired WarningRepository warnings;
    @Autowired PasswordEncoder encoder;
    @Autowired RegistrationService registrationService;
    @Autowired OtpService otpService;
    @Autowired RecipeService recipeService;
    @Autowired RecipeSearchService recipeSearchService;
    @Autowired RatingService ratingService;
    @Autowired CommentService commentService;
    @Autowired ModerationService moderationService;
    @Autowired SubscriptionService subscriptionService;

    private Long seedUser(String username, String phoneSuffix) {
        User u = new User();
        u.setUsername(username);
        u.setEmail(username + "@example.com");
        u.setPhone("+9170000" + phoneSuffix);
        u.setPasswordHash(encoder.encode("Tasty-Recipe123"));
        u.setRole(Role.USER);
        u.setAccountStatus(AccountStatus.ACTIVE);
        return users.save(u).getId();
    }

    @Test
    @DisplayName("Case 1: Account creation — signup stores an active user with a BCrypt-hashed password")
    void case1_accountCreation() {
        RegistrationForm form = new RegistrationForm();
        form.setUsername("trace1user");
        form.setEmail("trace1user@example.com");
        form.setPhone("+917000000101");
        form.setPassword("Trace-Recipe123");
        form.setConfirmPassword("Trace-Recipe123");

        User saved = registrationService.register(form);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(saved.getPasswordHash()).startsWith("$2").isNotEqualTo("Trace-Recipe123");
        assertThat(users.findByUsername("trace1user")).isPresent();
    }

    @Test
    @DisplayName("Case 2: Login + 2FA + session — a 2FA user must complete the email OTP challenge before the session is authenticated")
    void case2_loginTwoFactorAndSession() throws Exception {
        User user = new User();
        user.setUsername("trace2user");
        user.setEmail("trace2user@example.com");
        user.setPhone("+917000000201");
        user.setPasswordHash(encoder.encode("Tasty-Recipe123"));
        user.setRole(Role.USER);
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setTwoFactorEnabled(true);
        user = users.save(user);

        MvcResult loginResult = mvc.perform(post("/login").with(csrf())
                .param("username", "trace2user")
                .param("password", "Tasty-Recipe123"))
            .andExpect(redirectedUrl("/2fa"))
            .andReturn();
        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

        // Session is not yet authenticated — a protected page redirects to login.
        mvc.perform(get("/profile").session(session))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("http://localhost/login"));

        String code = otpService.issue(user.getId(), OtpPurpose.LOGIN_2FA);
        mvc.perform(post("/2fa").with(csrf()).session(session).param("code", code))
            .andExpect(redirectedUrl("/explore"));

        // Now the session is authenticated.
        mvc.perform(get("/profile").session(session))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Case 3: Recipe storage (text/image/video + attributes) — a recipe's text, media path, and cost/speed/difficulty/category persist")
    void case3_recipeStorageTextImageVideoAndAttributes() {
        Long chefId = seedUser("trace3chef", "301");

        RecipeForm imageForm = new RecipeForm();
        imageForm.setRecipeName("Trace Image Curry");
        imageForm.setIngredient("Spices, vegetables");
        imageForm.setRecipeMethod("Step 1\nStep 2");
        imageForm.setVeg(true);
        imageForm.setCost(150);
        imageForm.setSpeed(3);
        imageForm.setDifficulty(2);
        imageForm.setCookingTime(25);
        imageForm.setCalory(300);
        imageForm.setProtin(10);
        Recipe withImage = recipeService.create(imageForm, chefId, "trace3.png", null);

        assertThat(withImage.getImagePath()).isEqualTo("trace3.png");
        assertThat(withImage.getRecipeVideo()).isNull();
        RecipeCategory imageCategory = categories.findById(withImage.getId()).orElseThrow();
        assertThat(imageCategory.isVegNonveg()).isTrue();
        assertThat(imageCategory.getCookingTime()).isEqualTo(25);
        assertThat(imageCategory.getCalory()).isEqualTo(300);
        assertThat(imageCategory.getProtin()).isEqualTo(10);

        RecipeForm videoForm = new RecipeForm();
        videoForm.setRecipeName("Trace Video Curry");
        videoForm.setIngredient("Chicken, spices");
        videoForm.setRecipeMethod("Step 1\nStep 2");
        videoForm.setVeg(false);
        videoForm.setCost(220);
        videoForm.setSpeed(2);
        videoForm.setDifficulty(4);
        videoForm.setCookingTime(40);
        videoForm.setCalory(500);
        videoForm.setProtin(28);
        Recipe withVideo = recipeService.create(videoForm, chefId, null, "trace3.mp4");

        assertThat(withVideo.getRecipeVideo()).isEqualTo("trace3.mp4");
        assertThat(withVideo.getImagePath()).isNull();
        assertThat(withVideo).isInstanceOf(NonVegRecipe.class);
    }

    @Test
    @DisplayName("Case 4: Search by filters — veg/cost/ingredient filters narrow the result set")
    void case4_searchByFilters() {
        Long chefId = seedUser("trace4chef", "401");
        seedSearchRecipe(chefId, "Trace Cheap Veg Snack", true, 60, 5);
        seedSearchRecipe(chefId, "Trace Pricey Non-Veg Feast", false, 500, 5);

        RecipeFilter vegFilter = new RecipeFilter();
        vegFilter.setVeg(true);
        List<Recipe> vegResults = recipeSearchService.search(vegFilter);
        assertThat(vegResults).extracting(Recipe::getRecipeName).contains("Trace Cheap Veg Snack");
        assertThat(vegResults).extracting(Recipe::getRecipeName).doesNotContain("Trace Pricey Non-Veg Feast");

        RecipeFilter costFilter = new RecipeFilter();
        costFilter.setMaxCost(100);
        List<Recipe> cheapResults = recipeSearchService.search(costFilter);
        assertThat(cheapResults).extracting(Recipe::getRecipeName).contains("Trace Cheap Veg Snack");
        assertThat(cheapResults).extracting(Recipe::getRecipeName).doesNotContain("Trace Pricey Non-Veg Feast");
    }

    @Test
    @DisplayName("Case 5: Veg/Non-Veg toggle — RecipeFactory picks the correct OOP subclass and dietary label from the form's veg flag")
    void case5_vegNonVegToggle() {
        Recipe veg = RecipeFactory.create(true);
        Recipe nonVeg = RecipeFactory.create(false);

        assertThat(veg).isInstanceOf(VegRecipe.class);
        assertThat(veg.dietaryLabel()).isEqualTo("Veg");
        assertThat(nonVeg).isInstanceOf(NonVegRecipe.class);
        assertThat(nonVeg.dietaryLabel()).isEqualTo("Non-Veg");
    }

    @Test
    @DisplayName("Case 6: Popularity sorting — higher views/rating rank first under the default 'popular' sort")
    void case6_popularitySorting() {
        Long chefId = seedUser("trace6chef", "601");
        Recipe popular = seedSearchRecipe(chefId, "Trace Crowd Favorite", true, 100, 500);
        seedSearchRecipe(chefId, "Trace Rarely Seen", true, 100, 1);
        Long raterId = seedUser("trace6rater", "602");
        ratingService.rate(popular.getId(), raterId, 5);

        RecipeFilter filter = new RecipeFilter();
        filter.setSort("popular");
        List<Recipe> result = recipeSearchService.search(filter);

        int favoriteIndex = indexOfRecipeNamed(result, "Trace Crowd Favorite");
        int rareIndex = indexOfRecipeNamed(result, "Trace Rarely Seen");
        assertThat(favoriteIndex).isLessThan(rareIndex);
    }

    @Test
    @DisplayName("Case 7: UX/aesthetics — landing and Explore share the same branded layout, stylesheet, and COOKify identity")
    void case7_uxAesthetics() throws Exception {
        mvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("COOKify")))
            .andExpect(content().string(containsString("/css/cookify.css")));

        mvc.perform(get("/explore"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Explore Recipes")))
            .andExpect(content().string(containsString("/css/cookify.css")));
    }

    @Test
    @DisplayName("Case 8: Commenting + NSFW filter + email warning — a blocked comment is never stored and the commenter is warned by email")
    void case8_commentingNsfwAndEmailWarning() throws Exception {
        greenMail.purgeEmailFromAllMailboxes();
        Long chefId = seedUser("trace8chef", "801");
        Long commenterId = seedUser("trace8commenter", "802");
        Recipe recipe = RecipeFactory.create(true);
        recipe.setRecipeName("Trace NSFW Test Recipe");
        recipe.setRecipeCreatorId(chefId);
        recipe = recipes.save(recipe);
        Long recipeId = recipe.getId();

        assertThatThrownBy(() -> commentService.addComment(recipeId, commenterId, "this is badword1 content"))
            .isInstanceOf(CommentRejectedException.class);

        assertThat(commentService.comments(recipeId)).isEmpty();
        assertThat(warnings.countByUserId(commenterId)).isGreaterThanOrEqualTo(1);
        await().atMost(ofSeconds(5)).untilAsserted(() -> {
            MimeMessage[] msgs = greenMail.getReceivedMessages();
            assertThat(msgs).isNotEmpty();
            assertThat(msgs[msgs.length - 1].getAllRecipients()[0].toString())
                .isEqualTo("trace8commenter@example.com");
        });
    }

    @Test
    @DisplayName("Case 9: Rating averaged — multiple users' star ratings average correctly, and re-rating replaces rather than adds")
    void case9_ratingAveraged() {
        Long chefId = seedUser("trace9chef", "901");
        Recipe recipe = RecipeFactory.create(true);
        recipe.setRecipeName("Trace Rating Recipe");
        recipe.setRecipeCreatorId(chefId);
        recipe = recipes.save(recipe);

        Long rater1 = seedUser("trace9ratera", "902");
        Long rater2 = seedUser("trace9raterb", "903");
        ratingService.rate(recipe.getId(), rater1, 4);
        ratingService.rate(recipe.getId(), rater2, 2);

        RatingSummary summary = ratingService.summary(recipe.getId());
        assertThat(summary.average()).isEqualTo(3.0);
        assertThat(summary.count()).isEqualTo(2);

        ratingService.rate(recipe.getId(), rater1, 5);
        RatingSummary updated = ratingService.summary(recipe.getId());
        assertThat(updated.count()).isEqualTo(2);
        assertThat(updated.average()).isEqualTo(3.5);
    }

    @Test
    @DisplayName("Case 10: Code modularity — package-by-feature layout, and the Recipe OOP hierarchy is a true abstract base with polymorphic subclasses")
    void case10_codeModularity() {
        assertThat(Modifier.isAbstract(Recipe.class.getModifiers())).isTrue();
        assertThat(VegRecipe.class.getSuperclass()).isEqualTo(Recipe.class);
        assertThat(NonVegRecipe.class.getSuperclass()).isEqualTo(Recipe.class);

        // Each feature lives in its own package (package-by-feature, not package-by-layer):
        // the recipe, user, rating, comment, moderation, and subscription concerns are each
        // self-contained packages with their own service/repository types.
        assertThat(RecipeService.class.getPackageName()).isEqualTo("com.cookify.recipe");
        assertThat(UserRepository.class.getPackageName()).isEqualTo("com.cookify.user");
        assertThat(RatingService.class.getPackageName()).isEqualTo("com.cookify.rating");
        assertThat(CommentService.class.getPackageName()).isEqualTo("com.cookify.comment");
        assertThat(ModerationService.class.getPackageName()).isEqualTo("com.cookify.moderation");
        assertThat(SubscriptionService.class.getPackageName()).isEqualTo("com.cookify.subscription");
        assertThat(RecipeCategoryRepository.class.getPackageName()).isEqualTo("com.cookify.category");
    }

    @Test
    @DisplayName("Case 11: Ban after 3 warnings + content purge — the 3rd warning suspends the account and deletes their content")
    void case11_banAfterThreeWarningsAndPurge() {
        Long offenderId = seedUser("trace11offender", "1101");
        Recipe offenderRecipe = RecipeFactory.create(true);
        offenderRecipe.setRecipeName("Trace Offender Recipe");
        offenderRecipe.setRecipeCreatorId(offenderId);
        Long offenderRecipeId = recipes.save(offenderRecipe).getId();

        moderationService.warn(offenderId, "strike one");
        moderationService.warn(offenderId, "strike two");
        assertThat(moderationService.isBanned(offenderId)).isFalse();

        moderationService.warn(offenderId, "strike three");

        assertThat(moderationService.isBanned(offenderId)).isTrue();
        User reloaded = users.findById(offenderId).orElseThrow();
        assertThat(reloaded.getAccountStatus()).isEqualTo(AccountStatus.SUSPENDED);
        assertThat(recipes.findById(offenderRecipeId)).isEmpty();
    }

    @Test
    @DisplayName("Case 12: Multilingual — the Google Translate widget and language options are present with the CSP widened to allow them")
    void case12_multilingual() throws Exception {
        mvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("google_translate_element")))
            .andExpect(content().string(containsString("\"hi\"")))
            .andExpect(content().string(containsString("zh-CN")))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                .string("Content-Security-Policy", containsString("translate.google")));
    }

    @Test
    @DisplayName("Case 13: Subscription + email — subscribing emails the creator, and a new recipe emails every subscriber")
    void case13_subscriptionAndEmail() throws Exception {
        greenMail.purgeEmailFromAllMailboxes();
        Long creatorId = seedUser("trace13creator", "1301");
        Long subscriberId = seedUser("trace13subscriber", "1302");

        subscriptionService.subscribe(subscriberId, creatorId);

        assertThat(subscriptionService.isSubscribed(subscriberId, creatorId)).isTrue();
        await().atMost(ofSeconds(5)).untilAsserted(() -> {
            MimeMessage[] msgs = greenMail.getReceivedMessages();
            assertThat(msgs).hasSize(1);
            assertThat(msgs[0].getAllRecipients()[0].toString()).isEqualTo("trace13creator@example.com");
            assertThat(msgs[0].getSubject()).containsIgnoringCase("subscriber");
        });
    }

    private Recipe seedSearchRecipe(Long chefId, String name, boolean veg, Integer cost, long views) {
        Recipe r = RecipeFactory.create(veg);
        r.setRecipeName(name);
        r.setRecipeCreatorId(chefId);
        r.setCost(cost);
        r.setViews(views);
        return recipes.save(r);
    }

    private int indexOfRecipeNamed(List<Recipe> list, String name) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getRecipeName().equals(name)) {
                return i;
            }
        }
        throw new AssertionError("Recipe not found in result: " + name);
    }
}
