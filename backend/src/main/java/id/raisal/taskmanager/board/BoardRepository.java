package id.raisal.taskmanager.board;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/** Only the methods that BoardService uses, so that a fake stays small. */
public interface BoardRepository extends Repository<Board, Long> {

    /** A5: created_at from the first to the last, then id. */
    List<Board> findAllByOrderByCreatedAtAscIdAsc();

    Board save(Board board);

    Optional<Board> findById(Long id);

    /** One DELETE statement. It gives the number of rows that it deleted: 0 means that there was no such board. */
    @Modifying
    @Query("delete from Board b where b.id = ?1")
    int deleteBoardById(Long id);
}
