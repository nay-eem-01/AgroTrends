package com.project.agriculturalblogapplication.util;

import com.project.agriculturalblogapplication.enums.AscOrDescType;
import com.project.agriculturalblogapplication.payloads.PaginationArgs;
import org.junit.jupiter.api.Test;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CommonUtilsTest {

    @Test
    void pageSizeIsClampedAndPageNumberIsNeverNegative() {
        Pageable huge = CommonUtils.getPageable(new PaginationArgs(-3, 100_000, "creationDate", AscOrDescType.desc));
        Pageable zero = CommonUtils.getPageable(new PaginationArgs(2, 0, null, AscOrDescType.asc));

        assertEquals(0, huge.getPageNumber());
        assertEquals(100, huge.getPageSize());
        assertEquals(2, zero.getPageNumber());
        assertEquals(1, zero.getPageSize());
    }

    @Test
    void sortingByAFieldOutsideTheAllowlistIsA400() {
        PaginationArgs args = new PaginationArgs(0, 20, "author.user.password", AscOrDescType.asc);

        ApplicationException e = assertThrows(ApplicationException.class,
                () -> CommonUtils.getPageable(args, Set.of("creationDate", "title"), "en"));

        assertEquals(HttpStatus.BAD_REQUEST, e.getHttpStatus());
    }

    @Test
    void allowlistedAndEmptySortFieldsPass() {
        Pageable byTitle = CommonUtils.getPageable(new PaginationArgs(0, 20, "title", AscOrDescType.asc), Set.of("title"), "en");
        Pageable unsorted = CommonUtils.getPageable(new PaginationArgs(0, 20, "", AscOrDescType.asc), Set.of("title"), "en");

        assertEquals(Sort.by("title").ascending(), byTitle.getSort());
        assertEquals(Sort.unsorted(), unsorted.getSort());
    }
}
