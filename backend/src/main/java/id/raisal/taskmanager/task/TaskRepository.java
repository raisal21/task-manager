package id.raisal.taskmanager.task;

import java.util.List;
import org.springframework.data.repository.Repository;

/** Only the methods that TaskService uses, so that a fake stays small. */
public interface TaskRepository extends Repository<Task, Long> {

    Task save(Task task);

    /** A5: created_at from the first to the last, then id. */
    List<Task> findByBoardIdOrderByCreatedAtAscIdAsc(Long boardId);
}
