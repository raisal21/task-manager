package id.raisal.taskmanager.task;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/** Only the methods that TaskService uses, so that a fake stays small. */
public interface TaskRepository extends Repository<Task, Long> {

    Task save(Task task);

    Optional<Task> findById(Long id);

    boolean existsByBoardId(Long boardId);

    /** A5: created_at from the first to the last, then id. */
    List<Task> findByBoardIdOrderByCreatedAtAscIdAsc(Long boardId);

    /** One DELETE statement. It gives the number of rows that it deleted: 0 means that there was no such task. */
    @Modifying
    @Query("delete from Task t where t.id = ?1")
    int deleteTaskById(Long id);
}
