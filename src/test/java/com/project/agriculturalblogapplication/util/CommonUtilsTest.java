package com.project.agriculturalblogapplication.util;

import com.project.agriculturalblogapplication.enums.AscOrDescType;
import com.project.agriculturalblogapplication.payloads.PaginationArgs;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
