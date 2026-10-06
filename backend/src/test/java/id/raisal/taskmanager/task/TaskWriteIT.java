package id.raisal.taskmanager.task;

import static org.assertj.core.api.Assertions.assertThat;

import id.raisal.taskmanager.TestcontainersConfiguration;
import id.raisal.taskmanager.support.TestApi;
import id.raisal.taskmanager.support.TestDatabase;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

/** The HTTP response of a task write must show the row that the service committed (P5). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class TaskWriteIT {

    private record Row(long id, long boardId, String title, String description, String status, Instant createdAt, Instant updatedAt) {
    }

    @LocalServerPort
    int port;

    @Autowired
    JdbcClient jdbc;

    long boardId;

    @BeforeEach
    void cleanAndAddABoard() {
        TestDatabase.clean(jdbc);
        boardId = jdbc.sql("INSERT INTO boards (name) VALUES ('Board') RETURNING id").query(Long.class).single();
    }

    @Test
    void createResponseMatchesCommittedTask() {
        TestApi.Response response = new TestApi(port)
                .postJson("/api/boards/" + boardId + "/tasks", "{\"title\":\"  Write tests  \",\"description\":\"   \"}");

        assertThat(response.status()).isEqualTo(201);
        List<Row> rows = jdbc.sql("SELECT id, board_id, title, description, status, created_at, updated_at FROM tasks")
                .query((rs, rowNumber) -> new Row(
                        rs.getLong("id"),
                        rs.getLong("board_id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getString("status"),
                        rs.getObject("created_at", OffsetDateTime.class).toInstant(),
                        rs.getObject("updated_at", OffsetDateTime.class).toInstant()))
                .list();
        assertThat(rows).hasSize(1);
        Row committed = rows.get(0);

        assertThat(committed.title()).isEqualTo("Write tests");
        assertThat(committed.description()).isNull();
        assertThat(committed.status()).isEqualTo("TODO");
        // R2 and A12: one Clock instant for both times.
        assertThat(committed.updatedAt()).isEqualTo(committed.createdAt());

        assertThat(response.<Number>json("$.id").longValue()).isEqualTo(committed.id());
        assertThat(response.<Number>json("$.boardId").longValue()).isEqualTo(boardId).isEqualTo(committed.boardId());
        assertThat(response.<String>json("$.title")).isEqualTo(committed.title());
        assertThat(response.<Object>json("$.description")).isNull();
        assertThat(response.<String>json("$.status")).isEqualTo(committed.status());
        assertThat(Instant.parse(response.json("$.createdAt"))).isEqualTo(committed.createdAt());
        assertThat(Instant.parse(response.json("$.updatedAt"))).isEqualTo(committed.updatedAt());
    }
}
