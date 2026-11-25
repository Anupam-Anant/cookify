package com.cookify.schema;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RecipeSchemaV2Test {
    @Autowired JdbcTemplate jdbc;

    @Test void recipeHasTypeAndAttributeColumns() {
        List<String> cols = jdbc.queryForList(
          "SELECT LOWER(column_name) FROM information_schema.columns WHERE LOWER(table_name)='recipe'",
          String.class);
        assertThat(cols).contains("recipe_type", "cost", "speed", "difficulty");
    }
}
