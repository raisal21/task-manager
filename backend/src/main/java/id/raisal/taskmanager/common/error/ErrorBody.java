package id.raisal.taskmanager.common.error;

/**
 * The one error body of the API (RFC 9457 Problem Details, with the extension members code and field).
 * type is always "about:blank", so title is the HTTP status phrase. code says what happened.
 * field names the input field when the failure is about one field, and it is null in all other cases.
 */
public record ErrorBody(String type, String title, int status, String detail, String instance, String code, String field) {
}
