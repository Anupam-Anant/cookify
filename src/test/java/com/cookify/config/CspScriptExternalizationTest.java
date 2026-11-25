package com.cookify.config;

import com.cookify.auth.CookifyUserDetails;
import com.cookify.recipe.Recipe;
import com.cookify.recipe.RecipeFactory;
import com.cookify.recipe.RecipeRepository;
import com.cookify.user.AccountStatus;
import com.cookify.user.Role;
import com.cookify.user.User;
import com.cookify.user.UserRepository;
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
 * Regression guard for the strict-CSP fix: {@code script-src} has no {@code 'unsafe-inline'}, so
 * every page's JS must be loaded from an external, same-origin file under {@code /js/**}
 * (permitted by {@link SecurityConfig}) rather than an inline {@code <script>} body or inline
 * {@code on*} handler. These assert the externalization actually happened and that the files are
 * reachable.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CspScriptExternalizationTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired RecipeRepository recipes;
    @Autowired PasswordEncoder encoder;

    @Test
    void landingPageReferencesExternalLangSwitcherScript() throws Exception {
        mvc.perform(get("/"))
           .andExpect(status().isOk())
           .andExpect(content().string(containsString("/js/lang-switcher.js")));
    }

    @Test
    void chatPageReferencesExternalChatScriptAndItIsServable() throws Exception {
        User seeded = users.findByUsername("cspchatuser").orElseGet(() -> {
            User u = new User();
            u.setUsername("cspchatuser");
            u.setEmail("cspchatuser@example.com");
            u.setPhone("+919999999801");
            u.setPasswordHash(encoder.encode("Tasty-Recipe123"));
            u.setRole(Role.USER);
            u.setAccountStatus(AccountStatus.ACTIVE);
            return users.save(u);
        });

        mvc.perform(get("/chat").with(user(new CookifyUserDetails(seeded))))
           .andExpect(status().isOk())
           .andExpect(content().string(containsString("/js/chat.js")));

        mvc.perform(get("/js/chat.js"))
           .andExpect(status().isOk());
    }

    @Test
    void recipeDetailPageReferencesExternalShareScript() throws Exception {
        User owner = users.findByUsername("cspdetailowner").orElseGet(() -> {
            User u = new User();
            u.setUsername("cspdetailowner");
            u.setEmail("cspdetailowner@example.com");
            u.setPhone("+919999999802");
            u.setPasswordHash(encoder.encode("Tasty-Recipe123"));
            u.setRole(Role.USER);
            u.setAccountStatus(AccountStatus.ACTIVE);
            return users.save(u);
        });
        Recipe r = RecipeFactory.create(true);
        r.setRecipeName("CSP Externalization Check Curry");
        r.setRecipeCreatorId(owner.getId());
        r = recipes.save(r);

        mvc.perform(get("/recipes/" + r.getId()))
           .andExpect(status().isOk())
           .andExpect(content().string(containsString("/js/recipe-detail.js")));
    }
}
