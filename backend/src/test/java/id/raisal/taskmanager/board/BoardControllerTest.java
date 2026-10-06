package id.raisal.taskmanager.board;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/** Web slice: the JSON of GET /api/boards. No database and no Docker. */
@WebMvcTest(BoardController.class)
@Import(BoardControllerTest.FakeBoards.class)
class BoardControllerTest {

    @TestConfiguration
    static class FakeBoards {

        @Bean
        InMemoryBoardRepository boardRepository() {
            InMemoryBoardRepository repository = new InMemoryBoardRepository();
            repository.add(new Board(7L, "Sprint 1", Instant.parse("2026-01-01T10:00:00Z")));
            return repository;
        }

        @Bean
        BoardService boardService(InMemoryBoardRepository repository) {
            return new BoardService(repository);
        }
    }

    @Autowired
    MockMvc mvc;

    @Test
    void sendsBoardsAsAJsonArrayWithIdNameAndCreatedAt() throws Exception {
        mvc.perform(get("/api/boards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].name").value("Sprint 1"))
                .andExpect(jsonPath("$[0].createdAt").value("2026-01-01T10:00:00Z"));
    }
}
