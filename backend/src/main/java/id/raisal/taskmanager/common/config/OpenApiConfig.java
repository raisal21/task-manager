package id.raisal.taskmanager.common.config;

import id.raisal.taskmanager.common.error.ErrorBody;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI taskManagerApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Task Manager API")
                        .version("1.0")
                        .description("Boards and tasks stored in PostgreSQL. Use Try it out to call the running API. "
                                + "Changes are shared with the frontend and pgAdmin. "
                                + "All failures return application/problem+json with code and field."));
    }

    @Bean
    OpenApiCustomizer errorResponses() {
        // Keep the inferred success schemas. Describe only failures that apply to each operation.
        return api -> {
            // Add this schema with its references, after springdoc removes unused definitions.
            ModelConverters.getInstance(true).read(ErrorBody.class).forEach(api.getComponents()::addSchemas);
            api.getPaths().forEach((path, item) -> item.readOperationsMap().forEach((method, operation) -> {
                var responses = operation.getResponses();
                responses.addApiResponse("403", error(403, "The browser origin is not allowed (CORS_REJECTED).", path));
                responses.addApiResponse("500", error(500, "Unexpected server fault (INTERNAL_ERROR).", path));

                if (!(method == PathItem.HttpMethod.GET && path.equals("/api/boards"))) {
                    responses.addApiResponse("400", error(400, "Invalid input or malformed JSON (VALIDATION_FAILED or MALFORMED_REQUEST).", path));
                }
                if (path.contains("{boardId}") || path.contains("{taskId}")) {
                    responses.addApiResponse("404", error(404, "The board or task does not exist (NOT_FOUND).", path));
                }
                if (method == PathItem.HttpMethod.DELETE && path.equals("/api/boards/{boardId}")) {
                    responses.addApiResponse("409", error(409, "The board still has tasks (BOARD_NOT_EMPTY). Delete its tasks first.", path));
                }
                if (method == PathItem.HttpMethod.POST || method == PathItem.HttpMethod.PATCH) {
                    responses.addApiResponse("415", error(415, "The request body must use Content-Type: application/json (UNSUPPORTED_MEDIA_TYPE).", path));
                }
            }));
        };
    }

    private static ApiResponse error(int status, String description, String path) {
        String field = status == 400
                ? path.contains("{boardId}") ? "boardId" : path.contains("{taskId}") ? "taskId" : "name"
                : null;
        String id = status == 400 ? "abc" : status == 404 ? "999999" : "1";
        String instance = path.replace("{boardId}", id).replace("{taskId}", id);
        String code = switch (status) {
            case 400 -> "VALIDATION_FAILED";
            case 403 -> "CORS_REJECTED";
            case 404 -> "NOT_FOUND";
            case 409 -> "BOARD_NOT_EMPTY";
            case 415 -> "UNSUPPORTED_MEDIA_TYPE";
            default -> "INTERNAL_ERROR";
        };
        String detail = switch (status) {
            case 400 -> "name".equals(field) ? "Name is required." : "The value of '" + field + "' is not correct.";
            case 403 -> "The origin of the request is not allowed.";
            case 404 -> "There is no " + (path.contains("{taskId}") ? "task" : "board") + " with ID 999999.";
            case 409 -> "Board 1 has tasks. Delete its tasks first.";
            case 415 -> "Content-Type 'text/plain' is not supported.";
            default -> "An unexpected error occurred.";
        };
        var example = new ErrorBody("about:blank", HttpStatus.valueOf(status).getReasonPhrase(), status,
                detail, instance, code, field);
        return new ApiResponse().description(description).content(new Content()
                .addMediaType("application/problem+json", new MediaType()
                        .schema(new Schema<>().$ref("#/components/schemas/ErrorBody"))
                        .example(example)));
    }
}
