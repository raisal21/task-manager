import { useRef, useState, type SubmitEvent } from "react";
import { createBoard, toApiError } from "./api/client";
import type { ApiError } from "./api/types";
import { serverErrorText } from "./errorText";

interface BoardFormProps {
  onAdded: () => void;
}

// A17: without a response, the board can be in the database. The form never sends the POST again by itself.
const UNCERTAIN_POST_MESSAGE =
  "The server may have saved this board. Reload the list before you send it again.";

function submitErrorText(error: ApiError): string {
  switch (error.kind) {
    case "network":
    case "unexpected":
      return UNCERTAIN_POST_MESSAGE;
    default:
      // A 400 response gives the message of the error body, near the input.
      return error.status === 400
        ? (error.detail ?? "The name is not correct.")
        : serverErrorText(error);
  }
}

export default function BoardForm({ onAdded }: BoardFormProps) {
  const [name, setName] = useState("");
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  // The pending guard (P2). A ref changes at once. The saving state changes only after the next render.
  // The handler checks the ref, so a fast second submit cannot start a second POST.
  const pending = useRef(false);

  async function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    if (pending.current) {
      return;
    }
    const trimmed = name.trim();
    if (trimmed === "") {
      setMessage("Name is required.");
      return;
    }

    pending.current = true;
    setSaving(true);
    setMessage(null);
    try {
      await createBoard(trimmed);
      setName("");
      onAdded();
    } catch (error) {
      setMessage(submitErrorText(toApiError(error)));
    } finally {
      pending.current = false;
      setSaving(false);
    }
  }

  return (
    <form className="board-form" onSubmit={(event) => void handleSubmit(event)} noValidate>
      <label htmlFor="board-name">New board</label>
      <div className="form-row">
        <input
          id="board-name"
          type="text"
          value={name}
          onChange={(event) => setName(event.target.value)}
          aria-invalid={message !== null}
          aria-describedby={message === null ? undefined : "board-name-message"}
          autoComplete="off"
        />
        <button type="submit" disabled={saving}>
          {saving ? "Saving…" : "Add board"}
        </button>
      </div>
      {message !== null && (
        <p id="board-name-message" role="alert" className="message message-error">
          {message}
        </p>
      )}
    </form>
  );
}
