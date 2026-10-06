package id.raisal.taskmanager.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import id.raisal.taskmanager.board.Board;
import id.raisal.taskmanager.board.InMemoryBoardRepository;
import id.raisal.taskmanager.common.error.NotFoundException;
import id.raisal.taskmanager.common.error.ValidationException;
import java.time.Clock;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicLong;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class TaskServiceTest {

    private static final long BOARD_ID = 1L;
    private static final long MISSING_BOARD_ID = 99L;
    private static final Instant NOW = Instant.parse("2026-02-03T04:05:06.123456789Z");
    private static final Instant NOW_IN_MICROS = Instant.parse("2026-02-03T04:05:06.123456Z");

    private final InMemoryBoardRepository boards = new InMemoryBoardRepository();
    private final InMemoryTaskRepository tasks = new InMemoryTaskRepository();
    private final TaskService service = new TaskService(tasks, boards, Clock.fixed(NOW, ZoneOffset.UTC));

    @BeforeEach
    void addABoard() {
        boards.add(new Board(BOARD_ID, "Board", Instant.parse("2026-01-01T10:00:00Z")));
    }

    @Test
    void createsTaskOnExistingBoardWithStatusTodo() {
        Task task = service.addTask(BOARD_ID, "  Write tests \t", "  Details  ");

        assertThat(task.getId()).isNotNull();
        assertThat(task.getBoardId()).isEqualTo(BOARD_ID);
        assertThat(task.getTitle()).isEqualTo("Write tests");
        assertThat(task.getDescription()).isEqualTo("Details");
        assertThat(task.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(tasks.all()).containsExactly(task);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void rejectsEmptyTitle(String title) {
        assertRejected("title", "Title is required.", () -> service.addTask(BOARD_ID, title, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"   ", "\t", " \t \n "})
    void rejectsWhitespaceOnlyTitle(String title) {
        assertRejected("title", "Title is required.", () -> service.addTask(BOARD_ID, title, null));
    }

    @Test
    void rejectsTaskOnMissingBoard() {
        assertThatThrownBy(() -> service.addTask(MISSING_BOARD_ID, "Title", null))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("There is no board with ID 99.");
        assertThat(tasks.all()).isEmpty();
    }

    @Test
    void missingBoardPrecedesTitleValidation() {
        // A11: the board (404) comes before the fields (400).
        assertThatThrownBy(() -> service.addTask(MISSING_BOARD_ID, "  ", "y".repeat(2001)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void rejectsTitleLongerThan200Characters() {
        assertRejected("title", "Title must have 200 characters or fewer.", () -> service.addTask(BOARD_ID, "x".repeat(201), null));
    }

    @Test
    void rejectsDescriptionLongerThan2000Characters() {
        assertRejected(
                "description",
                "Description must have 2000 characters or fewer.",
                () -> service.addTask(BOARD_ID, "Title", "y".repeat(2001)));
    }

    @Test
    void acceptsTitleAndDescriptionAtLengthLimits() {
        Task task = service.addTask(BOARD_ID, "x".repeat(200), "y".repeat(2000));

        assertThat(task.getTitle()).hasSize(200);
        assertThat(task.getDescription()).hasSize(2000);
    }

    @Test
    void countsCharactersAndNotUtf16Units() {
        // 200 characters that each use two UTF-16 units. The database accepts them.
        assertThat(service.addTask(BOARD_ID, "😀".repeat(200), null).getTitle()).hasSize(400);
        assertRejected("title", "Title must have 200 characters or fewer.", () -> service.addTask(BOARD_ID, "😀".repeat(201), null));
    }

    @Test
    void creationTimesUseSameClockInstant() {
        // A12 and P6: one instant for both times. TIMESTAMPTZ keeps microseconds, so the service cuts the Clock value.
        Task task = service.addTask(BOARD_ID, "Title", null);

        assertThat(task.getCreatedAt()).isEqualTo(NOW_IN_MICROS);
        assertThat(task.getUpdatedAt()).isEqualTo(task.getCreatedAt());
    }

    @Test
    void readsTheClockOnlyOnceForTheTwoTimes() {
        // A clock that moves on at each reading. Two readings would give two different times.
        TickingClock ticking = new TickingClock(NOW_IN_MICROS);
        TaskService tickingService = new TaskService(tasks, boards, ticking);

        Task task = tickingService.addTask(BOARD_ID, "Title", null);

        assertThat(task.getUpdatedAt()).isEqualTo(task.getCreatedAt());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", " \t\n "})
    void keepsBlankDescriptionAsNull(String description) {
        assertThat(service.addTask(BOARD_ID, "Title", description).getDescription()).isNull();
    }

    private void assertRejected(String field, String message, Runnable action) {
        int tasksBefore = tasks.all().size();
        assertThatThrownBy(action::run).isInstanceOfSatisfying(ValidationException.class, exception -> {
            assertThat(exception.getField()).isEqualTo(field);
            assertThat(exception.getMessage()).isEqualTo(message);
        });
        assertThat(tasks.all()).hasSize(tasksBefore);
    }

    /** Each reading of instant() is one microsecond later than the previous reading. */
    private static final class TickingClock extends Clock {

        private final AtomicLong microsecondsAfterStart = new AtomicLong();
        private final Instant start;

        TickingClock(Instant start) {
            this.start = start;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return start.plusNanos(1_000 * microsecondsAfterStart.getAndIncrement());
        }
    }
}
