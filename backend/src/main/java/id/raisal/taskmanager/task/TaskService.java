package id.raisal.taskmanager.task;

import id.raisal.taskmanager.board.BoardRepository;
import id.raisal.taskmanager.common.error.ConstraintNames;
import id.raisal.taskmanager.common.error.NotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
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
            if (ConstraintNames.isForeignKeyViolation(exception, BOARD_FOREIGN_KEY)) {
                throw new NotFoundException(noBoardMessage(boardId));
            }
            throw exception;
        }
    }

    /**
     * A read-only transaction: the board lookup and the list. The status filter is a service rule (dec_10),
     * so that a unit test can examine it with a fake. The status comes in as a string and TaskStatus.parse checks it.
     * The sequence of the errors is the same as for the add: the board (404), then the status (400).
     */
    @Transactional(readOnly = true)
    public List<Task> listTasks(long boardId, String statusFilter) {
        requireBoard(boardId);
        TaskStatus status = statusFilter == null ? null : TaskStatus.parse(statusFilter);
        List<Task> boardTasks = tasks.findByBoardIdOrderByCreatedAtAscIdAsc(boardId);
        if (status == null) {
            return boardTasks;
        }
        return boardTasks.stream().filter(task -> task.getStatus() == status).toList();
    }

    /**
     * One write transaction: the task lookup, the rule functions, and the change (P5). The task is a managed entity,
     * so that the UPDATE goes to the database when the transaction ends. The sequence of the errors is: the task (404),
     * then the body fields (400). All status changes are permitted (A4). If the status is the same, nothing changes,
     * also not updated_at (A18).
     */
    @Transactional
    public Task changeStatus(long taskId, String status, List<String> unknownFields) {
        Task task = tasks.findById(taskId).orElseThrow(() -> new NotFoundException("There is no task with ID " + taskId + "."));
        TaskStatus newStatus = TaskRules.statusChange(unknownFields, status);
        if (task.getStatus() != newStatus) {
            task.setStatus(newStatus);
            task.setUpdatedAt(clock.instant().truncatedTo(ChronoUnit.MICROS));
        }
        return task;
    }

    /** One write transaction with one DELETE statement. A task that is not there (also if it is just gone) is a 404. */
    @Transactional
    public void deleteTask(long taskId) {
        if (tasks.deleteTaskById(taskId) == 0) {
            throw new NotFoundException("There is no task with ID " + taskId + ".");
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
