package com.project.agriculturalblogapplication.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.random.RandomGenerator;

/** URL slugs that keep Bangla (and any other script) readable instead of stripping it to nothing. */
public final class Slugs {

    private static final int MAX_BASE_LENGTH = 60;
    private static final int SUFFIX_LENGTH = 6;
    private static final String SUFFIX_ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";

    private Slugs() {}

    /** Letters, combining marks (Bangla vowel signs) and digits survive; everything else becomes a single dash. */
    public static String base(String title) {
        String slug = Normalizer.normalize(title == null ? "" : title, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{M}\\p{N}]+", "-")
                .replaceAll("(^-+|-+$)", "");
        if (slug.length() > MAX_BASE_LENGTH) {
            slug = slug.substring(0, MAX_BASE_LENGTH).replaceAll("-+$", "");
        }
        return slug.isEmpty() ? "post" : slug;
    }

    /** {@code base-xxxxxx}: the random suffix keeps slugs unique without exposing the database id. */
    public static String withSuffix(String base, RandomGenerator random) {
        StringBuilder slug = new StringBuilder(base).append('-');
        for (int i = 0; i < SUFFIX_LENGTH; i++) {
            slug.append(SUFFIX_ALPHABET.charAt(random.nextInt(SUFFIX_ALPHABET.length())));
        }
        return slug.toString();
    }

    /** About 200 words a minute, never less than one minute. */
    public static int readingTimeMinutes(String content) {
        if (content == null || content.isBlank()) {
            return 1;
        }
        int words = content.trim().split("\\s+").length;
        return Math.max(1, (words + 199) / 200);
    }
}
