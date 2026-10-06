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

    private java.util.List<Long> taskIds() {
        return jdbc.sql("SELECT id FROM tasks ORDER BY id").query(Long.class).list();
    }

    private int count(String table) {
        return jdbc.sql("SELECT count(*) FROM " + table).query(Integer.class).single();
    }
}
