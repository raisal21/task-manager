package id.raisal.taskmanager.board;

import java.time.Instant;

public record BoardResponse(long id, String name, Instant createdAt) {

    static BoardResponse from(Board board) {
        return new BoardResponse(board.getId(), board.getName(), board.getCreatedAt());
    }
}
