package id.raisal.taskmanager.task;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** A fake for unit tests. It keeps tasks in memory and obeys the contract of the repository interface. */
public class InMemoryTaskRepository implements TaskRepository {

    private final List<Task> tasks = new ArrayList<>();
    private long lastId = 0;

    public void add(Task task) {
        tasks.add(task);
        lastId = Math.max(lastId, task.getId());
    }

    public List<Task> all() {
        return List.copyOf(tasks);
    }

    public void clear() {
        tasks.clear();
        lastId = 0;
    }

    @Override
    public Task save(Task task) {
        Task saved = new Task(
                ++lastId, task.getBoardId(), task.getTitle(), task.getDescription(), task.getStatus(), task.getCreatedAt(), task.getUpdatedAt());
        tasks.add(saved);
        return saved;
    }

    @Override
    public List<Task> findByBoardIdOrderByCreatedAtAscIdAsc(Long boardId) {
        return tasks.stream()
                .filter(task -> task.getBoardId().equals(boardId))
                .sorted(Comparator.comparing(Task::getCreatedAt).thenComparing(Task::getId))
                .toList();
    }
}
