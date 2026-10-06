package id.raisal.taskmanager.common.error;

import java.sql.SQLException;
import org.hibernate.exception.ConstraintViolationException;

/** Reads which database constraint an exception violated. */
public final class ConstraintNames {

    private static final String FOREIGN_KEY_VIOLATION = "23503";
    private static final String RESTRICT_VIOLATION = "23001";

    private ConstraintNames() {
    }

    /** The name of the violated constraint. Null if there is no name. */
    public static String of(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation && violation.getConstraintName() != null) {
                return violation.getConstraintName();
            }
        }
        return null;
    }

    /**
     * True if the exception is a violation of this foreign key. Hibernate cannot read the name of a RESTRICT violation
     * (SQLState 23001, for example a board DELETE that still has tasks). If there is no name, the SQLState tells
     * that a foreign key blocked the statement. The caller names the operation, and the operation tells which key it was.
     */
    public static boolean isForeignKeyViolation(Throwable exception, String foreignKey) {
        String name = of(exception);
        if (name != null) {
            return name.equals(foreignKey);
        }
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sql
                    && (FOREIGN_KEY_VIOLATION.equals(sql.getSQLState()) || RESTRICT_VIOLATION.equals(sql.getSQLState()))) {
                return true;
            }
        }
        return false;
    }
}
