package com.cookify.schema;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * V5 adds supporting indexes for the FK/filter columns hit hardest by Explore/search, plus a
 * DB-level unique constraint that makes the "one rating row per (recipe, user)" invariant
 * enforceable even if application code ever regresses.
 */
@SpringBootTest
class SchemaV5Test {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void recipeRatingHasUniqueConstraintOnRecipeAndUser() {
        List<String> constraintNames = jdbc.queryForList(
            "SELECT constraint_name FROM information_schema.table_constraints " +
                "WHERE LOWER(table_name)='recipe_rating' AND constraint_type='UNIQUE'",
            String.class);
        assertThat(constraintNames).isNotEmpty();
    }

    @Test
    void duplicateRecipeRatingForSameUserIsRejectedByTheDatabase() {
        jdbc.update("INSERT INTO app_user (email, username, pass) VALUES (?, ?, ?)",
            "schemav5-creator@example.com", "schemav5creator", "x");
        Long creatorId = jdbc.queryForObject(
            "SELECT user_id FROM app_user WHERE username = ?", Long.class, "schemav5creator");

        jdbc.update("INSERT INTO app_user (email, username, pass) VALUES (?, ?, ?)",
            "schemav5-rater@example.com", "schemav5rater", "x");
        Long raterId = jdbc.queryForObject(
            "SELECT user_id FROM app_user WHERE username = ?", Long.class, "schemav5rater");

        jdbc.update("INSERT INTO recipe (recipe_name, recipe_creator_id) VALUES (?, ?)",
            "Schema V5 Test Curry", creatorId);
        Long recipeId = jdbc.queryForObject(
            "SELECT recipe_id FROM recipe WHERE recipe_name = ?", Long.class, "Schema V5 Test Curry");

        jdbc.update("INSERT INTO recipe_rating (recipe_id, user_id, rate_5) VALUES (?, ?, 1)",
            recipeId, raterId);

        assertThatThrownBy(() ->
            jdbc.update("INSERT INTO recipe_rating (recipe_id, user_id, rate_1) VALUES (?, ?, 1)",
                recipeId, raterId))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void coreIndexesExist() {
        List<String> indexedTables = jdbc.queryForList(
            "SELECT LOWER(table_name) FROM information_schema.indexes",
            String.class);
        assertThat(indexedTables).contains("recipe_rating", "chat_message", "subscription");
    }
}
