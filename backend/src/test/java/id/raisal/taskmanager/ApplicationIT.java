package id.raisal.taskmanager;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ApplicationIT {

    @Autowired
    JdbcClient jdbc;

    @Test
    void contextStartsWithTheFlywaySchema() {
        Integer boardColumns = jdbc.sql("""
                SELECT count(*) FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name = 'boards'
                """).query(Integer.class).single();

        assertThat(boardColumns).isEqualTo(3);
    }
}
