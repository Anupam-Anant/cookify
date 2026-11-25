package com.cookify.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;

import static org.assertj.core.api.Assertions.assertThat;

class ProdProfileConfigTest {

    @Test
    void prodProfileUsesPostgres() {
        StandardEnvironment env = new StandardEnvironment();
        env.setActiveProfiles("prod");
        ConfigDataEnvironmentPostProcessor.applyTo(env);
        String url = Binder.get(env).bind("spring.datasource.url", String.class).orElse("");
        assertThat(url).contains("postgresql");
    }
}
