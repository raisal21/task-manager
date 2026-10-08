package id.raisal.taskmanager.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The one error body of the API (RFC 9457 Problem Details, with the extension members code and field).
 * type is always "about:blank", so title is the HTTP status phrase. code says what happened.
 * field names the input field when the failure is about one field, and it is null in all other cases.
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
@Schema(description = "RFC 9457 Problem Details with the stable code and optional field name",
        requiredProperties = {"type", "title", "status", "detail", "instance", "code", "field"})
public record ErrorBody(
        @Schema(example = "about:blank") String type,
        @Schema(example = "Bad Request") String title,
        @Schema(example = "400") int status,
        @Schema(example = "Name is required.") String detail,
        @Schema(example = "/api/boards") String instance,
        @Schema(example = "VALIDATION_FAILED") String code,
        @Schema(description = "Input field when relevant; otherwise null", example = "name", nullable = true) String field) {
}
