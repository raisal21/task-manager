package id.raisal.taskmanager.board;

import id.raisal.taskmanager.common.error.BoardNotEmptyException;
import id.raisal.taskmanager.common.error.ConstraintNames;
import id.raisal.taskmanager.common.error.NotFoundException;
import id.raisal.taskmanager.task.TaskRepository;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BoardService {

    private static final String TASK_BOARD_FOREIGN_KEY = "fk_tasks_board";

    private final BoardRepository boards;
    private final TaskRepository tasks;
    private final Clock clock;

    public BoardService(BoardRepository boards, TaskRepository tasks, Clock clock) {
        this.boards = boards;
        this.tasks = tasks;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Board> listBoards() {
        return boards.findAllByOrderByCreatedAtAscIdAsc();
    }

    /** One write transaction: the name rule and the new row. The Clock gives created_at (A12). */
    @Transactional
    public Board addBoard(String name) {
        String validName = BoardRules.name(name);
        // TIMESTAMPTZ keeps microseconds. The response must show the value that the row keeps.
        return boards.save(new Board(null, validName, clock.instant().truncatedTo(ChronoUnit.MICROS)));
    }

    /**
     * A board can be deleted only if it has no tasks (A2). One write transaction: the task check, then one DELETE
     * statement. The statement runs at once, so that its foreign key error comes here and not at the commit.
     * Another transaction can add a task between the check and the DELETE. Then the foreign key
     * (ON DELETE RESTRICT) rejects the DELETE, and the answer is the same 409 (section 7.10).
     */
    @Transactional
    public void deleteBoard(long boardId) {
        if (tasks.existsByBoardId(boardId)) {
            throw new BoardNotEmptyException(boardId);
        }
        int deleted;
        try {
            deleted = boards.deleteBoardById(boardId);
        } catch (DataIntegrityViolationException exception) {
            if (ConstraintNames.isForeignKeyViolation(exception, TASK_BOARD_FOREIGN_KEY)) {
                throw new BoardNotEmptyException(boardId);
            }
            throw exception;
        }
        if (deleted == 0) {
            throw new NotFoundException("There is no board with ID " + boardId + ".");
        }
    }
}
