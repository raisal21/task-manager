import { API_BASE_URL } from "./api/config";
import type { ApiError, TaskStatus } from "./api/types";
import { STATUS_LABELS } from "./taskStatus";

type WriteAction = "add board" | "add task" | "status change" | "delete";

export type StateMessageProps =
  | { state: "loading"; what: "boards" | "tasks" }
  | { state: "empty"; what: "boards" }
  | { state: "empty"; what: "tasks"; status?: TaskStatus }
  | { state: "read-error"; error: ApiError; onRetry: () => void }
  | { state: "write-error"; action: WriteAction; error: ApiError; id?: string }
  | { state: "validation"; text: string; id?: string };

/**
 * All state texts of the UI (R25) are in this file. A text says only what the
 * browser knows: what the server sent, or that it did not answer.
 */
export default function StateMessage(props: StateMessageProps) {
  switch (props.state) {
    case "loading":
      return <output>{`Loading ${props.what}…`}</output>;
    case "empty":
      return <p>{emptyText(props)}</p>;
    case "read-error":
      // Retry is only for a read that got an error. A read changes no data, so it is safe to send it again.
      // A POST, a status change, and a delete never get a Retry button (A17).
      return <ReadError text={readErrorText(props.error)} onRetry={props.onRetry} />;
    case "write-error":
      return <ErrorText id={props.id} text={writeErrorText(props.error, props.action)} />;
    case "validation":
      return <ErrorText id={props.id} text={props.text} />;
  }
}

function ReadError({ text, onRetry }: { text: string; onRetry: () => void }) {
  return (
    <div role="alert" className="message message-error message-with-action">
      <p>{text}</p>
      <button type="button" className="button-secondary" onClick={onRetry}>
        Retry
      </button>
    </div>
  );
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

/** A read that got an error. Without an answer, the URL or the CORS setting can be the cause. */
function readErrorText(error: ApiError): string {
  switch (error.kind) {
    case "network":
      return `Cannot reach the server at ${API_BASE_URL}. The server may be stopped, or the URL or CORS setting may be wrong.`;
    case "http":
      return serverErrorText(error);
    default:
      // An answer that the page cannot read. The message of the client says what it was.
      return `Something went wrong: ${withPeriod(error.message)}`;
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

/** "The server returned an error: {detail}." The text has only what the server sent. */
function serverErrorText(error: ApiError): string {
  return `The server returned an error: ${withPeriod(error.detail ?? `status ${error.status}`)}`;
}

/** The detail of the backend is a sentence with a period. Do not add a second one. */
function withPeriod(text: string): string {
  return text.endsWith(".") ? text : `${text}.`;
}
