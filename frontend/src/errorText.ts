import { API_BASE_URL } from "./api/config";
import type { ApiError } from "./api/types";

/** The text for a failed read, the same for all lists (R25). */
export function readErrorText(error: ApiError): string {
  switch (error.kind) {
    case "network":
      return `Cannot reach the server at ${API_BASE_URL}. The server may be stopped, or the URL or CORS setting may be wrong.`;
    case "http":
      return serverErrorText(error);
    default:
      return `Something went wrong: ${error.message}.`;
  }
}

/** "The server returned an error: {detail}." The detail of the backend is a sentence, so do not add a second period. */
export function serverErrorText(error: ApiError): string {
  const detail = error.detail ?? `status ${error.status}`;
  return `The server returned an error: ${detail}${detail.endsWith(".") ? "" : "."}`;
}

/**
 * The text for a failed write on a task row (a status change or a delete). The row keeps its last status from the server.
 * Without an answer, the write can be in the database, so the text says that and tells the user to reload the list.
 */
export function writeErrorText(error: ApiError, action: "status change" | "delete"): string {
  if (error.kind === "http") {
    return serverErrorText(error);
  }
  return `The server did not answer, so the ${action} may or may not be saved. Reload the list to check.`;
}
