package id.raisal.taskmanager.task;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import java.util.ArrayList;
import java.util.List;

/**
 * The body of PATCH /api/tasks/{taskId}: a JSON object with one field, status (A7).
 * Spring Boot switches off FAIL_ON_UNKNOWN_PROPERTIES for all requests, so that Jackson does not reject other
 * fields. This class collects them, and TaskRules.statusChange rejects them (P10). A body that is not an object
 * or is not JSON never gets here: Jackson rejects it, and the error handler sends MALFORMED_REQUEST.
 */
public class UpdateTaskRequest {

    private String status;
    private final List<String> unknownFields = new ArrayList<>();

    @JsonSetter("status")
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
