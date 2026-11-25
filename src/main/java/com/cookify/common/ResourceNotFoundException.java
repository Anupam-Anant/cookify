package com.cookify.common;

/**
 * Thrown when an entity is looked up by an identifier (id, username, ...) supplied by the
 * caller and no such entity exists. Distinct from {@link IllegalArgumentException}, which
 * remains reserved for genuine bad-input cases (e.g. an out-of-range star rating). Mapped by
 * {@link GlobalExceptionHandler} to the themed 404 page.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
