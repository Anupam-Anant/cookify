package com.cookify.common;

import com.cookify.recipe.RecipeNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Centralised mapping from exceptions raised by controllers/services to themed HTML error
 * pages, so a failure never surfaces as Spring Boot's default whitelabel page or a raw stack
 * trace. The view names returned here ("error/404", "error/403", ...) are the SAME templates
 * Spring Boot auto-resolves for container-level errors (unmapped routes, etc.) via
 * {@code templates/error/{status}.html}, so every path to an error page ends up themed and
 * consistent.
 *
 * <ul>
 *   <li>{@link RecipeNotFoundException} -&gt; 404 (recipe lookup by id failed).</li>
 *   <li>{@link ResourceNotFoundException} -&gt; 404 (any other "entity not found by id/name"
 *       lookup, e.g. a creator profile looked up by username, or a user looked up by id).</li>
 *   <li>{@link AccessDeniedException} -&gt; 403 ({@code RecipeService}/{@code RecipeController}'s
 *       owner-only checks throw Spring Security's {@code AccessDeniedException} directly, so
 *       there is no separate application-level type to also handle here).</li>
 *   <li>{@link IllegalArgumentException} -&gt; 400. Reserved for genuine bad input (invalid star
 *       rating, blank chat message) now that "not found" lookups use
 *       {@link ResourceNotFoundException} instead; 400 Bad Request is the one HTTP-standard
 *       meaning of {@code IllegalArgumentException}.</li>
 *   <li>Anything else -&gt; logged at ERROR with the full exception, then 500.</li>
 * </ul>
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RecipeNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleRecipeNotFound(RecipeNotFoundException ex) {
        return "error/404";
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleResourceNotFound(ResourceNotFoundException ex) {
        return "error/404";
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleAccessDenied(AccessDeniedException ex) {
        return "error/403";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleIllegalArgument(IllegalArgumentException ex) {
        return "error/400";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleUnexpected(Exception ex) {
        log.error("Unhandled exception reached GlobalExceptionHandler", ex);
        return "error/500";
    }
}
