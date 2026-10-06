package id.raisal.taskmanager.common.error;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import id.raisal.taskmanager.board.BoardController;
import id.raisal.taskmanager.support.FakeServicesConfiguration;
import id.raisal.taskmanager.task.TaskController;
import org.junit.jupiter.api.Test;
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
