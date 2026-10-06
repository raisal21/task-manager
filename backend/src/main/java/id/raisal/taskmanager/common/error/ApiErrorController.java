package id.raisal.taskmanager.common.error;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * The servlet container sends errors here that Spring MVC did not handle, for example a bad request line
 * or a call of sendError. It replaces the default error controller of Spring Boot.
 */
@Controller
public class ApiErrorController implements ErrorController {

    private final ErrorWriter errors;

    public ApiErrorController(ErrorWriter errors) {
        this.errors = errors;
    }

    @RequestMapping("/error")
    public ResponseEntity<ErrorBody> error(HttpServletRequest request) {
        Object statusAttribute = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        // A direct call of /error has no error attributes. Show it as an unknown path.
        HttpStatusCode status = statusAttribute instanceof Integer value ? HttpStatusCode.valueOf(value) : HttpStatus.NOT_FOUND;
        Object original = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        String instance = original instanceof String uri ? uri : request.getRequestURI();

        String code = status.value() == 400 ? "MALFORMED_REQUEST" : ErrorWriter.codeForStatus(status);
        String detail = status.is5xxServerError()
                ? ApiExceptionHandler.GENERAL_MESSAGE
                : status.value() == 404 ? "There is no endpoint for this path." : "The request could not be processed.";
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(errors.body(status, code, detail, null, instance));
    }
}
