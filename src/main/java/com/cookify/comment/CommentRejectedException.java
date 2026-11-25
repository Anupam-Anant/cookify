package com.cookify.comment;

/** Thrown when a comment fails the {@code ContentFilter} NSFW gate; never stored. */
public class CommentRejectedException extends RuntimeException {
    public CommentRejectedException(String message) {
        super(message);
    }
}
