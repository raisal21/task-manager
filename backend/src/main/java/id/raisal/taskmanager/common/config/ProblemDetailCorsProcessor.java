package id.raisal.taskmanager.common.config;

import id.raisal.taskmanager.common.error.ErrorWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsProcessor;
import org.springframework.web.cors.DefaultCorsProcessor;

/** The standard CORS processor, but a rejected origin gets the one error body and not plain text (R16, R18). */
class ProblemDetailCorsProcessor implements CorsProcessor {

    private final ErrorWriter errors;

    private final DefaultCorsProcessor delegate = new DefaultCorsProcessor() {
        @Override
        protected void rejectRequest(ServerHttpResponse response) {
            // The status only. The body comes from the error writer below.
            response.setStatusCode(HttpStatus.FORBIDDEN);
        }
    };

    ProblemDetailCorsProcessor(ErrorWriter errors) {
        this.errors = errors;
    }

    @Override
    public boolean processRequest(CorsConfiguration config, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        boolean continueChain = delegate.processRequest(config, request, response);
        // A handled preflight request also ends the chain, but with status 200. Only a rejection has 403.
        if (!continueChain && response.getStatus() == HttpStatus.FORBIDDEN.value()) {
            errors.write(response, errors.body(
                    HttpStatus.FORBIDDEN, "CORS_REJECTED", "The origin of the request is not allowed.", null, request.getRequestURI()));
        }
        return continueChain;
    }
}
