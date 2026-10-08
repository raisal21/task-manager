package id.raisal.taskmanager.task;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Tasks", description = "Create tasks in a board, filter by status, change status, and delete tasks.")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/api/boards/{boardId}/tasks")
    @Operation(summary = "List tasks in a board", description = "Ordered by createdAt, then id. Omit status to list all tasks.")
    public List<TaskResponse> listTasks(@PathVariable long boardId,
            @Parameter(description = "Optional status filter", schema = @Schema(allowableValues = {"TODO", "IN_PROGRESS", "DONE"}))
            @RequestParam(required = false) String status) {
        return taskService.listTasks(boardId, status).stream().map(TaskResponse::from).toList();
    }

    @PostMapping("/api/boards/{boardId}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a task", description = "New tasks start as TODO. Empty descriptions become null.")
    public TaskResponse addTask(@PathVariable long boardId, @RequestBody CreateTaskRequest request) {
        return TaskResponse.from(taskService.addTask(boardId, request.title(), request.description()));
    }

    @PatchMapping("/api/tasks/{taskId}")
    @Operation(summary = "Change task status", description = "Only status can be changed. All transitions are allowed. "
            + "Repeating the current status leaves updatedAt unchanged.")
    public TaskResponse changeStatus(@PathVariable long taskId, @RequestBody UpdateTaskRequest request) {
        return TaskResponse.from(taskService.changeStatus(taskId, request.status(), request.unknownFields()));
    }

    @DeleteMapping("/api/tasks/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a task", description = "A second delete returns 404 because the task no longer exists.")
    public void deleteTask(@PathVariable long taskId) {
        taskService.deleteTask(taskId);
    }
}
