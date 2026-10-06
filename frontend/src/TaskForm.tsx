import { useRef, useState, type SubmitEvent } from "react";
import { createTask, toApiError } from "./api/client";
import type { ApiError } from "./api/types";
import { serverErrorText } from "./errorText";

interface TaskFormProps {
  boardId: number;
  onAdded: () => void;
}

// A17: without a response, the task can be in the database. The form never sends the POST again by itself.
const UNCERTAIN_POST_MESSAGE =
  "The server may have saved this task. Reload the list before you send it again.";

interface FormMessages {
  title?: string;
  description?: string;
  form?: string;
}

function messagesFor(error: ApiError): FormMessages {
  if (error.kind === "network" || error.kind === "unexpected") {
    return { form: UNCERTAIN_POST_MESSAGE };
  }
  // A 400 response gives the message of the error body, near the field that it names.
  if (error.status === 400 && error.field === "title") {
    return { title: error.detail };
  }
  if (error.status === 400 && error.field === "description") {
    return { description: error.detail };
  }
  return { form: serverErrorText(error) };
}

/**
 * The form belongs to one board. BoardTasks gives it a key, so that a board change makes a new form with new state.
 * The request of the previous form can still end, but its result cannot reach the form of the new board.
 */
export default function TaskForm({ boardId, onAdded }: TaskFormProps) {
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [saving, setSaving] = useState(false);
  const [messages, setMessages] = useState<FormMessages>({});
  // The pending guard (P2). A ref changes at once. The saving state changes only after the next render.
  const pending = useRef(false);
  const titleInput = useRef<HTMLInputElement>(null);

  async function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    if (pending.current) {
      return;
    }
    if (title.trim() === "") {
      setMessages({ title: "Title is required." });
      titleInput.current?.focus();
      return;
    }

    pending.current = true;
    setSaving(true);
    setMessages({});
    try {
      await createTask(boardId, { title: title.trim(), description: description.trim() });
      setTitle("");
      setDescription("");
      onAdded();
      titleInput.current?.focus();
    } catch (error) {
      setMessages(messagesFor(toApiError(error)));
    } finally {
      pending.current = false;
      setSaving(false);
    }
  }

  return (
    <form className="task-form" onSubmit={(event) => void handleSubmit(event)} noValidate>
      <div className="field">
        <label htmlFor="task-title">Title</label>
        <input
          id="task-title"
          ref={titleInput}
          type="text"
          value={title}
          onChange={(event) => setTitle(event.target.value)}
          aria-invalid={messages.title !== undefined}
          aria-describedby={messages.title === undefined ? undefined : "task-title-message"}
          autoComplete="off"
        />
        {messages.title !== undefined && (
          <p id="task-title-message" role="alert" className="message message-error">
            {messages.title}
          </p>
        )}
      </div>
      <div className="field">
        <label htmlFor="task-description">Description (optional)</label>
        <textarea
          id="task-description"
          rows={2}
          value={description}
          onChange={(event) => setDescription(event.target.value)}
          aria-invalid={messages.description !== undefined}
          aria-describedby={
            messages.description === undefined ? undefined : "task-description-message"
          }
        />
        {messages.description !== undefined && (
          <p id="task-description-message" role="alert" className="message message-error">
            {messages.description}
          </p>
        )}
      </div>
      <div className="form-actions">
        <button type="submit" disabled={saving}>
          {saving ? "Saving…" : "Add task"}
        </button>
      </div>
      {messages.form !== undefined && (
        <p role="alert" className="message message-error">
          {messages.form}
        </p>
      )}
    </form>
  );
}
