package id.raisal.taskmanager.board;

import java.util.List;
import org.springframework.data.repository.Repository;

/** Only the methods that BoardService uses, so that a fake stays small. */
public interface BoardRepository extends Repository<Board, Long> {

    /** A5: created_at from the first to the last, then id. */
    List<Board> findAllByOrderByCreatedAtAscIdAsc();
}
