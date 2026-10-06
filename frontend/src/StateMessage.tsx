import { API_BASE_URL } from "./api/config";
import type { ApiError, TaskStatus } from "./api/types";
import { STATUS_LABELS } from "./taskStatus";

type WriteAction = "add board" | "add task" | "status change" | "delete";

export type StateMessageProps =
  | { state: "loading"; what: "boards" | "tasks" }
  | { state: "empty"; what: "boards" }
  | { state: "empty"; what: "tasks"; status?: TaskStatus }
  | { state: "read-error"; error: ApiError }
  | { state: "write-error"; action: WriteAction; error: ApiError; id?: string }
  | { state: "validation"; text: string; id?: string };

/**
 * All state texts of the UI (R25, section 7.7 of the plan) are in this file. A text says only what the
 * browser knows: what the server sent, or that it did not answer.
 */
export default function StateMessage(props: StateMessageProps) {
  switch (props.state) {
    case "loading":
      return <output>{`Loading ${props.what}…`}</output>;
    case "empty":
      return <p>{emptyText(props)}</p>;
    case "read-error":
      return <ErrorText text={readErrorText(props.error)} />;
    case "write-error":
      return <ErrorText id={props.id} text={writeErrorText(props.error, props.action)} />;
    case "validation":
      return <ErrorText id={props.id} text={props.text} />;
  }
}

function ErrorText({ text, id }: { text: string; id?: string }) {
  return (
    <p id={id} role="alert" className="message message-error">
      {text}
    </p>
  );
}

function emptyText(props: { what: "boards" | "tasks"; status?: TaskStatus }): string {
  if (props.what === "boards") {
    return "No boards yet. Create one to start.";
  }
  return props.status === undefined
    ? "No tasks yet."
    : `No tasks with the status ${STATUS_LABELS[props.status]}.`;
}

/** A read that got an error. Without an answer, the URL or the CORS setting can be the cause (section 7.7). */
function readErrorText(error: ApiError): string {
  switch (error.kind) {
    case "network":
      return `Cannot reach the server at ${API_BASE_URL}. The server may be stopped, or the URL or CORS setting may be wrong.`;
    case "http":
      return serverErrorText(error);
    default:
      return `Something went wrong: ${error.message}.`;
  }
}

/**
 * A write that got an error. A POST without an answer can be in the database (A17), so the text says that and
 * tells the user to reload the list before a new submit. A status change or a delete is the same.
 */
function writeErrorText(error: ApiError, action: WriteAction): string {
  if (error.kind === "http") {
    return serverErrorText(error);
  }
  switch (action) {
    case "add board":
      return "The server may have saved this board. Reload the list before you send it again.";
    case "add task":
      return "The server may have saved this task. Reload the list before you send it again.";
    default:
      return `The server did not answer, so the ${action} may or may not be saved. Reload the list to check.`;
  }
}

/** "The server returned an error: {detail}." The detail of the backend is a sentence, so do not add a second period. */
function serverErrorText(error: ApiError): string {
  const detail = error.detail ?? `status ${error.status}`;
  return `The server returned an error: ${detail}${detail.endsWith(".") ? "" : "."}`;
}
