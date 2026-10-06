package id.raisal.taskmanager.common.error;

/** The resource that the request names is not there. The error body gives 404 NOT_FOUND. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
