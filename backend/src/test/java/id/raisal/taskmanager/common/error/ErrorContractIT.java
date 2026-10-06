package id.raisal.taskmanager.common.error;

import static org.assertj.core.api.Assertions.assertThat;

import id.raisal.taskmanager.TestcontainersConfiguration;
import id.raisal.taskmanager.support.TestDatabase;
import id.raisal.taskmanager.board.Board;
import id.raisal.taskmanager.board.BoardRepository;
import id.raisal.taskmanager.support.TestApi;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The error body on a real HTTP port, with PostgreSQL. It covers the paths that MockMvc does not use. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({TestcontainersConfiguration.class, ErrorContractIT.Probe.class})
class ErrorContractIT {

    /** Test-only endpoints that cause faults on purpose. They bypass the rule functions. */
    @RestController
    @RequestMapping("/probe")
    static class Probe {

        private final BoardRepository boards;
        private final TransactionTemplate transaction;

        Probe(BoardRepository boards, PlatformTransactionManager transactionManager) {
            this.boards = boards;
            this.transaction = new TransactionTemplate(transactionManager);
        }

        @GetMapping("/send-error")
        void sendError(HttpServletResponse response) throws IOException {
            response.sendError(500, "secret container detail");
        }

        @GetMapping("/boom")
        void boom() {
            throw new IllegalStateException("secret internal detail");
        }

        /** The name rule of the service is not used here, so that the database rejects the name. */
        @PostMapping("/board")
        void saveBoardWithoutRules(@RequestBody String name) {
            transaction.executeWithoutResult(status -> boards.save(new Board(null, name, Instant.now())));
        }
    }

    @LocalServerPort
    int port;

    @Autowired
    JdbcClient jdbc;

    TestApi api;

    @BeforeEach
    void setUp() {
        api = new TestApi(port);
        TestDatabase.clean(jdbc);
    }

    @Test
    void servletErrorUsesProblemDetails() {
        TestApi.Response response = api.get("/probe/send-error");

        assertErrorShape(response, 500, "INTERNAL_ERROR", "/probe/send-error");
        assertThat(response.<String>json("$.detail")).isEqualTo("An unexpected error occurred.");
        assertThat(response.body()).doesNotContain("secret container detail");
    }

    @Test
    void rejectedOriginUsesProblemDetails() {
        TestApi.Response response = api.get("/api/boards", Map.of("Origin", "http://evil.example"));

        assertErrorShape(response, 403, "CORS_REJECTED", "/api/boards");
        assertThat(response.headers().containsHeader("Access-Control-Allow-Origin")).isFalse();
    }

    @Test
    void allowedOriginStillGetsTheBoards() {
        TestApi.Response response = api.get("/api/boards", Map.of("Origin", "http://localhost:5173"));

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.headers().getFirst("Access-Control-Allow-Origin")).isEqualTo("http://localhost:5173");
    }

    @Test
    void namedBoardConstraintUsesNameField() {
        TestApi.Response response = api.send(HttpMethod.POST, "/probe/board", MediaType.TEXT_PLAIN, "  \t");

        assertErrorShape(response, 400, "VALIDATION_FAILED", "/probe/board");
        assertThat(response.<String>json("$.field")).isEqualTo("name");
        assertThat(response.<String>json("$.detail")).isEqualTo("Name is required.");
        assertThat(count("boards")).isZero();
    }

    @Test
    void unknownConstraintUsesInternalError() {
        // 101 characters: the database rejects the length, and no mapping names that rule.
        TestApi.Response response = api.send(HttpMethod.POST, "/probe/board", MediaType.TEXT_PLAIN, "x".repeat(101));

        assertErrorShape(response, 500, "INTERNAL_ERROR", "/probe/board");
        assertThat(response.<String>json("$.detail")).isEqualTo("An unexpected error occurred.");
        assertThat(response.body()).doesNotContain("varying").doesNotContain("boards");
        assertThat(count("boards")).isZero();
    }

    @Test
    void internalFaultHidesImplementationDetails() {
        TestApi.Response response = api.get("/probe/boom");

        assertErrorShape(response, 500, "INTERNAL_ERROR", "/probe/boom");
        assertThat(response.body()).doesNotContain("secret internal detail").doesNotContain("IllegalStateException");
    }

    @Test
    void currentEndpointsUseTheSameErrorShape() {
        assertErrorShape(api.postJson("/api/boards", "{bad"), 400, "MALFORMED_REQUEST", "/api/boards");
        assertErrorShape(api.get("/api/nothing-here"), 404, "NOT_FOUND", "/api/nothing-here");
        assertErrorShape(api.send(HttpMethod.PUT, "/api/boards", MediaType.APPLICATION_JSON, "{}"), 405, "METHOD_NOT_ALLOWED", "/api/boards");
        assertErrorShape(api.send(HttpMethod.POST, "/api/boards", MediaType.TEXT_PLAIN, "name=x"), 415, "UNSUPPORTED_MEDIA_TYPE", "/api/boards");
        assertErrorShape(api.postJson("/api/boards", "{\"name\":\"\"}"), 400, "VALIDATION_FAILED", "/api/boards");
    }

    private int count(String table) {
        return jdbc.sql("SELECT count(*) FROM " + table).query(Integer.class).single();
    }

    private static void assertErrorShape(TestApi.Response response, int status, String code, String instance) {
        assertThat(response.status()).isEqualTo(status);
        assertThat(response.headers().getContentType().isCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)).isTrue();
        Map<String, Object> body = response.json("$");
        assertThat(body).containsOnlyKeys("type", "title", "status", "detail", "instance", "code", "field");
        assertThat(body).containsEntry("type", "about:blank").containsEntry("status", status)
                .containsEntry("code", code).containsEntry("instance", instance);
        assertThat(body.get("title")).isEqualTo(org.springframework.http.HttpStatus.valueOf(status).getReasonPhrase());
    }
}
