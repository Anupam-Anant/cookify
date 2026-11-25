package com.cookify.recipe;

import com.cookify.category.*;
import com.cookify.user.AccountStatus;
import com.cookify.user.Role;
import com.cookify.user.User;
import com.cookify.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

// Seeds a real User row and uses its generated id rather than a hardcoded 1L:
// recipe.recipe_creator_id REFERENCES app_user(user_id) NOT NULL (V1), so a hardcoded id
// with no matching app_user row would violate that FK constraint under H2 — same pattern
// as OtpServiceTest.
@DataJpaTest
class RecipeRepositoryTest {
    @Autowired RecipeRepository recipes;
    @Autowired UserRepository users;

    private Long seedUser() {
        User u = new User();
        u.setUsername("recipeowner");
        u.setEmail("recipeowner@example.com");
        u.setPhone("+911111111111");
        u.setPasswordHash("$2a$dummy");
        u.setRole(Role.USER);
        u.setAccountStatus(AccountStatus.ACTIVE);
        return users.save(u).getId();
    }

    @Test void persistsSubclassWithDiscriminator() {
        Long creatorId = seedUser();
        Recipe r = RecipeFactory.create(false);
        r.setRecipeName("Chicken Biryani");
        r.setRecipeCreatorId(creatorId);
        Recipe saved = recipes.save(r);
        assertThat(recipes.findById(saved.getId()).get()).isInstanceOf(NonVegRecipe.class);
    }
}
