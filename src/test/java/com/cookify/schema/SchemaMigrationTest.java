package com.cookify.schema;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SchemaMigrationTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void allCoreTablesExist() {
        List<String> tables = jdbc.queryForList(
            "SELECT LOWER(table_name) FROM information_schema.tables WHERE table_schema='PUBLIC'",
            String.class);
        assertThat(tables).contains(
            "app_user", "user_preference", "recipe", "recipe_category",
            "recipe_rating", "persistent_logins", "email_otp",
            "subscription", "warning", "chat_message", "saved_recipe");
    }
}
