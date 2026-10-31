package com.project.agriculturalblogapplication.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

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

    // Foreign keys created without ON DELETE: {table, column, referenced table}. @OnDelete covers new databases.
    static final List<String[]> CASCADE_ON_DELETE = List.of(
            new String[]{"comment", "blog_id", "blogs"},
            new String[]{"comment", "parent_comment_id", "comment"},
            new String[]{"answers", "question_id", "questions"},
            new String[]{"answers", "answer_id", "answers"});

    // Columns that became optional: Google accounts have no password or mobile number, phone accounts no e-mail.
    // Idempotent.
    static final List<String> DROP_NOT_NULL = List.of(
            "ALTER TABLE users ALTER COLUMN user_password DROP NOT NULL",
            "ALTER TABLE users ALTER COLUMN mobile_number DROP NOT NULL",
            "ALTER TABLE users ALTER COLUMN user_email DROP NOT NULL");

    static final String PUBLISHED_AT_BACKFILL =
            "UPDATE blogs SET published_at = creation_date WHERE status = 'PUBLISHED' AND published_at IS NULL";

    static final String BLOG_SEARCH_INDEX = """
            CREATE INDEX IF NOT EXISTS blogs_search_idx ON blogs
            USING gin (to_tsvector('simple', coalesce(title, '') || ' ' || coalesce(content, '')))""";

    // Authors had their own ids; now an author's id is its user's id (@MapsId). Remaps authors and every foreign key
    // pointing at them in one statement, so it either all happens or none of it. Negative ids first: a direct update
    // could collide with an id not yet moved. The old user_id column stays (an old instance may still read it).
    static final String AUTHOR_IDS_TO_USER_IDS = """
            DO $$
            DECLARE fk record;
            BEGIN
              CREATE TEMP TABLE author_remap ON COMMIT DROP AS
                SELECT id AS old_id, user_id AS new_id FROM authors WHERE user_id IS NOT NULL AND id <> user_id;
              CREATE TEMP TABLE author_fks ON COMMIT DROP AS
                SELECT c.conrelid::regclass::text AS tbl, c.conname, pg_get_constraintdef(c.oid) AS def, a.attname AS col
                FROM pg_constraint c JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = c.conkey[1]
                WHERE c.contype = 'f' AND c.confrelid = 'authors'::regclass;
              FOR fk IN SELECT * FROM author_fks LOOP
                EXECUTE format('ALTER TABLE %s DROP CONSTRAINT %I', fk.tbl, fk.conname);
                EXECUTE format('UPDATE %s t SET %I = -r.new_id FROM author_remap r WHERE t.%I = r.old_id', fk.tbl, fk.col, fk.col);
                EXECUTE format('UPDATE %s SET %I = -%I WHERE %I < 0', fk.tbl, fk.col, fk.col, fk.col);
              END LOOP;
              UPDATE authors a SET id = -r.new_id FROM author_remap r WHERE a.id = r.old_id;
              UPDATE authors SET id = -id WHERE id < 0;
              FOR fk IN SELECT * FROM author_fks LOOP
                EXECUTE format('ALTER TABLE %s ADD CONSTRAINT %I %s', fk.tbl, fk.conname, fk.def);
              END LOOP;
            END $$""";

    static final String AUTHORS_TO_REMAP = "SELECT count(*) FROM authors WHERE user_id IS NOT NULL AND id <> user_id";

    // Users from before every account got a profile at sign-up.
    static final String AUTHOR_BACKFILL =
            "INSERT INTO authors (id) SELECT u.id FROM users u WHERE NOT EXISTS (SELECT 1 FROM authors a WHERE a.id = u.id)";

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
        if (columnType("authors", "user_id") != null) {
            Long moved = jdbcTemplate.queryForObject(AUTHORS_TO_REMAP, Long.class);
            if (moved != null && moved > 0) {
                jdbcTemplate.execute(AUTHOR_IDS_TO_USER_IDS);
                log.warn("Schema patch: {} authors now use their user's id", moved);
            }
        }
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute(BLOG_SEARCH_INDEX);
        CASCADE_ON_DELETE.forEach(fk -> cascadeOnDelete(fk[0], fk[1], fk[2]));
        DROP_NOT_NULL.forEach(jdbcTemplate::execute);
        int profiles = jdbcTemplate.update(AUTHOR_BACKFILL);
        if (profiles > 0) {
            log.warn("Schema patch: {} users got an author profile", profiles);
        }
        // Posts published before drafts existed have no published_at; feeds sort by it.
        int stamped = jdbcTemplate.update(PUBLISHED_AT_BACKFILL);
        if (stamped > 0) {
            log.warn("Schema patch: {} published blogs got published_at = creation_date", stamped);
        }
    }

    /** Recreates a foreign key with ON DELETE CASCADE unless it already has it (confdeltype 'c'). */
    void cascadeOnDelete(String table, String column, String referencedTable) {
        List<Map<String, Object>> keys = jdbcTemplate.queryForList("""
                SELECT c.conname, c.confdeltype::text AS action FROM pg_constraint c
                JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = ANY (c.conkey)
                WHERE c.contype = 'f' AND c.conrelid = to_regclass(?) AND a.attname = ?""", table, column);
        for (Map<String, Object> key : keys) {
            if (!"c".equals(key.get("action"))) {
                String name = (String) key.get("conname");
                jdbcTemplate.execute("ALTER TABLE " + table + " DROP CONSTRAINT " + name + ", ADD CONSTRAINT " + name
                        + " FOREIGN KEY (" + column + ") REFERENCES " + referencedTable + "(id) ON DELETE CASCADE");
                log.warn("Schema patch: {}.{} foreign key -> ON DELETE CASCADE", table, column);
            }
        }
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
