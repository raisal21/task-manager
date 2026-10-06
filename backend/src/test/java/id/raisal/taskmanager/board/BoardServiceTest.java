package id.raisal.taskmanager.board;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class BoardServiceTest {

    private static final Instant EARLY = Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant LATE = Instant.parse("2026-01-01T11:00:00Z");

    private final InMemoryBoardRepository repository = new InMemoryBoardRepository();
    private final BoardService service = new BoardService(repository);

    @Test
    void listsBoardsInCreationOrder() {
        repository.add(new Board(1L, "late", LATE));
        repository.add(new Board(3L, "early, second id", EARLY));
        repository.add(new Board(2L, "early, first id", EARLY));

        assertThat(service.listBoards())
                .extracting(Board::getName)
                .containsExactly("early, first id", "early, second id", "late");
    }

    @Test
    void listsNoBoardsWhenThereAreNone() {
        assertThat(service.listBoards()).isEmpty();
    }
}
