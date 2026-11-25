package com.cookify.recipe;

import com.cookify.auth.CookifyUserDetails;
import com.cookify.comment.CommentRejectedException;
import com.cookify.comment.CommentService;
import com.cookify.common.ResourceNotFoundException;
import com.cookify.rating.RatingService;
import com.cookify.saved.SavedRecipeService;
import com.cookify.storage.FileStorageService;
import com.cookify.subscription.SubscriptionRepository;
import com.cookify.subscription.SubscriptionService;
import com.cookify.user.User;
import com.cookify.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Handles both {@code /recipes/**} (CRUD, detail, rate, comment) and the creator-subscribe
 * routes under {@code /creators/**}. There is deliberately no class-level {@code @RequestMapping}
 * prefix so a single controller can serve both path families; every method spells out its
 * full path instead.
 */
@Controller
public class RecipeController {

    private static final Set<String> IMAGE_TYPES = Set.of("image/png", "image/jpeg", "image/webp");
    private static final Set<String> VIDEO_TYPES = Set.of("video/mp4", "video/webm");

    private final RecipeService recipeService;
    private final RecipeRepository recipeRepository;
    private final FileStorageService storage;
    private final UserRepository users;
    private final RatingService ratingService;
    private final CommentService commentService;
    private final SubscriptionService subscriptionService;
    private final SubscriptionRepository subscriptionRepository;
    private final SavedRecipeService savedRecipeService;

    public RecipeController(RecipeService recipeService, RecipeRepository recipeRepository,
                             FileStorageService storage, UserRepository users,
                             RatingService ratingService, CommentService commentService,
                             SubscriptionService subscriptionService, SubscriptionRepository subscriptionRepository,
                             SavedRecipeService savedRecipeService) {
        this.recipeService = recipeService;
        this.recipeRepository = recipeRepository;
        this.storage = storage;
        this.users = users;
        this.ratingService = ratingService;
        this.commentService = commentService;
        this.subscriptionService = subscriptionService;
        this.subscriptionRepository = subscriptionRepository;
        this.savedRecipeService = savedRecipeService;
    }

    @GetMapping("/recipes/new")
    public String newForm(Model model) {
        model.addAttribute("form", new RecipeForm());
        model.addAttribute("editing", false);
        model.addAttribute("pageTitle", "Add Recipe");
        return "recipe/form";
    }

    @PostMapping("/recipes")
    public String create(@AuthenticationPrincipal CookifyUserDetails principal,
                          @Valid @ModelAttribute("form") RecipeForm form,
                          BindingResult binding,
                          @RequestParam(value = "image", required = false) MultipartFile image,
                          @RequestParam(value = "video", required = false) MultipartFile video,
                          Model model) {
        if (binding.hasErrors()) {
            model.addAttribute("editing", false);
            model.addAttribute("pageTitle", "Add Recipe");
            return "recipe/form";
        }
        String imagePath = storeOrNull(image, IMAGE_TYPES);
        String videoPath = storeOrNull(video, VIDEO_TYPES);
        Recipe recipe = recipeService.create(form, principal.getUser().getId(), imagePath, videoPath);
        return "redirect:/recipes/" + recipe.getId();
    }

    @GetMapping("/recipes/{id:\\d+}/edit")
    public String editForm(@AuthenticationPrincipal CookifyUserDetails principal,
                            @PathVariable Long id, Model model) {
        Recipe recipe = recipeService.get(id);
        requireOwner(recipe, principal);
        model.addAttribute("form", toForm(recipe));
        model.addAttribute("recipeId", id);
        model.addAttribute("editing", true);
        model.addAttribute("pageTitle", "Edit Recipe");
        return "recipe/form";
    }

    @PostMapping("/recipes/{id:\\d+}")
    public String update(@AuthenticationPrincipal CookifyUserDetails principal,
                          @PathVariable Long id,
                          @Valid @ModelAttribute("form") RecipeForm form,
                          BindingResult binding,
                          @RequestParam(value = "image", required = false) MultipartFile image,
                          @RequestParam(value = "video", required = false) MultipartFile video,
                          Model model) {
        // Verify ownership BEFORE storing any uploaded file, so a non-owner's
        // request never writes to disk before being rejected.
        recipeService.assertOwner(id, principal.getUser().getId());
        if (binding.hasErrors()) {
            // The dietary type is immutable once created; re-render with the recipe's actual
            // persisted type rather than whatever (possibly mismatched) value was submitted.
            Recipe existing = recipeService.get(id);
            form.setVeg(existing.type() == RecipeType.VEG);
            model.addAttribute("recipeId", id);
            model.addAttribute("editing", true);
            model.addAttribute("pageTitle", "Edit Recipe");
            return "recipe/form";
        }
        String imagePath = storeOrNull(image, IMAGE_TYPES);
        String videoPath = storeOrNull(video, VIDEO_TYPES);
        Recipe recipe = recipeService.update(id, form, principal.getUser().getId(), imagePath, videoPath);
        return "redirect:/recipes/" + recipe.getId();
    }

    @PostMapping("/recipes/{id:\\d+}/delete")
    public String delete(@AuthenticationPrincipal CookifyUserDetails principal, @PathVariable Long id) {
        recipeService.delete(id, principal.getUser().getId());
        return "redirect:/explore";
    }

    @GetMapping("/recipes/{id:\\d+}")
    public String detail(@AuthenticationPrincipal CookifyUserDetails principal, @PathVariable Long id, Model model) {
        Recipe recipe = recipeService.incrementViewsAndGet(id);
        String creatorName = users.findById(recipe.getRecipeCreatorId())
            .map(User::getUsername)
            .orElse("Unknown Chef");
        model.addAttribute("recipe", recipe);
        model.addAttribute("creatorName", creatorName);
        model.addAttribute("steps", splitSteps(recipe.getRecipeMethod()));
        model.addAttribute("ratingSummary", ratingService.summary(id));
        model.addAttribute("comments", commentService.comments(id));

        boolean authenticated = principal != null;
        model.addAttribute("authenticated", authenticated);
        boolean isOwner = authenticated && principal.getUser().getId().equals(recipe.getRecipeCreatorId());
        model.addAttribute("isOwner", isOwner);
        boolean subscribed = authenticated && !isOwner
            && subscriptionService.isSubscribed(principal.getUser().getId(), recipe.getRecipeCreatorId());
        model.addAttribute("subscribed", subscribed);
        boolean saved = authenticated && savedRecipeService.isSaved(principal.getUser().getId(), id);
        model.addAttribute("saved", saved);
        return "recipe/detail";
    }

    @PostMapping("/recipes/{id:\\d+}/save")
    public String save(@AuthenticationPrincipal CookifyUserDetails principal, @PathVariable Long id) {
        savedRecipeService.save(principal.getUser().getId(), id);
        return "redirect:/recipes/" + id;
    }

    @PostMapping("/recipes/{id:\\d+}/unsave")
    public String unsave(@AuthenticationPrincipal CookifyUserDetails principal, @PathVariable Long id) {
        savedRecipeService.unsave(principal.getUser().getId(), id);
        return "redirect:/recipes/" + id;
    }

    @PostMapping("/recipes/{id:\\d+}/rate")
    public String rate(@AuthenticationPrincipal CookifyUserDetails principal,
                        @PathVariable Long id,
                        @RequestParam int stars) {
        ratingService.rate(id, principal.getUser().getId(), stars);
        return "redirect:/recipes/" + id;
    }

    @PostMapping("/recipes/{id:\\d+}/comment")
    public String comment(@AuthenticationPrincipal CookifyUserDetails principal,
                           @PathVariable Long id,
                           @RequestParam String text,
                           RedirectAttributes redirectAttributes) {
        try {
            commentService.addComment(id, principal.getUser().getId(), text);
        } catch (CommentRejectedException e) {
            redirectAttributes.addFlashAttribute("commentError",
                "Comment blocked (inappropriate content) — warning issued");
        }
        return "redirect:/recipes/" + id;
    }

    /**
     * Public creator profile (no auth required to view): the creator's uploaded recipes,
     * follower/following counts, and — for a signed-in visitor viewing someone else's
     * profile — a Subscribe/Unsubscribe button.
     */
    @GetMapping("/creators/{username}")
    public String creatorProfile(@AuthenticationPrincipal CookifyUserDetails principal,
                                  @PathVariable String username, Model model) {
        User creator = users.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("Creator not found: " + username));

        boolean authenticated = principal != null;
        boolean isSelf = authenticated && principal.getUser().getId().equals(creator.getId());
        boolean subscribed = authenticated && !isSelf
            && subscriptionService.isSubscribed(principal.getUser().getId(), creator.getId());

        model.addAttribute("creator", creator);
        model.addAttribute("uploadedRecipes", recipeRepository.findByRecipeCreatorId(creator.getId()));
        model.addAttribute("followerCount", subscriptionRepository.countByCreatorId(creator.getId()));
        model.addAttribute("followingCount", subscriptionRepository.countBySubscriberId(creator.getId()));
        model.addAttribute("authenticated", authenticated);
        model.addAttribute("isSelf", isSelf);
        model.addAttribute("subscribed", subscribed);
        return "user/creator-profile";
    }

    @PostMapping("/creators/{creatorId:\\d+}/subscribe")
    public String subscribe(@AuthenticationPrincipal CookifyUserDetails principal,
                             @PathVariable Long creatorId,
                             @RequestParam(required = false, defaultValue = "/explore") String redirectTo) {
        subscriptionService.subscribe(principal.getUser().getId(), creatorId);
        return "redirect:" + safeRedirect(redirectTo);
    }

    @PostMapping("/creators/{creatorId:\\d+}/unsubscribe")
    public String unsubscribe(@AuthenticationPrincipal CookifyUserDetails principal,
                               @PathVariable Long creatorId,
                               @RequestParam(required = false, defaultValue = "/explore") String redirectTo) {
        subscriptionService.unsubscribe(principal.getUser().getId(), creatorId);
        return "redirect:" + safeRedirect(redirectTo);
    }

    /** Only ever redirect to a same-site relative path — never an attacker-supplied absolute URL. */
    private String safeRedirect(String redirectTo) {
        if (redirectTo == null || !redirectTo.startsWith("/") || redirectTo.startsWith("//")) {
            return "/explore";
        }
        return redirectTo;
    }

    private List<String> splitSteps(String method) {
        if (method == null || method.isBlank()) {
            return List.of();
        }
        return Arrays.stream(method.split("\\r?\\n"))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
    }

    private void requireOwner(Recipe recipe, CookifyUserDetails principal) {
        if (!recipe.getRecipeCreatorId().equals(principal.getUser().getId())) {
            throw new AccessDeniedException("Only the recipe owner may perform this action.");
        }
    }

    private String storeOrNull(MultipartFile file, Set<String> allowed) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        return storage.store(file, allowed);
    }

    private RecipeForm toForm(Recipe recipe) {
        RecipeForm form = new RecipeForm();
        form.setRecipeName(recipe.getRecipeName());
        form.setIngredient(recipe.getIngredient());
        form.setRecipeMethod(recipe.getRecipeMethod());
        form.setVeg(recipe.type() == RecipeType.VEG);
        form.setCost(recipe.getCost());
        form.setSpeed(recipe.getSpeed());
        form.setDifficulty(recipe.getDifficulty());
        return form;
    }
}
