package com.cookify.comment;

import com.cookify.moderation.ContentFilter;
import com.cookify.moderation.ModerationService;
import com.cookify.email.EmailService;
import com.cookify.rating.RecipeRatingRepository;
import com.cookify.recipe.Recipe;
import com.cookify.recipe.RecipeFactory;
import com.cookify.recipe.RecipeRepository;
import com.cookify.user.AccountStatus;
import com.cookify.user.Role;
import com.cookify.user.User;
import com.cookify.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

// @DataJpaTest supplies real RecipeRatingRepository/RecipeRepository/UserRepository (FK
// constraints mean recipe/user rows must really exist). ContentFilter, ModerationService
// and EmailService are NOT JPA beans and are not loaded by this slice, so they are mocked
// directly (same deviation pattern as OtpServiceTest/RatingServiceTest).
@DataJpaTest
class CommentServiceTest {

    @Autowired RecipeRatingRepository ratingRepo;
    @Autowired RecipeRepository recipes;
    @Autowired UserRepository users;

    ContentFilter contentFilter;
    ModerationService moderationService;
    EmailService emailService;
    CommentService service;

    Long recipeId;
    Long userId;

    @BeforeEach
    void setUp() {
        contentFilter = mock(ContentFilter.class);
        moderationService = mock(ModerationService.class);
        emailService = mock(EmailService.class);
        service = new CommentService(ratingRepo, contentFilter, moderationService, emailService, users);

        Long creatorId = seedUser("chef2");
        Recipe r = RecipeFactory.create(true);
        r.setRecipeName("Aloo Paratha");
        r.setRecipeCreatorId(creatorId);
        recipeId = recipes.save(r).getId();

        userId = seedUser("commenter1");
    }

    private Long seedUser(String username) {
        User u = new User();
        u.setUsername(username);
        u.setEmail(username + "@example.com");
        u.setPhone("+91" + Math.abs(username.hashCode() % 1000000000));
        u.setPasswordHash("$2a$dummy");
        u.setRole(Role.USER);
        u.setAccountStatus(AccountStatus.ACTIVE);
        return users.save(u).getId();
    }

    @Test
    void cleanCommentIsStoredAndListed() {
        when(contentFilter.isClean("Delicious, will make again!")).thenReturn(true);

        service.addComment(recipeId, userId, "Delicious, will make again!");

        var comments = service.comments(recipeId);
        assertThat(comments).hasSize(1);
        assertThat(comments.get(0).username()).isEqualTo("commenter1");
        assertThat(comments.get(0).text()).isEqualTo("Delicious, will make again!");

        verifyNoInteractions(moderationService);
        verifyNoInteractions(emailService);
    }

    @Test
    void nsfwCommentIsRejectedNotStoredAndWarns() {
        when(contentFilter.isClean("bad stuff")).thenReturn(false);

        assertThatThrownBy(() -> service.addComment(recipeId, userId, "bad stuff"))
            .isInstanceOf(CommentRejectedException.class);

        assertThat(service.comments(recipeId)).isEmpty();
        verify(moderationService).warn(eq(userId), anyString());
        verify(emailService).sendNotification(eq("commenter1@example.com"), anyString(), anyString());
    }
}
