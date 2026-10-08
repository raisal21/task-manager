package id.raisal.taskmanager.task;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;

/**
 * The body of PATCH /api/tasks/{taskId}: a JSON object with one field, status (A7).
 * Spring Boot switches off FAIL_ON_UNKNOWN_PROPERTIES for all requests, so that Jackson does not reject other
 * fields. This class collects them, and TaskRules.statusChange rejects them. A body that is not an object
 * or is not JSON never gets here: Jackson rejects it, and the error handler sends MALFORMED_REQUEST.
 */
@Schema(additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public class UpdateTaskRequest {

    private String status;
    private final List<String> unknownFields = new ArrayList<>();

    @JsonSetter("status")
    @Schema(description = "The only field accepted by PATCH", example = "IN_PROGRESS",
            requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"TODO", "IN_PROGRESS", "DONE"})
    public void setStatus(String status) {
        this.status = status;
    }

    @JsonAnySetter
    public void addUnknownField(String name, Object value) {
        unknownFields.add(name);
    }

    /** Null when the field is missing or null. */
    public String status() {
        return status;
    }

    public List<String> unknownFields() {
        return List.copyOf(unknownFields);
    }
}
