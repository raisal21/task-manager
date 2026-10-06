package id.raisal.taskmanager.common.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class ConstraintNamesTest {

    private static final String FK = "fk_tasks_board";

    private static DataIntegrityViolationException violation(String sqlState, String constraintName) {
        SQLException sql = new SQLException("database message", sqlState);
        return new DataIntegrityViolationException("wrapped", new ConstraintViolationException("hibernate message", sql, constraintName));
    }

    @Test
    void readsTheNameFromTheCauseChain() {
        assertThat(ConstraintNames.of(violation("23514", "tasks_status_valid"))).isEqualTo("tasks_status_valid");
        assertThat(ConstraintNames.of(new RuntimeException("no database cause"))).isNull();
    }

    @Test
    void knowsTheForeignKeyByItsName() {
        assertThat(ConstraintNames.isForeignKeyViolation(violation("23503", FK), FK)).isTrue();
        assertThat(ConstraintNames.isForeignKeyViolation(violation("23503", "other_fk"), FK)).isFalse();
        // A name for another constraint wins over the SQLState.
        assertThat(ConstraintNames.isForeignKeyViolation(violation("23503", "tasks_title_not_blank"), FK)).isFalse();
    }

    @Test
    void usesTheSqlStateWhenHibernateGivesNoName() {
        // A RESTRICT violation (a board delete with tasks) has no name for Hibernate. The SQLState tells.
        assertThat(ConstraintNames.isForeignKeyViolation(violation("23001", null), FK)).isTrue();
        assertThat(ConstraintNames.isForeignKeyViolation(violation("23503", null), FK)).isTrue();
    }

    @Test
    void doesNotTakeOtherViolationsForTheForeignKey() {
        assertThat(ConstraintNames.isForeignKeyViolation(violation("23505", null), FK)).isFalse();
        assertThat(ConstraintNames.isForeignKeyViolation(violation("23514", null), FK)).isFalse();
        assertThat(ConstraintNames.isForeignKeyViolation(violation("22001", null), FK)).isFalse();
        assertThat(ConstraintNames.isForeignKeyViolation(new RuntimeException("no database cause"), FK)).isFalse();
    }
}
