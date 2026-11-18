package com.project.agriculturalblogapplication.util;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlugsTest {

    @Test
    void englishTitlesBecomeLowercaseDashedWords() {
        assertEquals("managing-rice-blast-in-2026", Slugs.base("  Managing Rice Blast (in 2026)! "));
    }

    @Test
    void banglaTitlesKeepTheirLettersAndVowelSigns() {
        // The old generateSlugFromString stripped every non-Latin character, leaving Bangla titles empty.
        assertEquals("ধানের-ব্লাস্ট-রোগ", Slugs.base("ধানের ব্লাস্ট রোগ?"));
    }

    @Test
    void emptyOrSymbolOnlyTitlesFallBackAndLongTitlesAreCut() {
        assertEquals("post", Slugs.base("!!!"));
        assertEquals("post", Slugs.base(null));
        String longSlug = Slugs.base("word ".repeat(40));
        assertTrue(longSlug.length() <= 60 && !longSlug.endsWith("-"));
    }

    @Test
    void suffixIsSixLowercaseLettersOrDigits() {
        String slug = Slugs.withSuffix("rice-blast", new Random(1));
        assertTrue(slug.matches("rice-blast-[a-z0-9]{6}"), slug);
    }

    @Test
    void readingTimeIsAboutTwoHundredWordsAMinuteAndAtLeastOne() {
        assertEquals(1, Slugs.readingTimeMinutes("Short post."));
        assertEquals(1, Slugs.readingTimeMinutes(null));
        assertEquals(2, Slugs.readingTimeMinutes("word ".repeat(201)));
        assertEquals(5, Slugs.readingTimeMinutes("শব্দ ".repeat(1000)));
    }
}
