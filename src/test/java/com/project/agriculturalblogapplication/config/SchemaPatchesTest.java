package com.project.agriculturalblogapplication.config;

import com.project.agriculturalblogapplication.constatnt.AppTables;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    void theOptionalColumnsAreTheMappedOnes() {
        assertEquals(List.of(
                "ALTER TABLE users ALTER COLUMN " + AppTables.UserTable.PASSWORD + " DROP NOT NULL",
                "ALTER TABLE users ALTER COLUMN " + AppTables.UserTable.MOBILE_NUMBER + " DROP NOT NULL",
                "ALTER TABLE users ALTER COLUMN " + AppTables.UserTable.EMAIL + " DROP NOT NULL"),
                SchemaPatches.DROP_NOT_NULL);
    }

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

    @Test
    void answerForeignKeysCascadeOnExistingDatabases() {
        // Question.answers and Answer.replies were removed; deleting a question or answer now relies on these.
        List<String> keys = SchemaPatches.CASCADE_ON_DELETE.stream().map(fk -> String.join(".", fk)).toList();

        assertTrue(keys.contains("answers.question_id.questions"));
        assertTrue(keys.contains("answers.answer_id.answers"));
    }

    @Test
    void aForeignKeyWithoutCascadeIsRecreatedAndACascadingOneIsLeftAlone() {
        when(jdbc.queryForList(anyString(), eq("comment"), eq("blog_id")))
                .thenReturn(List.of(Map.of("conname", "fk_comment_blog", "action", "a")));
        when(jdbc.queryForList(anyString(), eq("comment"), eq("parent_comment_id")))
                .thenReturn(List.of(Map.of("conname", "fk_comment_parent", "action", "c")));

        patches.cascadeOnDelete("comment", "blog_id", "blogs");
        patches.cascadeOnDelete("comment", "parent_comment_id", "comment");

        verify(jdbc).execute("ALTER TABLE comment DROP CONSTRAINT fk_comment_blog, ADD CONSTRAINT fk_comment_blog"
                + " FOREIGN KEY (blog_id) REFERENCES blogs(id) ON DELETE CASCADE");
        verify(jdbc, never()).execute(contains("fk_comment_parent"));
    }
}
