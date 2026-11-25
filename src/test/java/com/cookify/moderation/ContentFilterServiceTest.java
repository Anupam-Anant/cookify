package com.cookify.moderation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContentFilterServiceTest {

    private final ContentFilter filter = new RuleBasedContentFilter();

    @Test
    void cleanTextPasses() {
        assertThat(filter.isClean("This recipe is absolutely delicious, thanks for sharing!")).isTrue();
    }

    @Test
    void nullOrBlankTextIsClean() {
        assertThat(filter.isClean(null)).isTrue();
        assertThat(filter.isClean("")).isTrue();
        assertThat(filter.isClean("   ")).isTrue();
    }

    @Test
    void bannedWordFails() {
        assertThat(filter.isClean("what a badword1 comment")).isFalse();
    }

    @Test
    void caseInsensitiveMatch() {
        assertThat(filter.isClean("BadWord1 in the middle")).isFalse();
        assertThat(filter.isClean("ASS used as a standalone word")).isFalse();
    }

    @Test
    void wordBoundaryAvoidsFalsePositiveOnSubstring() {
        // "ass" is banned but must not flag innocent words containing it as a substring.
        assertThat(filter.isClean("I'm taking a class on assembly and classical music")).isTrue();
    }
}
