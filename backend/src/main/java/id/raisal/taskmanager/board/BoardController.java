package id.raisal.taskmanager.board;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/boards")
@Tag(name = "Boards", description = "Create and list boards. A board can be deleted when it has no tasks.")
public class BoardController {

    private final BoardService boardService;

    public BoardController(BoardService boardService) {
        this.boardService = boardService;
    }

    @GetMapping
    @Operation(summary = "List boards", description = "Ordered by createdAt, then id. An empty database returns [].")
    public List<BoardResponse> listBoards() {
        return boardService.listBoards().stream().map(BoardResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a board", description = "Trims the name. Duplicate names are allowed.")
    public BoardResponse addBoard(@RequestBody CreateBoardRequest request) {
        return BoardResponse.from(boardService.addBoard(request.name()));
    }

    @DeleteMapping("/{boardId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete an empty board", description = "Returns 409 if any tasks remain, including DONE tasks.")
    public void deleteBoard(@PathVariable long boardId) {
        boardService.deleteBoard(boardId);
    }
}
