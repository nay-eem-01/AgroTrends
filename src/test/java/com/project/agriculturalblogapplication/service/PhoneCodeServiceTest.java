package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.entities.PhoneCode;
import com.project.agriculturalblogapplication.entities.PhoneCode.Purpose;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.repositories.PhoneCodeRepository;
import com.project.agriculturalblogapplication.security.service.AttemptLimiter;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** SMS codes: 6 digits, hashed, single use, dead after 5 wrong tries or 10 minutes. */
class PhoneCodeServiceTest {

    private final PhoneCodeRepository repository = mock(PhoneCodeRepository.class);
    private final AttemptLimiter limiter = new AttemptLimiter();
    private final PhoneCodeService service = new PhoneCodeService(repository, limiter);

    private PhoneCode issued(String[] code) {
        code[0] = service.issue(4L, Purpose.VERIFY_PHONE);
        ArgumentCaptor<PhoneCode> captor = ArgumentCaptor.forClass(PhoneCode.class);
        verify(repository).save(captor.capture());
        PhoneCode stored = captor.getValue();
        when(repository.findByUserIdAndPurpose(4L, Purpose.VERIFY_PHONE)).thenReturn(Optional.of(stored));
        return stored;
    }

    private static String wrong(String code) {
        return code.equals("000000") ? "000001" : "000000";
    }

    @Test
    void theCodeIsSixDigitsAndOnlyItsHashIsStored() {
        String[] code = new String[1];
        PhoneCode stored = issued(code);

        assertTrue(code[0].matches("\\d{6}"));
        assertNotEquals(code[0], stored.getCodeHash());
        assertEquals(64, stored.getCodeHash().length());
    }

    @Test
    void theRightCodeIsUsedUp() {
        String[] code = new String[1];
        issued(code);

        assertDoesNotThrow(() -> service.consume(4L, Purpose.VERIFY_PHONE, code[0], "en"));
        // once at issue (replacing an older code), once on use
        verify(repository, times(2)).deleteAllByUserIdAndPurpose(4L, Purpose.VERIFY_PHONE);
    }

    @Test
    void theFifthWrongTryKillsTheCode() {
        String[] code = new String[1];
        PhoneCode stored = issued(code);

        for (int i = 1; i < PhoneCode.MAX_ATTEMPTS; i++) {
            assertThrows(ApplicationException.class, () -> service.consume(4L, Purpose.VERIFY_PHONE, wrong(code[0]), "en"));
            assertEquals(i, stored.getFailedAttempts());
        }
        verify(repository, times(1)).deleteAllByUserIdAndPurpose(4L, Purpose.VERIFY_PHONE);

        assertThrows(ApplicationException.class, () -> service.consume(4L, Purpose.VERIFY_PHONE, wrong(code[0]), "en"));
        verify(repository, times(2)).deleteAllByUserIdAndPurpose(4L, Purpose.VERIFY_PHONE);
    }

    @Test
    void anExpiredCodeIsRefusedEvenWhenRight() {
        String[] code = new String[1];
        PhoneCode stored = issued(code);
        stored.setExpiryDate(Instant.now().minusSeconds(1));

        ApplicationException e = assertThrows(ApplicationException.class,
                () -> service.consume(4L, Purpose.VERIFY_PHONE, code[0], "en"));
        assertEquals(HttpStatus.BAD_REQUEST, e.getHttpStatus());
    }

    @Test
    void noAccountIsTheSame400() {
        ApplicationException e = assertThrows(ApplicationException.class,
                () -> service.consume(null, Purpose.VERIFY_PHONE, "123456", "en"));
        assertEquals(HttpStatus.BAD_REQUEST, e.getHttpStatus());
        verify(repository, never()).findByUserIdAndPurpose(any(), any());
    }

    @Test
    void theFourthRequestForANumberWithinAnHourIs429() {
        for (int i = 0; i < 3; i++) {
            service.limitRequests(Purpose.VERIFY_PHONE, "+8801712345678", "10.0.0." + i, "en");
        }
        ApplicationException e = assertThrows(ApplicationException.class,
                () -> service.limitRequests(Purpose.VERIFY_PHONE, "+8801712345678", "10.0.0.9", "en"));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, e.getHttpStatus());
    }
}
