package id.raisal.taskmanager.board;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import id.raisal.taskmanager.support.FakeBoardsConfiguration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Web slice: the JSON of the board endpoints. No database and no Docker. */
@WebMvcTest(BoardController.class)
@Import(FakeBoardsConfiguration.class)
class BoardControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    InMemoryBoardRepository repository;

    @BeforeEach
    void seedOneBoard() {
        repository.clear();
        repository.add(new Board(7L, "Sprint 1", Instant.parse("2026-01-01T10:00:00Z")));
    }

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
