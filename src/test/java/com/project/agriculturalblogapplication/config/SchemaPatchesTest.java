package com.project.agriculturalblogapplication.config;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SchemaPatchesTest {

    private static final String TYPE_QUERY = "SELECT data_type FROM information_schema.columns WHERE table_schema = current_schema() "
            + "AND table_name = ? AND column_name = ?";

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final SchemaPatches patches = new SchemaPatches(jdbc);

    @Test
    void largeObjectColumnsAreCopiedAsTextAndTheObjectsFreed() {
        when(jdbc.queryForList(TYPE_QUERY, String.class, "blogs", "content")).thenReturn(List.of("oid"));
        when(jdbc.queryForList(TYPE_QUERY, String.class, "ai_answers", "ai_answer")).thenReturn(List.of("text"));
        when(jdbc.queryForList(TYPE_QUERY, String.class, "ai_answers", "question")).thenReturn(List.of("text"));
        when(jdbc.queryForList("SELECT content FROM blogs WHERE content IS NOT NULL", Long.class)).thenReturn(List.of(24591L));

        patches.convertColumns();

        // Regression: letting Hibernate change the type copied the object ids ("24591") instead of the text.
        verify(jdbc).execute("ALTER TABLE blogs ALTER COLUMN content TYPE text USING convert_from(lo_get(content), 'UTF8')");
        verify(jdbc).queryForList("SELECT lo_unlink(?)", Integer.class, 24591L);
        verify(jdbc, never()).execute(contains("ai_answer TYPE"));
    }

    @Test
    void aNewDatabaseOrAnAlreadyPatchedOneIsLeftAlone() {
        when(jdbc.queryForList(eq(TYPE_QUERY), eq(String.class), anyString(), anyString())).thenReturn(List.of());

        patches.convertColumns();

        verify(jdbc, never()).execute(anyString());
    }

    @Test
    void longQuestionsBecomeText() {
        when(jdbc.queryForList(eq(TYPE_QUERY), eq(String.class), anyString(), anyString())).thenReturn(List.of("text"));
        when(jdbc.queryForList(TYPE_QUERY, String.class, "ai_answers", "question")).thenReturn(List.of("character varying"));

        patches.convertColumns();

        verify(jdbc).execute("ALTER TABLE ai_answers ALTER COLUMN question TYPE text");
    }
}
