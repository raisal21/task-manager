package id.raisal.taskmanager.board;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import id.raisal.taskmanager.TestcontainersConfiguration;
import id.raisal.taskmanager.support.TestDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;

/** The schema rules of the boards table, through SQL without the API. Each statement commits on its own. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class BoardSchemaIT {

    @Autowired
    JdbcClient jdbc;

    @BeforeEach
    void cleanBoards() {
        TestDatabase.clean(jdbc);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   ", "\t", " \t\n "})
    void rejectsBlankName(String name) {
        assertThatThrownBy(() -> insert(name))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("boards_name_not_blank");
        assertThat(countBoards()).isZero();
    }

    @Test
    void rejectsMissingName() {
        assertThatThrownBy(() -> insert(null)).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(countBoards()).isZero();
    }

    @Test
    void rejectsNameLongerThan100Characters() {
        assertThatThrownBy(() -> insert("x".repeat(101))).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(countBoards()).isZero();
    }

    @Test
    void acceptsNameWithOneVisibleCharacterAndTheLimit() {
        insert("x");
        insert("y".repeat(100));
        insert(" padded name ");

        assertThat(countBoards()).isEqualTo(3);
    }

    private void insert(String name) {
        jdbc.sql("INSERT INTO boards (name) VALUES (?)").param(name).update();
    }

    private int countBoards() {
        return jdbc.sql("SELECT count(*) FROM boards").query(Integer.class).single();
    }
}
