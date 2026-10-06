package id.raisal.taskmanager;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/** One disposable PostgreSQL container for each Spring test context. Same major version as compose.yaml. */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    public static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:18");

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(POSTGRES_IMAGE);
    }
}
