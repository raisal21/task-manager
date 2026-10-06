package id.raisal.taskmanager.task;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import id.raisal.taskmanager.board.Board;
import id.raisal.taskmanager.board.InMemoryBoardRepository;
import id.raisal.taskmanager.support.FakeServicesConfiguration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Web slice: the JSON of the task endpoints. No database and no Docker. */
@WebMvcTest(TaskController.class)
@Import(FakeServicesConfiguration.class)
class TaskControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    InMemoryBoardRepository boards;

    @Autowired
    InMemoryTaskRepository tasks;

    @BeforeEach
    void seedOneBoard() {
        boards.clear();
        tasks.clear();
        boards.add(new Board(1L, "Board", Instant.parse("2026-01-01T10:00:00Z")));
    }

    @Test
    void addsATaskAndSends201WithStatusTodo() throws Exception {
        postTask(1, "{\"title\":\"  Write  \",\"description\":\"  Details \"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.boardId").value(1))
                .andExpect(jsonPath("$.title").value("Write"))
                .andExpect(jsonPath("$.description").value("Details"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.createdAt").value("2026-02-03T04:05:06Z"))
                .andExpect(jsonPath("$.updatedAt").value("2026-02-03T04:05:06Z"));
    }

    @Test
    void sendsNullForAMissingDescription() throws Exception {
        postTask(1, "{\"title\":\"Write\",\"description\":\"   \"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value(nullValue()));
    }

    @Test
    void rejectsAMissingTitleWithTheTitleField() throws Exception {
        postTask(1, "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.field").value("title"))
                .andExpect(jsonPath("$.detail").value("Title is required."));
    }

    @Test
    void sends404ForAMissingBoard() throws Exception {
        postTask(99, "{\"title\":\"Write\"}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.field").value(nullValue()))
                .andExpect(jsonPath("$.detail").value("There is no board with ID 99."));
    }

    private ResultActions postTask(long boardId, String json) throws Exception {
        return mvc.perform(post("/api/boards/" + boardId + "/tasks").contentType(MediaType.APPLICATION_JSON).content(json));
    }
}
