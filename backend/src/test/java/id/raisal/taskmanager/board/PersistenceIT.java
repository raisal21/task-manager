package id.raisal.taskmanager.board;

import static org.assertj.core.api.Assertions.assertThat;

import id.raisal.taskmanager.TaskManagerApplication;
import id.raisal.taskmanager.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** G3: the data is in the database, not in the memory of the application. */
class PersistenceIT {

    @Test
    void keepsBoardsAfterContextRestart() {
        try (PostgreSQLContainer postgres = new PostgreSQLContainer(TestcontainersConfiguration.POSTGRES_IMAGE)) {
            postgres.start();

            try (ConfigurableApplicationContext first = start(postgres)) {
                first.getBean(JdbcClient.class)
                        .sql("INSERT INTO boards (name) VALUES (?)")
                        .param("Kept board")
                        .update();
            }

            try (ConfigurableApplicationContext second = start(postgres)) {
                assertThat(second.getBean(BoardService.class).listBoards())
                        .extracting(Board::getName)
                        .containsExactly("Kept board");
            }
        }
    }

    private static ConfigurableApplicationContext start(PostgreSQLContainer postgres) {
        return new SpringApplicationBuilder(TaskManagerApplication.class)
                .web(WebApplicationType.NONE)
                .run(
                        "--spring.datasource.url=" + postgres.getJdbcUrl(),
                        "--spring.datasource.username=" + postgres.getUsername(),
                        "--spring.datasource.password=" + postgres.getPassword());
    }
}
