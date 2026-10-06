package id.raisal.taskmanager.board;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import id.raisal.taskmanager.common.error.BoardNotEmptyException;
import id.raisal.taskmanager.common.error.NotFoundException;
import id.raisal.taskmanager.common.error.ValidationException;
import id.raisal.taskmanager.task.InMemoryTaskRepository;
import id.raisal.taskmanager.task.Task;
import id.raisal.taskmanager.task.TaskStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class BoardServiceTest {

    private static final Instant EARLY = Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant LATE = Instant.parse("2026-01-01T11:00:00Z");
    private static final Instant NOW = Instant.parse("2026-02-03T04:05:06Z");

    private final InMemoryBoardRepository repository = new InMemoryBoardRepository();
    private final InMemoryTaskRepository tasks = new InMemoryTaskRepository();
    private final BoardService service = new BoardService(repository, tasks, Clock.fixed(NOW, ZoneOffset.UTC));

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

    @Test
    void createsBoardWithTrimmedName() {
        Board board = service.addBoard("  Sprint 1 \t");

        assertThat(board.getId()).isNotNull();
        assertThat(board.getName()).isEqualTo("Sprint 1");
        assertThat(board.getCreatedAt()).isEqualTo(NOW);
        assertThat(repository.all()).containsExactly(board);
    }

    @Test
    void storesTheCreationTimeWithMicrosecondPrecision() {
        BoardService nanoService =
                new BoardService(repository, tasks, Clock.fixed(Instant.parse("2026-02-03T04:05:06.123456789Z"), ZoneOffset.UTC));

        assertThat(nanoService.addBoard("Sprint 1").getCreatedAt()).isEqualTo(Instant.parse("2026-02-03T04:05:06.123456Z"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void rejectsEmptyBoardName(String name) {
        assertRejectedAsName(name, "Name is required.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"   ", "\t", " \t \n "})
    void rejectsWhitespaceOnlyBoardName(String name) {
        assertRejectedAsName(name, "Name is required.");
    }

    @Test
    void rejectsBoardNameLongerThan100Characters() {
        assertRejectedAsName("x".repeat(101), "Name must have 100 characters or fewer.");
    }

    @Test
    void acceptsBoardNameWithExactly100Characters() {
        assertThat(service.addBoard("x".repeat(100)).getName()).hasSize(100);
    }

    @Test
    void countsCharactersAndNotUtf16Units() {
        // 100 characters that each use two UTF-16 units. The database accepts them.
        String hundredEmoji = "😀".repeat(100);

        assertThat(service.addBoard(hundredEmoji).getName()).isEqualTo(hundredEmoji);
        assertRejectedAsName("😀".repeat(101), "Name must have 100 characters or fewer.");
    }

    // --- deleteBoard ---

    @Test
    void deletesEmptyBoard() {
        Board keep = new Board(1L, "keep", EARLY);
        repository.add(keep);
        repository.add(new Board(2L, "remove", EARLY));

        service.deleteBoard(2);

        assertThat(repository.all()).containsExactly(keep);
    }

    @Test
    void rejectsDeleteOfBoardWithTasks() {
        Board board = new Board(1L, "has a task", EARLY);
        repository.add(board);
        tasks.add(new Task(5L, 1L, "Task", null, TaskStatus.DONE, EARLY, EARLY));

        // R4: the rule is in the service. A task in any status blocks the delete.
        assertThatThrownBy(() -> service.deleteBoard(1))
                .isInstanceOf(BoardNotEmptyException.class)
                .hasMessage("Board 1 has tasks. Delete its tasks first.");
        assertThat(repository.all()).containsExactly(board);
        assertThat(tasks.all()).hasSize(1);
    }

    @Test
    void rejectsDeleteOfMissingBoard() {
        Board keep = new Board(1L, "keep", EARLY);
        repository.add(keep);

        assertThatThrownBy(() -> service.deleteBoard(99))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("There is no board with ID 99.");
        assertThat(repository.all()).containsExactly(keep);
    }

    @Test
    void blocksOnlyTheBoardThatHasTasks() {
        repository.add(new Board(1L, "with task", EARLY));
        repository.add(new Board(2L, "empty", EARLY));
        tasks.add(new Task(5L, 1L, "Task", null, TaskStatus.TODO, EARLY, EARLY));

        service.deleteBoard(2);

        assertThat(repository.all()).extracting(Board::getName).containsExactly("with task");
    }

    @Test
    void deletesTheBoardAfterItsLastTaskIsGone() {
        repository.add(new Board(1L, "board", EARLY));
        tasks.add(new Task(5L, 1L, "Task", null, TaskStatus.TODO, EARLY, EARLY));
        assertThatThrownBy(() -> service.deleteBoard(1)).isInstanceOf(BoardNotEmptyException.class);

        tasks.deleteTaskById(5L);
        service.deleteBoard(1);

        assertThat(repository.all()).isEmpty();
    }

    private void assertRejectedAsName(String name, String message) {
        int boardsBefore = repository.all().size();
        assertThatThrownBy(() -> service.addBoard(name))
                .isInstanceOfSatisfying(ValidationException.class, exception -> {
                    assertThat(exception.getField()).isEqualTo("name");
                    assertThat(exception.getMessage()).isEqualTo(message);
                });
        assertThat(repository.all()).hasSize(boardsBefore);
    }
}
