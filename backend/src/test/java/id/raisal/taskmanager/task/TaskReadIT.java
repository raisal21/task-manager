package id.raisal.taskmanager.task;

import static org.assertj.core.api.Assertions.assertThat;

import id.raisal.taskmanager.TestcontainersConfiguration;
import id.raisal.taskmanager.support.TestApi;
import id.raisal.taskmanager.support.TestDatabase;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

/** ver_r10 on a real HTTP port with PostgreSQL: the list, its order, the status filter, and the errors. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class TaskReadIT {

    private static final long MISSING_BOARD_ID = 999_999L;

    @LocalServerPort
    int port;

    @Autowired
    JdbcClient jdbc;

    TestApi api;
    long boardId;
    long emptyBoardId;

    @BeforeEach
    void addBoardsAndTasks() {
        TestDatabase.clean(jdbc);
        api = new TestApi(port);
        boardId = insertBoard("With tasks");
        emptyBoardId = insertBoard("Empty");
        long other = insertBoard("Other board");
        // Inserted out of order, so that the order of the list comes from created_at and id (A5).
        insertTask(boardId, "late done", "DONE", "2026-01-01T11:00:00Z");
        insertTask(boardId, "early todo", "TODO", "2026-01-01T10:00:00Z");
        insertTask(boardId, "early done", "DONE", "2026-01-01T10:00:00Z");
        insertTask(other, "other board", "DONE", "2026-01-01T09:00:00Z");
    }

    @Test
    void listsAllTasksOfTheBoardInOrder() {
        TestApi.Response response = api.get("/api/boards/" + boardId + "/tasks");

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.<List<String>>json("$[*].title")).containsExactly("early todo", "early done", "late done");
        assertThat(response.<List<Number>>json("$[*].boardId")).allSatisfy(id -> assertThat(id.longValue()).isEqualTo(boardId));
    }

    @Test
    void filtersByStatus() {
        TestApi.Response done = api.get("/api/boards/" + boardId + "/tasks?status=DONE");
        TestApi.Response inProgress = api.get("/api/boards/" + boardId + "/tasks?status=IN_PROGRESS");

        assertThat(done.status()).isEqualTo(200);
        assertThat(done.<List<String>>json("$[*].title")).containsExactly("early done", "late done");
        assertThat(inProgress.status()).isEqualTo(200);
        assertThat(inProgress.body()).isEqualTo("[]");
    }

    @Test
    void sendsAnEmptyArrayForABoardWithoutTasks() {
        TestApi.Response response = api.get("/api/boards/" + emptyBoardId + "/tasks");

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("[]");
    }

    @Test
    void sends404ForAMissingBoardAndKeepsTheSameBody() {
        assertThat(jdbc.sql("SELECT count(*) FROM boards WHERE id = ?").param(MISSING_BOARD_ID).query(Integer.class).single()).isZero();

        TestApi.Response response = api.get("/api/boards/" + MISSING_BOARD_ID + "/tasks");

        assertThat(response.status()).isEqualTo(404);
        assertThat(response.<String>json("$.code")).isEqualTo("NOT_FOUND");
        assertThat(response.<String>json("$.instance")).isEqualTo("/api/boards/" + MISSING_BOARD_ID + "/tasks");
    }

    @Test
    void sends400ForAnUnknownStatusAndForAWrongPathType() {
        TestApi.Response status = api.get("/api/boards/" + boardId + "/tasks?status=BLOCKED");
        TestApi.Response pathType = api.get("/api/boards/abc/tasks");

        assertThat(status.status()).isEqualTo(400);
        assertThat(status.<String>json("$.field")).isEqualTo("status");
        assertThat(pathType.status()).isEqualTo(400);
        assertThat(pathType.<String>json("$.field")).isEqualTo("boardId");
    }

    @Test
    void showsTheTaskThatThePostAdded() {
        TestApi.Response created = api.postJson("/api/boards/" + emptyBoardId + "/tasks", "{\"title\":\"New\"}");
        TestApi.Response listed = api.get("/api/boards/" + emptyBoardId + "/tasks");

        assertThat(created.status()).isEqualTo(201);
        assertThat(listed.<List<String>>json("$[*].title")).containsExactly("New");
        assertThat(listed.<Number>json("$[0].id").longValue()).isEqualTo(created.<Number>json("$.id").longValue());
        assertThat(listed.<String>json("$[0].status")).isEqualTo("TODO");
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
