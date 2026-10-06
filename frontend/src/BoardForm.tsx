import { useRef, useState, type SubmitEvent } from "react";
import { createBoard, toApiError } from "./api/client";
import type { ApiError } from "./api/types";
import StateMessage from "./StateMessage";

interface BoardFormProps {
  onAdded: () => void;
}

/** What the form shows under the input: a validation text, or the result of a failed POST. */
type FormMessage = { kind: "validation"; text: string } | { kind: "write-error"; error: ApiError };

function messageFor(error: ApiError): FormMessage {
  // A 400 response gives the message of the error body, near the input.
  if (error.kind === "http" && error.status === 400 && error.detail !== undefined) {
    return { kind: "validation", text: error.detail };
  }
  return { kind: "write-error", error };
}

export default function BoardForm({ onAdded }: BoardFormProps) {
  const [name, setName] = useState("");
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<FormMessage | null>(null);
  // The pending guard. A ref changes at once. The saving state changes only after the next render.
  // The handler checks the ref, so a fast second submit cannot start a second POST.
  const pending = useRef(false);

  async function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    if (pending.current) {
      return;
    }
    const trimmed = name.trim();
    if (trimmed === "") {
      setMessage({ kind: "validation", text: "Name is required." });
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
      // The input stays. The form never sends the POST again by itself (A17).
      setMessage(messageFor(toApiError(error)));
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
      {message?.kind === "validation" && (
        <StateMessage state="validation" id="board-name-message" text={message.text} />
      )}
      {message?.kind === "write-error" && (
        <StateMessage
          state="write-error"
          id="board-name-message"
          action="add board"
          error={message.error}
        />
      )}
    </form>
  );
}
