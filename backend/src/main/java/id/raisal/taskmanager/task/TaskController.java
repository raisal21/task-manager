package id.raisal.taskmanager.task;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/api/boards/{boardId}/tasks")
    public List<TaskResponse> listTasks(@PathVariable long boardId, @RequestParam(required = false) String status) {
        return taskService.listTasks(boardId, status).stream().map(TaskResponse::from).toList();
    }

    @PostMapping("/api/boards/{boardId}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse addTask(@PathVariable long boardId, @RequestBody CreateTaskRequest request) {
        return TaskResponse.from(taskService.addTask(boardId, request.title(), request.description()));
    }

    @PatchMapping("/api/tasks/{taskId}")
    public TaskResponse changeStatus(@PathVariable long taskId, @RequestBody UpdateTaskRequest request) {
        return TaskResponse.from(taskService.changeStatus(taskId, request.status(), request.unknownFields()));
    }

    @DeleteMapping("/api/tasks/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTask(@PathVariable long taskId) {
        taskService.deleteTask(taskId);
    }
}
