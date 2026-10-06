package id.raisal.taskmanager.board;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BoardService {

    private final BoardRepository boards;

    public BoardService(BoardRepository boards) {
        this.boards = boards;
    }

    @Transactional(readOnly = true)
    public List<Board> listBoards() {
        return boards.findAllByOrderByCreatedAtAscIdAsc();
    }
}
