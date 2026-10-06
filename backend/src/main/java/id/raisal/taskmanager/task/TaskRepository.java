package id.raisal.taskmanager.task;

import org.springframework.data.repository.Repository;

/** Only the methods that TaskService uses, so that a fake stays small. */
public interface TaskRepository extends Repository<Task, Long> {

    Task save(Task task);
}
