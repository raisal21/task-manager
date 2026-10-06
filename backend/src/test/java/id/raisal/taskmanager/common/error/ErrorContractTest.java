package id.raisal.taskmanager.common.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import id.raisal.taskmanager.board.Board;
import id.raisal.taskmanager.board.BoardController;
import id.raisal.taskmanager.board.InMemoryBoardRepository;
import id.raisal.taskmanager.support.FakeServicesConfiguration;
import id.raisal.taskmanager.task.InMemoryTaskRepository;
import id.raisal.taskmanager.task.Task;
import id.raisal.taskmanager.task.TaskController;
import id.raisal.taskmanager.task.TaskStatus;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Web slice: the error body of the controller paths. MockMvc does not do the container error dispatch
 * to /error, so the /error path has its own test in ErrorContractIT.
 */
@WebMvcTest({BoardController.class, TaskController.class})
@Import(FakeServicesConfiguration.class)
class ErrorContractTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    InMemoryBoardRepository boards;

    @Autowired
    InMemoryTaskRepository tasks;

    /** Task 1 on board 1, with status TODO. The task must be there, because the task (404) comes before the body (400). */
    @BeforeEach
    void seedOneTask() {
        boards.clear();
        tasks.clear();
        Instant created = Instant.parse("2026-01-01T10:00:00Z");
        boards.add(new Board(1L, "Board", created));
        tasks.add(new Task(1L, 1L, "Title", null, TaskStatus.TODO, created, created));
    }

    @Test
    void validationErrorHasFieldAndCode() throws Exception {
        ResultActions result = mvc.perform(post("/api/boards").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"  \"}"));

        assertErrorShape(result, 400, "VALIDATION_FAILED", "/api/boards")
                .andExpect(jsonPath("$.field").value("name"))
                .andExpect(jsonPath("$.detail").value("Name is required."));
    }

    @Test
    void malformedJsonUsesErrorShape() throws Exception {
        ResultActions result = mvc.perform(post("/api/boards").contentType(MediaType.APPLICATION_JSON).content("{bad"));

        assertErrorShape(result, 400, "MALFORMED_REQUEST", "/api/boards")
                .andExpect(jsonPath("$.field").value(nullValue()))
                .andExpect(jsonPath("$.detail").value("The request body is missing or is not valid JSON."));
    }

    @Test
    void missingBodyUsesErrorShape() throws Exception {
        ResultActions result = mvc.perform(post("/api/boards").contentType(MediaType.APPLICATION_JSON));

        assertErrorShape(result, 400, "MALFORMED_REQUEST", "/api/boards");
    }

    @Test
    void wrongPathTypeUsesErrorShape() throws Exception {
        // P3: the path has a variable, so that this is a real path type error. M1 had no such path.
        ResultActions result = mvc.perform(get("/api/boards/abc/tasks"));

        assertErrorShape(result, 400, "VALIDATION_FAILED", "/api/boards/abc/tasks")
                .andExpect(jsonPath("$.field").value("boardId"))
                .andExpect(jsonPath("$.detail").value("The value of 'boardId' is not correct."));
    }

    @Test
    void wrongPathTypeOnPostUsesTheSameErrorShape() throws Exception {
        ResultActions result = mvc.perform(post("/api/boards/abc/tasks").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"x\"}"));

        assertErrorShape(result, 400, "VALIDATION_FAILED", "/api/boards/abc/tasks").andExpect(jsonPath("$.field").value("boardId"));
    }

    @Test
    void malformedTaskPostPrecedesBoardLookup() throws Exception {
        // A11: malformed JSON (400) comes before the board (404). The board 99999 is not there.
        ResultActions result = mvc.perform(post("/api/boards/99999/tasks").contentType(MediaType.APPLICATION_JSON).content("{bad"));

        assertErrorShape(result, 400, "MALFORMED_REQUEST", "/api/boards/99999/tasks");
    }

    @Test
    void unknownRouteUsesErrorShape() throws Exception {
        ResultActions result = mvc.perform(get("/api/nothing-here"));

        assertErrorShape(result, 404, "NOT_FOUND", "/api/nothing-here")
                .andExpect(jsonPath("$.detail").value("There is no endpoint for this path."));
    }

    @Test
    void unsupportedMethodUsesErrorShape() throws Exception {
        ResultActions result = mvc.perform(put("/api/boards").contentType(MediaType.APPLICATION_JSON).content("{}"));

        assertErrorShape(result, 405, "METHOD_NOT_ALLOWED", "/api/boards")
                .andExpect(header().string("Allow", containsString("GET")))
                .andExpect(header().string("Allow", containsString("POST")));
    }

    @Test
    void unsupportedMediaTypeUsesErrorShape() throws Exception {
        ResultActions result = mvc.perform(post("/api/boards").contentType(MediaType.TEXT_PLAIN).content("name=x"));

        assertErrorShape(result, 415, "UNSUPPORTED_MEDIA_TYPE", "/api/boards")
                .andExpect(header().string("Accept", containsString("application/json")));
    }

    // --- PATCH /api/tasks/{taskId}: the body forms of A7 ---

    private ResultActions patchTask(String path, String body) throws Exception {
        return mvc.perform(patch(path).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private void assertTaskUnchanged() {
        assertThat(tasks.findById(1L)).hasValueSatisfying(task -> {
            assertThat(task.getStatus()).isEqualTo(TaskStatus.TODO);
            assertThat(task.getUpdatedAt()).isEqualTo(Instant.parse("2026-01-01T10:00:00Z"));
        });
    }

    @Test
    void unknownFieldInPatchIsRejected() throws Exception {
        ResultActions result = patchTask("/api/tasks/1", "{\"status\":\"DONE\",\"title\":\"Other\"}");

        assertErrorShape(result, 400, "VALIDATION_FAILED", "/api/tasks/1")
                .andExpect(jsonPath("$.field").value("title"))
                .andExpect(jsonPath("$.detail").value("Unknown field 'title'. Only 'status' can be changed."));
        assertTaskUnchanged();
    }

    @Test
    void emptyPatchIsRejected() throws Exception {
        // No body at all.
        assertErrorShape(mvc.perform(patch("/api/tasks/1").contentType(MediaType.APPLICATION_JSON)), 400, "MALFORMED_REQUEST", "/api/tasks/1");
        // An empty object has no status.
        assertErrorShape(patchTask("/api/tasks/1", "{}"), 400, "VALIDATION_FAILED", "/api/tasks/1").andExpect(jsonPath("$.field").value("status"));
        assertTaskUnchanged();
    }

    @Test
    void missingPatchStatusIsRejected() throws Exception {
        ResultActions result = patchTask("/api/tasks/1", "{}");

        assertErrorShape(result, 400, "VALIDATION_FAILED", "/api/tasks/1")
                .andExpect(jsonPath("$.field").value("status"))
                .andExpect(jsonPath("$.detail").value("Status is required."));
        assertTaskUnchanged();
    }

    @Test
    void nullPatchStatusIsRejected() throws Exception {
        ResultActions result = patchTask("/api/tasks/1", "{\"status\":null}");

        assertErrorShape(result, 400, "VALIDATION_FAILED", "/api/tasks/1")
                .andExpect(jsonPath("$.field").value("status"))
                .andExpect(jsonPath("$.detail").value("Status is required."));
        assertTaskUnchanged();
    }

    @Test
    void malformedPatchUsesErrorShape() throws Exception {
        assertErrorShape(patchTask("/api/tasks/1", "{bad"), 400, "MALFORMED_REQUEST", "/api/tasks/1");
        assertTaskUnchanged();
    }

    @ParameterizedTest
    @ValueSource(strings = {"[]", "[\"DONE\"]", "\"DONE\"", "42", "true", "null"})
    void nonObjectPatchIsRejected(String body) throws Exception {
        assertErrorShape(patchTask("/api/tasks/1", body), 400, "MALFORMED_REQUEST", "/api/tasks/1");
        assertTaskUnchanged();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"status\":{\"a\":1}}", "{\"status\":[\"DONE\"]}"})
    void statusThatIsNotAScalarIsRejected(String body) throws Exception {
        assertErrorShape(patchTask("/api/tasks/1", body), 400, "MALFORMED_REQUEST", "/api/tasks/1");
        assertTaskUnchanged();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"status\":\"BLOCKED\"}", "{\"status\":\"todo\"}", "{\"status\":\"\"}", "{\"status\":5}", "{\"status\":true}", "{\"Status\":\"DONE\"}"})
    void invalidStatusPatchUsesErrorShape(String body) throws Exception {
        ResultActions result = patchTask("/api/tasks/1", body);

        assertErrorShape(result, 400, "VALIDATION_FAILED", "/api/tasks/1");
        assertTaskUnchanged();
    }

    @Test
    void missingTaskPatchUsesErrorShapeBeforeTheBodyIsChecked() throws Exception {
        // The body is also not correct. The task (404) comes first.
        ResultActions result = patchTask("/api/tasks/99999", "{\"title\":\"x\"}");

        assertErrorShape(result, 404, "NOT_FOUND", "/api/tasks/99999")
                .andExpect(jsonPath("$.detail").value("There is no task with ID 99999."));
    }

    @Test
    void wrongTaskIdTypeOnPatchUsesErrorShape() throws Exception {
        assertErrorShape(patchTask("/api/tasks/abc", "{\"status\":\"DONE\"}"), 400, "VALIDATION_FAILED", "/api/tasks/abc")
                .andExpect(jsonPath("$.field").value("taskId"));
    }

    @Test
    void wrongTaskIdTypeOnDeleteUsesErrorShape() throws Exception {
        assertErrorShape(mvc.perform(delete("/api/tasks/abc")), 400, "VALIDATION_FAILED", "/api/tasks/abc")
                .andExpect(jsonPath("$.field").value("taskId"));
    }

    @Test
    void unsupportedMethodOnATaskUsesErrorShape() throws Exception {
        ResultActions result = mvc.perform(put("/api/tasks/1").contentType(MediaType.APPLICATION_JSON).content("{}"));

        assertErrorShape(result, 405, "METHOD_NOT_ALLOWED", "/api/tasks/1")
                .andExpect(header().string("Allow", containsString("PATCH")))
                .andExpect(header().string("Allow", containsString("DELETE")));
    }

    /** All members are there. field is present, also when it is null. */
    private static ResultActions assertErrorShape(ResultActions result, int status, String code, String instance) throws Exception {
        return result.andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$", hasKey("type")))
                .andExpect(jsonPath("$", hasKey("title")))
                .andExpect(jsonPath("$", hasKey("status")))
                .andExpect(jsonPath("$", hasKey("detail")))
                .andExpect(jsonPath("$", hasKey("instance")))
                .andExpect(jsonPath("$", hasKey("code")))
                .andExpect(jsonPath("$", hasKey("field")))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.instance").value(instance));
    }
}
