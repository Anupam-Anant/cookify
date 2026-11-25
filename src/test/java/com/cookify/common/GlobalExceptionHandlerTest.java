package com.cookify.common;

import com.cookify.auth.CookifyUserDetails;
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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies {@link GlobalExceptionHandler} maps known exceptions to themed HTTP-status-correct
 * error pages instead of letting them surface as Spring Boot's default whitelabel page (or,
 * for an unmapped exception, a raw 500 with no page at all).
 */
@SpringBootTest
@AutoConfigureMockMvc
class GlobalExceptionHandlerTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired RecipeRepository recipes;
    @Autowired PasswordEncoder encoder;

    private User owner;
    private User other;

    @BeforeEach
    void seed() {
        owner = users.findByUsername("gehowner").orElseGet(() -> seedUser("gehowner", "gehowner@example.com", "+917777779991"));
        other = users.findByUsername("gehother").orElseGet(() -> seedUser("gehother", "gehother@example.com", "+917777779992"));
    }

    private User seedUser(String username, String email, String phone) {
        User u = new User();
        u.setUsername(username);
        u.setEmail(email);
        u.setPhone(phone);
        u.setPasswordHash(encoder.encode("Tasty-Recipe123"));
        u.setRole(Role.USER);
        u.setAccountStatus(AccountStatus.ACTIVE);
        return users.save(u);
    }

    @Test
    void nonExistentRecipeReturns404PageNotServerError() throws Exception {
        mvc.perform(get("/recipes/999999").with(user(new CookifyUserDetails(owner))))
           .andExpect(status().isNotFound())
           .andExpect(content().string(containsString("404")))
           .andExpect(content().string(containsString("wandered off")));
    }

    @Test
    void nonExistentRecipeIsA404ForAnonymousUsersToo() throws Exception {
        mvc.perform(get("/recipes/999998"))
           .andExpect(status().isNotFound())
           .andExpect(content().string(containsString("wandered off")));
    }

    @Test
    void nonExistentCreatorReturns404NotBadRequest() throws Exception {
        mvc.perform(get("/creators/nope-does-not-exist"))
           .andExpect(status().isNotFound())
           .andExpect(content().string(containsString("404")))
           .andExpect(content().string(containsString("wandered off")));
    }

    @Test
    void nonOwnerEditAttemptReturns403ThemedPage() throws Exception {
        Recipe r = RecipeFactory.create(true);
        r.setRecipeName("Protected Recipe");
        r.setRecipeCreatorId(owner.getId());
        r = recipes.save(r);

        mvc.perform(get("/recipes/" + r.getId() + "/edit").with(user(new CookifyUserDetails(other))))
           .andExpect(status().isForbidden())
           .andExpect(content().string(containsString("403")))
           .andExpect(content().string(containsString("Only the chef")));
    }
}
