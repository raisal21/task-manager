package id.raisal.taskmanager.task;

import id.raisal.taskmanager.board.BoardRepository;
import id.raisal.taskmanager.common.error.ConstraintNames;
import id.raisal.taskmanager.common.error.NotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {

    private static final String BOARD_FOREIGN_KEY = "fk_tasks_board";

    private final TaskRepository tasks;
    private final BoardRepository boards;
    private final Clock clock;

    public TaskService(TaskRepository tasks, BoardRepository boards, Clock clock) {
        this.tasks = tasks;
        this.boards = boards;
        this.clock = clock;
    }

    /**
     * One write transaction: the board lookup, the rule functions, and the new row (P5).
     * The sequence of the errors is: the board (404), then the fields (400) (A11).
     */
    @Transactional
    public Task addTask(long boardId, String title, String description) {
        requireBoard(boardId);
        String validTitle = TaskRules.title(title);
        String validDescription = TaskRules.description(description);
        // One instant for both times (A12). TIMESTAMPTZ keeps microseconds.
        Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
        Task task = new Task(null, boardId, validTitle, validDescription, TaskStatus.TODO, now, now);
        try {
            return tasks.save(task);
        } catch (DataIntegrityViolationException exception) {
            // Another transaction deleted the board after the lookup. For this operation, the board is not there.
            if (BOARD_FOREIGN_KEY.equals(ConstraintNames.of(exception))) {
                throw new NotFoundException(noBoardMessage(boardId));
            }
            throw exception;
        }
    }

    private void requireBoard(long boardId) {
        if (boards.findById(boardId).isEmpty()) {
            throw new NotFoundException(noBoardMessage(boardId));
        }
    }

    private static String noBoardMessage(long boardId) {
        return "There is no board with ID " + boardId + ".";
    }
}
