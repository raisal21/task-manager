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
