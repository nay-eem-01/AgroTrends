package com.project.agriculturalblogapplication.util;

import java.util.regex.Pattern;

/**
 * Phone numbers are stored in E.164 ({@code +8801712345678}). Bangladeshi local forms ({@code 01712345678}) are
 * accepted: the country code defaults to +880 and the leading trunk 0 is dropped.
 */
public final class PhoneNumbers {

    public static final String DEFAULT_COUNTRY_CODE = "+880";

    private static final Pattern E164 = Pattern.compile("\\+[1-9]\\d{7,14}");
    private static final Pattern SEPARATORS = Pattern.compile("[\\s().-]");

    private PhoneNumbers() {}

    /** The E.164 form of a country code (optional) and a number, or null when that is not a valid phone number. */
    public static String normalise(String countryCode, String number) {
        if (number == null || number.isBlank()) {
            return null;
        }
        String digits = SEPARATORS.matcher(number.strip()).replaceAll("");
        if (digits.startsWith("+")) {
            return valid(digits);
        }
        String code = countryCode == null || countryCode.isBlank() ? DEFAULT_COUNTRY_CODE : countryCode.strip();
        if (!code.startsWith("+")) {
            code = "+" + code;
        }
        if (digits.startsWith("0")) {
            digits = digits.substring(1);
        }
        return valid(code + digits);
    }

    public static String normalise(String number) {
        return normalise(null, number);
    }

    private static String valid(String candidate) {
        return E164.matcher(candidate).matches() ? candidate : null;
    }
}
