package id.raisal.taskmanager.task;

import static org.assertj.core.api.Assertions.assertThat;

import id.raisal.taskmanager.TestcontainersConfiguration;
import id.raisal.taskmanager.support.TestApi;
import id.raisal.taskmanager.support.TestDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

/** The delete operations on a real HTTP port with PostgreSQL. The checks read the rows through a different connection. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class DeleteIT {

    @LocalServerPort
    int port;

    @Autowired
    JdbcClient jdbc;

    TestApi api;

    @BeforeEach
    void clean() {
        TestDatabase.clean(jdbc);
        api = new TestApi(port);
    }

    @Test
    void commitsTaskDelete() {
        long boardId = insertBoard("Board");
        long keep = insertTask(boardId, "Keep");
        long remove = insertTask(boardId, "Remove");

        TestApi.Response first = api.delete("/api/tasks/" + remove);
        TestApi.Response second = api.delete("/api/tasks/" + remove);

        // R13: 204 without a body, then 404.
        assertThat(first.status()).isEqualTo(204);
        assertThat(first.body()).isEmpty();
        assertThat(second.status()).isEqualTo(404);
        assertThat(second.<String>json("$.code")).isEqualTo("NOT_FOUND");
        assertThat(second.<String>json("$.detail")).isEqualTo("There is no task with ID " + remove + ".");
        assertThat(taskIds()).containsExactly(keep);
        assertThat(count("boards")).isEqualTo(1);
    }

    @Test
    void commitsTaskAndEmptyBoardDeletes() {
        long withTask = insertBoard("With a task");
        long empty = insertBoard("Empty");
        long task = insertTask(withTask, "Task");

        // R14: an empty board is deleted (204), a second delete gives 404.
        assertThat(api.delete("/api/boards/" + empty).status()).isEqualTo(204);
        assertThat(api.delete("/api/boards/" + empty).status()).isEqualTo(404);
        assertThat(boardIds()).containsExactly(withTask);

        // R4: a board with a task stays (409), with its task. This is the same 409 for all statuses.
        TestApi.Response blocked = api.delete("/api/boards/" + withTask);
        assertThat(blocked.status()).isEqualTo(409);
        assertThat(blocked.<String>json("$.code")).isEqualTo("BOARD_NOT_EMPTY");
        assertThat(boardIds()).containsExactly(withTask);
        assertThat(taskIds()).containsExactly(task);

        // After the last task is deleted, the board can be deleted.
        assertThat(api.delete("/api/tasks/" + task).status()).isEqualTo(204);
        assertThat(api.delete("/api/boards/" + withTask).status()).isEqualTo(204);
        assertThat(boardIds()).isEmpty();
        assertThat(taskIds()).isEmpty();
    }

    @Test
    void blocksTheBoardDeleteForATaskInAnyStatus() {
        long board = insertBoard("Board");
        for (String status : new String[] {"TODO", "IN_PROGRESS", "DONE"}) {
            jdbc.sql("INSERT INTO tasks (board_id, title, status) VALUES (?, ?, ?)").param(board).param(status).param(status).update();
            assertThat(api.delete("/api/boards/" + board).status()).isEqualTo(409);
            jdbc.sql("DELETE FROM tasks").update();
        }
        assertThat(boardIds()).containsExactly(board);
    }

    @Test
    void sends404ForABoardIdThatIsNotInTheDatabaseAnd400ForANonNumber() {
        assertThat(count("boards")).isZero();

        TestApi.Response missing = api.delete("/api/boards/999999");
        TestApi.Response notANumber = api.delete("/api/boards/abc");

        assertThat(missing.status()).isEqualTo(404);
        assertThat(missing.<String>json("$.detail")).isEqualTo("There is no board with ID 999999.");
        assertThat(notANumber.status()).isEqualTo(400);
        assertThat(notANumber.<String>json("$.field")).isEqualTo("boardId");
    }

    @Test
    void sends404ForATaskIdThatIsNotInTheDatabase() {
        assertThat(count("tasks")).isZero();

        TestApi.Response response = api.delete("/api/tasks/999999");

        assertThat(response.status()).isEqualTo(404);
        assertThat(response.<String>json("$.instance")).isEqualTo("/api/tasks/999999");
    }

    @Test
    void sends400ForATaskIdThatIsNotANumber() {
        TestApi.Response response = api.delete("/api/tasks/abc");

        assertThat(response.status()).isEqualTo(400);
        assertThat(response.<String>json("$.field")).isEqualTo("taskId");
    }

    private long insertBoard(String name) {
        return jdbc.sql("INSERT INTO boards (name) VALUES (?) RETURNING id").param(name).query(Long.class).single();
    }

    private long insertTask(long boardId, String title) {
        return jdbc.sql("INSERT INTO tasks (board_id, title) VALUES (?, ?) RETURNING id").param(boardId).param(title).query(Long.class).single();
    }

    private java.util.List<Long> boardIds() {
        return jdbc.sql("SELECT id FROM boards ORDER BY id").query(Long.class).list();
    }

    private java.util.List<Long> taskIds() {
        return jdbc.sql("SELECT id FROM tasks ORDER BY id").query(Long.class).list();
    }

    private int count(String table) {
        return jdbc.sql("SELECT count(*) FROM " + table).query(Integer.class).single();
    }
}
