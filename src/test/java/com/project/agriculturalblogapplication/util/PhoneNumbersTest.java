package com.project.agriculturalblogapplication.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PhoneNumbersTest {

    @Test
    void bangladeshiLocalFormBecomesE164() {
        assertEquals("+8801712345678", PhoneNumbers.normalise("01712345678"));
        assertEquals("+8801712345678", PhoneNumbers.normalise("+880", "01712345678"));
        assertEquals("+8801712345678", PhoneNumbers.normalise("+880", "1712345678"));
        assertEquals("+8801712345678", PhoneNumbers.normalise("880", "1712-345 678"));
    }

    @Test
    void fullInternationalNumberIgnoresTheCountryCode() {
        assertEquals("+447700900123", PhoneNumbers.normalise("+880", "+44 7700 900123"));
    }

    @Test
    void anythingElseIsNotAPhoneNumber() {
        assertNull(PhoneNumbers.normalise(null));
        assertNull(PhoneNumbers.normalise("  "));
        assertNull(PhoneNumbers.normalise("017abc45678"));
        assertNull(PhoneNumbers.normalise("123"));
        assertNull(PhoneNumbers.normalise("+0123456789"));
    }
}
