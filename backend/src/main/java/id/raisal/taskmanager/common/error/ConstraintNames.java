package id.raisal.taskmanager.common.error;

import org.hibernate.exception.ConstraintViolationException;

/** Reads the name of the violated database constraint from an exception. Null if there is no name. */
public final class ConstraintNames {

    private ConstraintNames() {
    }

    public static String of(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation && violation.getConstraintName() != null) {
                return violation.getConstraintName();
            }
        }
        return null;
    }
}
