package com.cookify.user;

import com.cookify.auth.CookifyUserDetails;
import com.cookify.common.ResourceNotFoundException;
import com.cookify.recipe.RecipeRepository;
import com.cookify.saved.SavedRecipeService;
import com.cookify.storage.FileStorageService;
import com.cookify.subscription.SubscriptionRepository;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private static final Set<String> IMAGE_TYPES = Set.of("image/png", "image/jpeg", "image/webp");

    private final UserRepository users;
    private final FileStorageService storage;
    private final RecipeRepository recipes;
    private final SavedRecipeService savedRecipeService;
    private final SubscriptionRepository subscriptionRepository;

    public ProfileController(UserRepository users, FileStorageService storage, RecipeRepository recipes,
                              SavedRecipeService savedRecipeService, SubscriptionRepository subscriptionRepository) {
        this.users = users;
        this.storage = storage;
        this.recipes = recipes;
        this.savedRecipeService = savedRecipeService;
        this.subscriptionRepository = subscriptionRepository;
    }

    @GetMapping
    public String view(@AuthenticationPrincipal CookifyUserDetails principal, Model model) {
        User user = users.findById(principal.getUser().getId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + principal.getUser().getId()));
        model.addAttribute("user", user);
        model.addAttribute("uploadedRecipes", recipes.findByRecipeCreatorId(user.getId()));
        model.addAttribute("savedRecipes", savedRecipeService.savedRecipes(user.getId()));
        model.addAttribute("followerCount", subscriptionRepository.countByCreatorId(user.getId()));
        model.addAttribute("followingCount", subscriptionRepository.countBySubscriberId(user.getId()));
        return "user/profile";
    }

    @GetMapping("/edit")
    public String editForm(@AuthenticationPrincipal CookifyUserDetails principal, Model model) {
        User user = users.findById(principal.getUser().getId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + principal.getUser().getId()));
        model.addAttribute("user", user);
        model.addAttribute("form", toForm(user));
        return "user/edit-profile";
    }

    @PostMapping("/edit")
    public String update(@AuthenticationPrincipal CookifyUserDetails principal,
                         @Valid @ModelAttribute("form") ProfileForm form,
                         BindingResult binding,
                         @RequestParam(value = "avatar", required = false) MultipartFile avatar) {
        if (binding.hasErrors()) {
            return "user/edit-profile";
        }
        User user = users.findById(principal.getUser().getId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + principal.getUser().getId()));
        user.setNameFirst(form.getNameFirst());
        user.setNameLast(form.getNameLast());
        user.setAge(form.getAge());
        user.setGender(form.getGender());
        user.setBio(form.getBio());
        user.setTwoFactorEnabled(form.isTwoFactorEnabled());
        if (avatar != null && !avatar.isEmpty()) {
            user.setProfileImagePath(storage.store(avatar, IMAGE_TYPES));
        }
        users.save(user);
        return "redirect:/profile";
    }

    private ProfileForm toForm(User user) {
        ProfileForm form = new ProfileForm();
        form.setNameFirst(user.getNameFirst());
        form.setNameLast(user.getNameLast());
        form.setAge(user.getAge());
        form.setGender(user.getGender());
        form.setBio(user.getBio());
        form.setTwoFactorEnabled(user.isTwoFactorEnabled());
        return form;
    }
}
