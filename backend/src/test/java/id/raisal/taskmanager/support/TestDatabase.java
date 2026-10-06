package id.raisal.taskmanager.support;

import org.springframework.jdbc.core.simple.JdbcClient;

/** Helpers for integration tests that commit data. Tasks go first, because of the foreign key. */
public final class TestDatabase {

    private TestDatabase() {
    }

    public static void clean(JdbcClient jdbc) {
        jdbc.sql("DELETE FROM tasks").update();
        jdbc.sql("DELETE FROM boards").update();
    }
}
