package id.raisal.taskmanager.task;

import io.swagger.v3.oas.annotations.media.Schema;

public record CreateTaskRequest(
        @Schema(description = "Task title, trimmed and nonblank", example = "Present the API",
                requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1, maxLength = 200)
        String title,
        @Schema(description = "Optional description; missing, blank, or null becomes null",
                example = "Show the request, response, and saved PostgreSQL row", maxLength = 2000, nullable = true)
        String description) {
}
