package id.raisal.taskmanager.board;

import static org.assertj.core.api.Assertions.assertThat;

import id.raisal.taskmanager.TestcontainersConfiguration;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class BoardRepositoryIT {

    @Autowired
    BoardRepository repository;

    @Autowired
    JdbcClient jdbc;

    @Test
    void ordersBoardsByCreationTimeThenId() {
        // Inserted out of order. Ids follow the insert order: late = 1, early (first) = 2, early (second) = 3.
        insert("late", "2026-01-01T11:00:00Z");
        insert("early, first id", "2026-01-01T10:00:00Z");
        insert("early, second id", "2026-01-01T10:00:00Z");

        assertThat(repository.findAllByOrderByCreatedAtAscIdAsc())
                .extracting(Board::getName)
                .containsExactly("early, first id", "early, second id", "late");
    }

    @Test
    void readsTheStoredColumnsOfABoard() {
        insert("Sprint 1", "2026-01-01T10:00:00Z");

        Board board = repository.findAllByOrderByCreatedAtAscIdAsc().get(0);

        assertThat(board.getId()).isNotNull();
        assertThat(board.getName()).isEqualTo("Sprint 1");
        assertThat(board.getCreatedAt()).isEqualTo("2026-01-01T10:00:00Z");
    }

    private void insert(String name, String createdAt) {
        jdbc.sql("INSERT INTO boards (name, created_at) VALUES (?, ?)")
                .param(name)
                .param(OffsetDateTime.parse(createdAt))
                .update();
    }
}
