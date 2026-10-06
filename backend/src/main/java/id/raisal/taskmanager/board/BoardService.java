package id.raisal.taskmanager.board;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BoardService {

    private final BoardRepository boards;
    private final Clock clock;

    public BoardService(BoardRepository boards, Clock clock) {
        this.boards = boards;
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
}
