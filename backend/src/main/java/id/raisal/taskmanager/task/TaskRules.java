package id.raisal.taskmanager.task;

import id.raisal.taskmanager.common.error.ValidationException;

/** Rule functions for tasks. They have no HTTP types and no database access. */
public final class TaskRules {

    public static final int TITLE_MAX_LENGTH = 200;
    public static final int DESCRIPTION_MAX_LENGTH = 2000;

    private TaskRules() {
    }

    /** A6: remove the spaces at the two ends, reject an empty title, and reject a title that is too long. */
    public static String title(String raw) {
        String title = raw == null ? "" : raw.strip();
        if (title.isEmpty()) {
            throw new ValidationException("title", "Title is required.");
        }
        requireMaxLength("title", "Title", title, TITLE_MAX_LENGTH);
        return title;
    }

    /** A6 and A10: remove the spaces at the two ends. A missing or empty description becomes null. */
    public static String description(String raw) {
        if (raw == null) {
            return null;
        }
        String description = raw.strip();
        if (description.isEmpty()) {
            return null;
        }
        requireMaxLength("description", "Description", description, DESCRIPTION_MAX_LENGTH);
        return description;
    }

    // The database counts characters, not UTF-16 units, so the rule counts code points.
    private static void requireMaxLength(String field, String label, String value, int max) {
        if (value.codePointCount(0, value.length()) > max) {
            throw new ValidationException(field, label + " must have " + max + " characters or fewer.");
        }
    }
}
