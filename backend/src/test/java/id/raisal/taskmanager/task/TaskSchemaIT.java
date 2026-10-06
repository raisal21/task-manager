package id.raisal.taskmanager.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import id.raisal.taskmanager.TestcontainersConfiguration;
import id.raisal.taskmanager.support.TestDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;

/** The schema rules of the tasks table, through SQL without the API. Each statement commits on its own. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class TaskSchemaIT {

    private static final long MISSING_BOARD_ID = 999_999L;

    @Autowired
    JdbcClient jdbc;

    long boardId;

    @BeforeEach
    void cleanAndAddABoard() {
        TestDatabase.clean(jdbc);
        boardId = jdbc.sql("INSERT INTO boards (name) VALUES ('Board') RETURNING id").query(Long.class).single();
    }

    @Test
    void rejectsTaskWithUnknownBoardOnDirectInsert() {
        assertThat(jdbc.sql("SELECT count(*) FROM boards WHERE id = ?").param(MISSING_BOARD_ID).query(Integer.class).single())
                .isZero();

        assertThatThrownBy(() -> insertTask(MISSING_BOARD_ID, "x", "TODO"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_tasks_board");
        assertThat(countTasks()).isZero();
    }

    @Test
    void restrictsDeleteOfBoardWithTasks() {
        insertTask(boardId, "Task", "TODO");

        assertThatThrownBy(() -> jdbc.sql("DELETE FROM boards WHERE id = ?").param(boardId).update())
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_tasks_board");

        assertThat(countTasks()).isEqualTo(1);
        assertThat(jdbc.sql("SELECT count(*) FROM boards WHERE id = ?").param(boardId).query(Integer.class).single()).isEqualTo(1);
    }

    @Test
    void deletesABoardWithoutTasks() {
        assertThat(jdbc.sql("DELETE FROM boards WHERE id = ?").param(boardId).update()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"BLOCKED", "todo", "In_Progress", "", "DONE "})
    void rejectsUnknownStatus(String status) {
        assertThatThrownBy(() -> insertTask(boardId, "Task", status))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("tasks_status_valid");
        assertThat(countTasks()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"TODO", "IN_PROGRESS", "DONE"})
    void acceptsEachKnownStatus(String status) {
        insertTask(boardId, "Task", status);

        assertThat(countTasks()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   ", "\t", " \t\n "})
    void rejectsBlankTitle(String title) {
        assertThatThrownBy(() -> insertTask(boardId, title, "TODO"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("tasks_title_not_blank");
        assertThat(countTasks()).isZero();
    }

    @Test
    void rejectsMissingTitleAndMissingBoard() {
        assertThatThrownBy(() -> insertTask(boardId, null, "TODO")).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertTask(null, "Task", "TODO")).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(countTasks()).isZero();
    }

    @Test
    void rejectsTitleAndDescriptionOverTheirLimits() {
        assertThatThrownBy(() -> insertTask(boardId, "x".repeat(201), "TODO")).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertWithDescription("y".repeat(2001))).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(countTasks()).isZero();
    }

    @Test
    void acceptsTitleAndDescriptionAtTheirLimits() {
        insertTask(boardId, "x".repeat(200), "TODO");
        insertWithDescription("y".repeat(2000));

        assertThat(countTasks()).isEqualTo(2);
    }

    @Test
    void usesTodoAndTheCurrentTimeAsDefaults() {
        jdbc.sql("INSERT INTO tasks (board_id, title) VALUES (?, 'Task')").param(boardId).update();

        var row = jdbc.sql("SELECT status, description, created_at <= now() AS created_ok, updated_at <= now() AS updated_ok FROM tasks")
                .query().singleRow();
        assertThat(row.get("status")).isEqualTo("TODO");
        assertThat(row.get("description")).isNull();
        assertThat(row.get("created_ok")).isEqualTo(true);
        assertThat(row.get("updated_ok")).isEqualTo(true);
    }

    @Test
    void hasTheIndexOnBoardId() {
        assertThat(jdbc.sql("SELECT indexdef FROM pg_indexes WHERE tablename = 'tasks' AND indexname = 'tasks_board_id_idx'")
                        .query(String.class)
                        .single())
                .contains("(board_id)");
    }

    private void insertTask(Long board, String title, String status) {
        jdbc.sql("INSERT INTO tasks (board_id, title, status) VALUES (?, ?, ?)").param(board).param(title).param(status).update();
    }

    private void insertWithDescription(String description) {
        jdbc.sql("INSERT INTO tasks (board_id, title, description) VALUES (?, 'Task', ?)").param(boardId).param(description).update();
    }

    private int countTasks() {
        return jdbc.sql("SELECT count(*) FROM tasks").query(Integer.class).single();
    }
}
