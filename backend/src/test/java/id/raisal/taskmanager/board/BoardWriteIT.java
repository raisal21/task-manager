package id.raisal.taskmanager.board;

import static org.assertj.core.api.Assertions.assertThat;

import id.raisal.taskmanager.TestcontainersConfiguration;
import id.raisal.taskmanager.support.TestDatabase;
import id.raisal.taskmanager.support.TestApi;
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

/** The HTTP response of a write must show the row that the service committed. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class BoardWriteIT {

    private record Row(long id, String name, Instant createdAt) {
    }

    @LocalServerPort
    int port;

    @Autowired
    JdbcClient jdbc;

    @BeforeEach
    void cleanBoards() {
        TestDatabase.clean(jdbc);
    }

    @Test
    void responseMatchesCommittedBoard() {
        TestApi.Response response = new TestApi(port).postJson("/api/boards", "{\"name\":\"  Sprint 1  \"}");

        assertThat(response.status()).isEqualTo(201);
        List<Row> rows = jdbc.sql("SELECT id, name, created_at FROM boards")
                .query((rs, rowNumber) -> new Row(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getObject("created_at", OffsetDateTime.class).toInstant()))
                .list();
        assertThat(rows).hasSize(1);
        Row committed = rows.get(0);
        assertThat(response.<Number>json("$.id").longValue()).isEqualTo(committed.id());
        assertThat(response.<String>json("$.name")).isEqualTo("Sprint 1").isEqualTo(committed.name());
        assertThat(Instant.parse(response.json("$.createdAt"))).isEqualTo(committed.createdAt());
    }
}
