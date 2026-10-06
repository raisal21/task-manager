package id.raisal.taskmanager.common.error;

/** A board that has tasks cannot be deleted. The error body gives 409 BOARD_NOT_EMPTY (A2, dec_03). */
public class BoardNotEmptyException extends RuntimeException {

    public BoardNotEmptyException(long boardId) {
        super("Board " + boardId + " has tasks. Delete its tasks first.");
    }
}
