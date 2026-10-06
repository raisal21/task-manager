package id.raisal.taskmanager.common.error;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** The one place that makes the error body. All error paths use it, also those outside the controllers. */
@Component
public class ErrorWriter {

    private final JsonMapper mapper;

    public ErrorWriter(JsonMapper mapper) {
        this.mapper = mapper;
    }

    public ErrorBody body(HttpStatusCode status, String code, String detail, String field, String instance) {
        return new ErrorBody("about:blank", reasonPhrase(status), status.value(), detail, instance, code, field);
    }

    /** For a path that has no controller, for example a filter that rejects a request. */
    public void write(HttpServletResponse response, ErrorBody body) throws IOException {
        response.setStatus(body.status());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(), body);
        response.flushBuffer();
    }

    /** The code for a status that has no more specific code. All server faults use INTERNAL_ERROR. */
    public static String codeForStatus(HttpStatusCode status) {
        if (status.is5xxServerError()) {
            return "INTERNAL_ERROR";
        }
        HttpStatus known = HttpStatus.resolve(status.value());
        return known == null ? "ERROR" : known.name();
    }

    private static String reasonPhrase(HttpStatusCode status) {
        HttpStatus known = HttpStatus.resolve(status.value());
        return known == null ? "Error" : known.getReasonPhrase();
    }
}
