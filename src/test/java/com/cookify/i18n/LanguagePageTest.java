package com.cookify.i18n;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Verifies the Google Translate widget + language switcher are present site-wide (public pages)
 * and that the CSP header was widened to permit the Google Translate hosts.
 */
@SpringBootTest
@AutoConfigureMockMvc
class LanguagePageTest {

    @Autowired MockMvc mvc;

    @Test
    void landingPageIncludesGoogleTranslateWidgetAndLanguageOptionsAndCsp() throws Exception {
        mvc.perform(get("/"))
           .andExpect(status().isOk())
           .andExpect(content().string(containsString("google_translate_element")))
           .andExpect(content().string(containsString("\"hi\"")))
           .andExpect(content().string(containsString("zh-CN")))
           .andExpect(header().string("Content-Security-Policy", containsString("translate.google")));
    }

    @Test
    void explorePageAlsoIncludesLanguageWidget() throws Exception {
        mvc.perform(get("/explore"))
           .andExpect(status().isOk())
           .andExpect(content().string(containsString("google_translate_element")))
           .andExpect(content().string(containsString("zh-CN")));
    }
}
