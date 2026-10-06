package id.raisal.taskmanager.common.error;

/** A rule function rejected a value. The error body names the field and gives the message. */
public class ValidationException extends RuntimeException {

    private final String field;

    public ValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
