package id.raisal.taskmanager.task;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(requiredProperties = {"id", "boardId", "title", "description", "status", "createdAt", "updatedAt"})
public record TaskResponse(
        long id, long boardId, String title,
        @Schema(nullable = true) String description,
        @Schema(allowableValues = {"TODO", "IN_PROGRESS", "DONE"}) String status,
        Instant createdAt, Instant updatedAt) {

    static TaskResponse from(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getBoardId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus().name(),
                task.getCreatedAt(),
                task.getUpdatedAt());
    }
}
