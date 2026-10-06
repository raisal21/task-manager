package id.raisal.taskmanager.common.error;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Changes all exceptions into the one error body (ErrorBody). Mapping table:
 * <pre>
 * ValidationException                      400 VALIDATION_FAILED, field from the exception
 * HttpMessageNotReadableException          400 MALFORMED_REQUEST
 * HttpRequestMethodNotSupportedException   405 METHOD_NOT_ALLOWED (keeps the Allow header)
 * HttpMediaTypeNotSupportedException       415 UNSUPPORTED_MEDIA_TYPE (keeps the Accept header)
 * NoResourceFoundException                 404 NOT_FOUND
 * NotFoundException                        404 NOT_FOUND
 * Path or query value of a wrong type      400 VALIDATION_FAILED, field = the name of the parameter
 * Constraint boards_name_not_blank         400 VALIDATION_FAILED, field "name"
 * Constraint tasks_title_not_blank         400 VALIDATION_FAILED, field "title"
 * Constraint tasks_status_valid            400 VALIDATION_FAILED, field "status"
 * Other constraint violation               500 INTERNAL_ERROR
 * All other faults                         500 INTERNAL_ERROR, with a general message
 * </pre>
 * Add a row with each new operation (P4).
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    private record NamedConstraint(String field, String detail) {
    }

    /** Database constraints that a rule function also checks. The database is the backstop. */
    private static final Map<String, NamedConstraint> NAMED_CONSTRAINTS = Map.of(
            "boards_name_not_blank", new NamedConstraint("name", "Name is required."),
            "tasks_title_not_blank", new NamedConstraint("title", "Title is required."),
            "tasks_status_valid", new NamedConstraint("status", "Status must be TODO, IN_PROGRESS, or DONE."));

    static final String GENERAL_MESSAGE = "An unexpected error occurred.";

    private final ErrorWriter errors;

    public ApiExceptionHandler(ErrorWriter errors) {
        this.errors = errors;
    }

    @ExceptionHandler(ValidationException.class)
    ResponseEntity<ErrorBody> handleValidation(ValidationException exception, HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", exception.getMessage(), exception.getField(), request);
    }

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ErrorBody> handleNotFound(NotFoundException exception, HttpServletRequest request) {
        return respond(HttpStatus.NOT_FOUND, "NOT_FOUND", exception.getMessage(), null, request);
    }

    /**
     * Only the CHECK constraints have a mapping here. The foreign key fk_tasks_board means different things
     * for different operations, so the service of each operation translates it (task add: 404).
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorBody> handleConstraintViolation(DataIntegrityViolationException exception, HttpServletRequest request) {
        return constraintResponse(exception, request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorBody> handleOtherFault(Exception exception, HttpServletRequest request) {
        // A violation can also come in another wrapper, for example at flush or commit (P5). The cause chain tells.
        if (ConstraintNames.of(exception) != null) {
            return constraintResponse(exception, request);
        }
        log.error("Unhandled fault on {} {}", request.getMethod(), request.getRequestURI(), exception);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", GENERAL_MESSAGE, null, request);
    }

    private ResponseEntity<ErrorBody> constraintResponse(Exception exception, HttpServletRequest request) {
        String constraint = ConstraintNames.of(exception);
        NamedConstraint named = constraint == null ? null : NAMED_CONSTRAINTS.get(constraint);
        if (named == null) {
            log.error("Constraint violation without a mapping on {} {}", request.getMethod(), request.getRequestURI(), exception);
            return respond(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", GENERAL_MESSAGE, null, request);
        }
        return respond(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", named.detail(), named.field(), request);
    }

    /** Spring's own exceptions (405, 415, unreadable body, unknown path) all pass here. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception exception, Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ResponseEntity<Object> standard = super.handleExceptionInternal(exception, body, headers, status, request);
        String springDetail = standard != null && standard.getBody() instanceof ProblemDetail problem ? problem.getDetail() : null;
        String instance = ((ServletWebRequest) request).getRequest().getRequestURI();

        String field = null;
        String code = codeFor(exception, status);
        if (exception instanceof TypeMismatchException mismatch) {
            // For example GET /api/boards/abc/tasks: the path value is not a number (P3).
            code = "VALIDATION_FAILED";
            field = mismatch.getPropertyName();
        }
        String detail = switch (code) {
            case "MALFORMED_REQUEST" -> "The request body is missing or is not valid JSON.";
            case "NOT_FOUND" -> "There is no endpoint for this path.";
            case "VALIDATION_FAILED" -> "The value of '" + field + "' is not correct.";
            default -> springDetail != null ? springDetail : GENERAL_MESSAGE;
        };
        if (status.is5xxServerError()) {
            log.error("Framework fault on {}", instance, exception);
            detail = GENERAL_MESSAGE;
        }

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.putAll(headers);
        responseHeaders.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        return new ResponseEntity<>(errors.body(status, code, detail, field, instance), responseHeaders, status);
    }

    /** Most codes follow the status: 404 NOT_FOUND, 405 METHOD_NOT_ALLOWED, 415 UNSUPPORTED_MEDIA_TYPE, 5xx INTERNAL_ERROR. */
    private static String codeFor(Exception exception, HttpStatusCode status) {
        if (exception instanceof HttpMessageNotReadableException) {
            return "MALFORMED_REQUEST";
        }
        return ErrorWriter.codeForStatus(status);
    }

    private ResponseEntity<ErrorBody> respond(
            HttpStatus status, String code, String detail, String field, HttpServletRequest request) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(errors.body(status, code, detail, field, request.getRequestURI()));
    }
}
