package id.raisal.taskmanager.task;

import id.raisal.taskmanager.common.error.ValidationException;

public enum TaskStatus {
    TODO,
    IN_PROGRESS,
    DONE;

    /** A1: an unknown value is a validation error for the field "status". The match is exact. */
    public static TaskStatus parse(String value) {
        for (TaskStatus status : values()) {
            if (status.name().equals(value)) {
                return status;
            }
        }
        throw new ValidationException("status", "Status must be TODO, IN_PROGRESS, or DONE.");
    }
}
