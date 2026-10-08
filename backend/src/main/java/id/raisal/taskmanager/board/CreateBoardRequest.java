package id.raisal.taskmanager.board;

import io.swagger.v3.oas.annotations.media.Schema;

public record CreateBoardRequest(
        @Schema(description = "Board name, trimmed and nonblank", example = "Presentation board",
                requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1, maxLength = 100)
        String name) {
}
