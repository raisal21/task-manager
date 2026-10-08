package id.raisal.taskmanager;

import static org.assertj.core.api.Assertions.assertThat;

import id.raisal.taskmanager.support.TestApi;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

/** Checks the generated documentation on a real application, including schema resolution. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class OpenApiIT {

    @LocalServerPort
    int port;

    @Test
    void servesSwaggerAndDocumentsThePublicContractWithoutBrokenReferences() {
        var api = new TestApi(port);
        var page = api.get("/swagger-ui/index.html");
        assertThat(page.status()).isEqualTo(200);
        assertThat(page.body()).contains("Swagger UI");

        var spec = api.get("/v3/api-docs");
        assertThat(spec.status()).isEqualTo(200);
        Map<String, Map<String, Object>> paths = spec.json("$.paths");
        assertThat(paths.keySet()).containsExactlyInAnyOrder(
                "/api/boards", "/api/boards/{boardId}", "/api/boards/{boardId}/tasks", "/api/tasks/{taskId}");
        assertThat(paths.values().stream().mapToInt(Map::size).sum()).isEqualTo(7);

        Map<String, Object> schemas = spec.json("$.components.schemas");
        assertReferencesResolve(spec.json("$"), schemas);
        assertThat(schemas).containsKey("ErrorBody");

        List<String> patchFields = spec.json("$.components.schemas.UpdateTaskRequest.required");
        List<String> statuses = spec.json("$.components.schemas.UpdateTaskRequest.properties.status.enum");
        String example = spec.json("$.components.schemas.UpdateTaskRequest.properties.status.example");
        Boolean additionalProperties = spec.json("$.components.schemas.UpdateTaskRequest.additionalProperties");
        assertThat(patchFields).containsExactly("status");
        assertThat(statuses).containsExactly("TODO", "IN_PROGRESS", "DONE");
        assertThat(example).isEqualTo("IN_PROGRESS");
        assertThat(additionalProperties).isFalse();

        Integer forbiddenStatus = spec.json("$.paths['/api/boards'].get.responses['403'].content['application/problem+json'].example.status");
        Integer faultStatus = spec.json("$.paths['/api/boards'].get.responses['500'].content['application/problem+json'].example.status");
        assertThat(forbiddenStatus).isEqualTo(403);
        assertThat(faultStatus).isEqualTo(500);
        Map<String, Object> faultExample = spec.json("$.paths['/api/boards'].get.responses['500'].content['application/problem+json'].example");
        assertThat(faultExample).containsEntry("field", null);
    }

    private static void assertReferencesResolve(Object node, Map<String, Object> schemas) {
        String prefix = "#/components/schemas/";
        if (node instanceof Map<?, ?> map) {
            if (map.get("$ref") instanceof String reference && reference.startsWith(prefix)) {
                assertThat(schemas).containsKey(reference.substring(prefix.length()));
            }
            map.values().forEach(value -> assertReferencesResolve(value, schemas));
        } else if (node instanceof Iterable<?> values) {
            values.forEach(value -> assertReferencesResolve(value, schemas));
        }
    }
}
