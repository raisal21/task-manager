package id.raisal.taskmanager.task;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @Test
    void listsTheTasksOfABoardAsAJsonArray() throws Exception {
        tasks.add(new Task(5L, 1L, "Write", "Details", TaskStatus.IN_PROGRESS, Instant.parse("2026-01-01T10:00:00Z"), Instant.parse("2026-01-02T10:00:00Z")));

        mvc.perform(get("/api/boards/1/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(5))
                .andExpect(jsonPath("$[0].boardId").value(1))
                .andExpect(jsonPath("$[0].title").value("Write"))
                .andExpect(jsonPath("$[0].description").value("Details"))
                .andExpect(jsonPath("$[0].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$[0].createdAt").value("2026-01-01T10:00:00Z"))
                .andExpect(jsonPath("$[0].updatedAt").value("2026-01-02T10:00:00Z"));
    }

    @Test
    void filtersByTheStatusQueryParameter() throws Exception {
        tasks.add(new Task(1L, 1L, "todo", null, TaskStatus.TODO, Instant.parse("2026-01-01T10:00:00Z"), Instant.parse("2026-01-01T10:00:00Z")));
        tasks.add(new Task(2L, 1L, "done", null, TaskStatus.DONE, Instant.parse("2026-01-01T10:00:00Z"), Instant.parse("2026-01-01T10:00:00Z")));

        mvc.perform(get("/api/boards/1/tasks").param("status", "DONE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("done"));
    }

    @Test
    void sendsAnEmptyArrayForABoardWithoutTasks() throws Exception {
        mvc.perform(get("/api/boards/1/tasks")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void rejectsAnUnknownStatusWithTheStatusField() throws Exception {
        mvc.perform(get("/api/boards/1/tasks").param("status", "BLOCKED"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.field").value("status"));
    }

    @Test
    void sends404WhenListingTheTasksOfAMissingBoard() throws Exception {
        mvc.perform(get("/api/boards/99/tasks")).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    private ResultActions postTask(long boardId, String json) throws Exception {
        return mvc.perform(post("/api/boards/" + boardId + "/tasks").contentType(MediaType.APPLICATION_JSON).content(json));
    }
}
