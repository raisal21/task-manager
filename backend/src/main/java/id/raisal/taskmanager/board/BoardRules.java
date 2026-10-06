package id.raisal.taskmanager.board;

import id.raisal.taskmanager.common.error.ValidationException;

/** Rule functions for boards. They have no HTTP types and no database access. */
public final class BoardRules {

    public static final int NAME_MAX_LENGTH = 100;

    private BoardRules() {
    }

    /** A6: remove the spaces at the two ends, reject an empty name, and reject a name that is too long. */
    public static String name(String raw) {
        String name = raw == null ? "" : raw.strip();
        if (name.isEmpty()) {
            throw new ValidationException("name", "Name is required.");
        }
        // The database counts characters, not UTF-16 units, so the rule counts code points.
        if (name.codePointCount(0, name.length()) > NAME_MAX_LENGTH) {
            throw new ValidationException("name", "Name must have " + NAME_MAX_LENGTH + " characters or fewer.");
        }
        return name;
    }
}
