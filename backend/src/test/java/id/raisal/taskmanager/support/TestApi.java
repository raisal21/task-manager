package id.raisal.taskmanager.support;

import com.jayway.jsonpath.JsonPath;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/** A small HTTP client for tests with a random port. It also reads 4xx and 5xx responses without an exception. */
public final class TestApi {

    public record Response(int status, HttpHeaders headers, String body) {

        public <T> T json(String path) {
            return JsonPath.read(body, path);
        }
    }

    private final RestClient client;

    public TestApi(int port) {
        this.client = RestClient.create("http://localhost:" + port);
    }

    public Response get(String path) {
        return send(HttpMethod.GET, path, null, null, Map.of());
    }

    public Response get(String path, Map<String, String> headers) {
        return send(HttpMethod.GET, path, null, null, headers);
    }

    public Response postJson(String path, String json) {
        return send(HttpMethod.POST, path, MediaType.APPLICATION_JSON, json, Map.of());
    }

    public Response delete(String path) {
        return send(HttpMethod.DELETE, path, null, null, Map.of());
    }

    public Response patchJson(String path, String json) {
        return send(HttpMethod.PATCH, path, MediaType.APPLICATION_JSON, json, Map.of());
    }

    public Response send(HttpMethod method, String path, MediaType contentType, String body) {
        return send(method, path, contentType, body, Map.of());
    }

    public Response send(HttpMethod method, String path, MediaType contentType, String body, Map<String, String> headers) {
        RestClient.RequestBodySpec request = client.method(method).uri(path);
        headers.forEach(request::header);
        if (contentType != null) {
            request.contentType(contentType);
        }
        if (body != null) {
            request.body(body);
        }
        return request.exchange((req, res) -> new Response(
                res.getStatusCode().value(), res.getHeaders(), read(res.getBody())));
    }

    private static String read(java.io.InputStream stream) throws IOException {
        return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
}
