package id.raisal.taskmanager.board;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Web slice: the JSON of the board endpoints. No database and no Docker. */
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
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-02-03T04:05:06Z"), ZoneOffset.UTC);
        }

        @Bean
        BoardService boardService(InMemoryBoardRepository repository, Clock clock) {
            return new BoardService(repository, clock);
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

    @Test
    void addsABoardAndSends201WithTheTrimmedName() throws Exception {
        mvc.perform(post("/api/boards").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"  Sprint 2  \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(8))
                .andExpect(jsonPath("$.name").value("Sprint 2"))
                .andExpect(jsonPath("$.createdAt").value("2026-02-03T04:05:06Z"));
    }

    @Test
    void rejectsAMissingNameWithTheValidationErrorBody() throws Exception {
        mvc.perform(post("/api/boards").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.field").value("name"))
                .andExpect(jsonPath("$.detail").value("Name is required."));
    }
}
