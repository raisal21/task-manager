package id.raisal.taskmanager.task;

import static org.assertj.core.api.Assertions.assertThat;

import id.raisal.taskmanager.TestcontainersConfiguration;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

/** What the unit tests with a fake cannot show: the query, the mapping, and the order in PostgreSQL. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class TaskRepositoryIT {

    @Autowired
    TaskRepository repository;

    @Autowired
    JdbcClient jdbc;

    @Test
    void findsOnlyTasksOfTheBoard() {
        long first = insertBoard("First");
        long second = insertBoard("Second");
        insertTask(first, "in first", "TODO", "2026-01-01T10:00:00Z");
        insertTask(second, "in second", "DONE", "2026-01-01T10:00:00Z");

        assertThat(repository.findByBoardIdOrderByCreatedAtAscIdAsc(first)).extracting(Task::getTitle).containsExactly("in first");
        assertThat(repository.findByBoardIdOrderByCreatedAtAscIdAsc(second)).extracting(Task::getTitle).containsExactly("in second");
        assertThat(repository.findByBoardIdOrderByCreatedAtAscIdAsc(999_999L)).isEmpty();
    }

    @Test
    void ordersTasksByCreationTimeThenId() {
        long board = insertBoard("Board");
        // Inserted out of order. Ids follow the insert order: late = 1, early (first) = 2, early (second) = 3.
        insertTask(board, "late", "TODO", "2026-01-01T11:00:00Z");
        insertTask(board, "early, first id", "TODO", "2026-01-01T10:00:00Z");
        insertTask(board, "early, second id", "TODO", "2026-01-01T10:00:00Z");

        assertThat(repository.findByBoardIdOrderByCreatedAtAscIdAsc(board))
                .extracting(Task::getTitle)
                .containsExactly("early, first id", "early, second id", "late");
    }

    @Test
    void readsTheStoredColumnsOfATask() {
        long board = insertBoard("Board");
        jdbc.sql("""
                INSERT INTO tasks (board_id, title, description, status, created_at, updated_at)
                VALUES (?, 'Write', 'Details', 'IN_PROGRESS', ?, ?)
                """)
                .param(board)
                .param(OffsetDateTime.parse("2026-01-01T10:00:00Z"))
                .param(OffsetDateTime.parse("2026-01-02T10:00:00Z"))
                .update();

        Task task = repository.findByBoardIdOrderByCreatedAtAscIdAsc(board).get(0);

        assertThat(task.getId()).isNotNull();
        assertThat(task.getBoardId()).isEqualTo(board);
        assertThat(task.getTitle()).isEqualTo("Write");
        assertThat(task.getDescription()).isEqualTo("Details");
        assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(task.getCreatedAt()).isEqualTo("2026-01-01T10:00:00Z");
        assertThat(task.getUpdatedAt()).isEqualTo("2026-01-02T10:00:00Z");
    }

    private long insertBoard(String name) {
        return jdbc.sql("INSERT INTO boards (name) VALUES (?) RETURNING id").param(name).query(Long.class).single();
    }

    private void insertTask(long board, String title, String status, String createdAt) {
        jdbc.sql("INSERT INTO tasks (board_id, title, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?)")
                .param(board)
                .param(title)
                .param(status)
                .param(OffsetDateTime.parse(createdAt))
                .param(OffsetDateTime.parse(createdAt))
                .update();
    }
}
