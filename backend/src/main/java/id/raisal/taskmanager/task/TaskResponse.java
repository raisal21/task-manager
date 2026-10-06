package id.raisal.taskmanager.task;

import java.time.Instant;

public record TaskResponse(
        long id, long boardId, String title, String description, String status, Instant createdAt, Instant updatedAt) {

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
