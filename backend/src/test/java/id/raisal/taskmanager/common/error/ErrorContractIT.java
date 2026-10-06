package id.raisal.taskmanager.common.error;

import static org.assertj.core.api.Assertions.assertThat;

import id.raisal.taskmanager.TestcontainersConfiguration;
import id.raisal.taskmanager.support.TestDatabase;
import id.raisal.taskmanager.board.Board;
import id.raisal.taskmanager.board.BoardRepository;
import id.raisal.taskmanager.support.PauseGate;
import id.raisal.taskmanager.task.TaskRepository;
import id.raisal.taskmanager.support.TestApi;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.io.IOException;
import java.time.Instant;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The error body on a real HTTP port, with PostgreSQL. It covers the paths that MockMvc does not use. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({TestcontainersConfiguration.class, ErrorContractIT.Probe.class, ErrorContractIT.Gates.class})
class ErrorContractIT {

    /** Race tests (section 7.10) use this gate. It is in test sources only. */
    static final PauseGate GATE = new PauseGate();

    /** Wraps the repositories, so that a test can stop a service after one of its repository calls. */
    @TestConfiguration
    static class Gates {

        @Bean
        static BeanPostProcessor pausingBoardRepository() {
            return new BeanPostProcessor() {
                @Override
                public Object postProcessAfterInitialization(Object bean, String beanName) {
                    if (bean instanceof BoardRepository repository) {
                        return PauseGate.wrap(BoardRepository.class, repository, GATE);
                    }
                    if (bean instanceof TaskRepository repository) {
                        return PauseGate.wrap(TaskRepository.class, repository, GATE);
                    }
                    return bean;
                }
            };
        }
    }

    /** Test-only endpoints that cause faults on purpose. They bypass the rule functions. */
    @RestController
    @RequestMapping("/probe")
    static class Probe {

        private final BoardRepository boards;
        private final EntityManager entityManager;
        private final TransactionTemplate transaction;

        Probe(BoardRepository boards, EntityManager entityManager, PlatformTransactionManager transactionManager) {
            this.boards = boards;
            this.entityManager = entityManager;
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

        /**
         * A task row with any title and status, through SQL in a transaction. TaskRules and the TaskStatus enum
         * cannot make such a row, so that the database rejects it. The exception is translated like in a Spring Data repository.
         */
        @PostMapping("/task")
        void saveTaskWithoutRules(@RequestParam long boardId, @RequestParam String status, @RequestBody String title) {
            try {
                transaction.executeWithoutResult(state -> entityManager
                        .createNativeQuery("INSERT INTO tasks (board_id, title, status) VALUES (:board, :title, :status)")
                        .setParameter("board", boardId)
                        .setParameter("title", title)
                        .setParameter("status", status)
                        .executeUpdate());
            } catch (PersistenceException exception) {
                DataAccessException translated = EntityManagerFactoryUtils.convertJpaAccessExceptionIfPossible(exception);
                throw translated != null ? translated : exception;
            }
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

    @Test
    void taskTitleConstraintUsesTitleField() {
        long boardId = insertBoard();

        TestApi.Response response = api.send(HttpMethod.POST, "/probe/task?boardId=" + boardId + "&status=TODO", MediaType.TEXT_PLAIN, "  \t");

        assertErrorShape(response, 400, "VALIDATION_FAILED", "/probe/task");
        assertThat(response.<String>json("$.field")).isEqualTo("title");
        assertThat(response.<String>json("$.detail")).isEqualTo("Title is required.");
        assertThat(count("tasks")).isZero();
    }

    @Test
    void taskStatusConstraintUsesStatusField() {
        long boardId = insertBoard();

        TestApi.Response response = api.send(HttpMethod.POST, "/probe/task?boardId=" + boardId + "&status=BLOCKED", MediaType.TEXT_PLAIN, "Title");

        assertErrorShape(response, 400, "VALIDATION_FAILED", "/probe/task");
        assertThat(response.<String>json("$.field")).isEqualTo("status");
        assertThat(response.<String>json("$.detail")).isEqualTo("Status must be TODO, IN_PROGRESS, or DONE.");
        assertThat(count("tasks")).isZero();
    }

    @Test
    void taskAddRaceMapsToNotFound() throws Exception {
        // Section 7.10. The service finds the board. Then a second transaction deletes the board and commits.
        // Then the first operation continues, and its insert breaks the foreign key.
        long boardId = insertBoard();
        String path = "/api/boards/" + boardId + "/tasks";
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            GATE.arm("findById");
            Future<TestApi.Response> request = executor.submit(() -> api.postJson(path, "{\"title\":\"Race\"}"));

            assertThat(GATE.awaitPaused(Duration.ofSeconds(10))).as("the service reached the pause point after the board lookup").isTrue();
            assertThat(jdbc.sql("DELETE FROM boards WHERE id = ?").param(boardId).update()).isEqualTo(1);
            GATE.release();

            TestApi.Response response = request.get(10, TimeUnit.SECONDS);
            assertErrorShape(response, 404, "NOT_FOUND", path);
            assertThat(response.<String>json("$.detail")).isEqualTo("There is no board with ID " + boardId + ".");
            assertThat(count("tasks")).isZero();
            assertThat(count("boards")).isZero();
        } finally {
            GATE.release();
            executor.shutdownNow();
        }
    }

    @Test
    void deleteRaceMapsToBoardNotEmpty() throws Exception {
        // Section 7.10. The service finds no task on the board. Then a second transaction adds a task and commits.
        // Then the first operation continues, and its DELETE breaks the foreign key (ON DELETE RESTRICT).
        long boardId = insertBoard();
        String path = "/api/boards/" + boardId;
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            GATE.arm("existsByBoardId");
            Future<TestApi.Response> request = executor.submit(() -> api.delete(path));

            assertThat(GATE.awaitPaused(Duration.ofSeconds(10))).as("the service reached the pause point after the task check").isTrue();
            assertThat(count("tasks")).isZero();
            assertThat(jdbc.sql("INSERT INTO tasks (board_id, title) VALUES (?, 'Raced')").param(boardId).update()).isEqualTo(1);
            GATE.release();

            TestApi.Response response = request.get(10, TimeUnit.SECONDS);
            assertErrorShape(response, 409, "BOARD_NOT_EMPTY", path);
            assertThat(response.<String>json("$.detail")).isEqualTo("Board " + boardId + " has tasks. Delete its tasks first.");
            assertThat(count("boards")).isEqualTo(1);
            assertThat(count("tasks")).isEqualTo(1);
        } finally {
            GATE.release();
            executor.shutdownNow();
        }
    }

    private long insertBoard() {
        return jdbc.sql("INSERT INTO boards (name) VALUES ('Board') RETURNING id").query(Long.class).single();
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
