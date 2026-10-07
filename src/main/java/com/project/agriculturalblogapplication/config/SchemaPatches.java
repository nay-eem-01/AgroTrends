package com.project.agriculturalblogapplication.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Column changes that ddl-auto=update cannot make safely. Temporary: Flyway (roadmap 4.2) takes these over.
 *
 * <p>The type changes must run <em>before</em> Hibernate updates the schema ({@link SchemaPatchesOrder}): Hibernate
 * turns an oid column into text by itself, copying the large-object ids instead of the content they point to.
 * The search index needs the tables, so it is created after startup.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SchemaPatches implements ApplicationRunner {

    // Large-object (oid) columns from @Lob Strings: reads failed outside a transaction and SQL could not search them.
    static final List<String[]> OID_TO_TEXT = List.of(
            new String[]{"blogs", "content"},
            new String[]{"ai_answers", "ai_answer"});

    // varchar(255) columns that must hold longer text.
    static final List<String[]> VARCHAR_TO_TEXT = List.<String[]>of(
            new String[]{"ai_answers", "question"});

    static final String BLOG_SEARCH_INDEX = """
            CREATE INDEX IF NOT EXISTS blogs_search_idx ON blogs
            USING gin (to_tsvector('simple', coalesce(title, '') || ' ' || coalesce(content, '')))""";

    private final JdbcTemplate jdbcTemplate;

    /** Runs before Hibernate; does nothing on a new database (no tables yet) or once the columns are text. */
    @PostConstruct
    void convertColumns() {
        for (String[] column : OID_TO_TEXT) {
            if ("oid".equals(columnType(column[0], column[1]))) {
                oidToText(column[0], column[1]);
            }
        }
        for (String[] column : VARCHAR_TO_TEXT) {
            if ("character varying".equals(columnType(column[0], column[1]))) {
                jdbcTemplate.execute("ALTER TABLE " + column[0] + " ALTER COLUMN " + column[1] + " TYPE text");
                log.warn("Schema patch: {}.{} varchar -> text", column[0], column[1]);
            }
        }
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute(BLOG_SEARCH_INDEX);
    }

    private void oidToText(String table, String column) {
        // Keep the old object ids so their large objects can be freed after the copy.
        List<Long> objectIds = jdbcTemplate.queryForList(
                "SELECT " + column + " FROM " + table + " WHERE " + column + " IS NOT NULL", Long.class);
        jdbcTemplate.execute("ALTER TABLE " + table + " ALTER COLUMN " + column
                + " TYPE text USING convert_from(lo_get(" + column + "), 'UTF8')");
        objectIds.forEach(id -> jdbcTemplate.queryForList("SELECT lo_unlink(?)", Integer.class, id));
        log.warn("Schema patch: {}.{} large object -> text ({} rows)", table, column, objectIds.size());
    }

    /** Postgres data_type of a column in the current schema, or null when the table/column does not exist. */
    private String columnType(String table, String column) {
        List<String> types = jdbcTemplate.queryForList(
                "SELECT data_type FROM information_schema.columns WHERE table_schema = current_schema() "
                        + "AND table_name = ? AND column_name = ?", String.class, table, column);
        return types.isEmpty() ? null : types.get(0);
    }
}
