package id.raisal.taskmanager.board;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(requiredProperties = {"id", "name", "createdAt"})
public record BoardResponse(long id, String name, Instant createdAt) {

    static BoardResponse from(Board board) {
        return new BoardResponse(board.getId(), board.getName(), board.getCreatedAt());
    }
}
