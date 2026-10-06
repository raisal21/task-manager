import { useRef, useState, type SubmitEvent } from "react";
import { createTask, toApiError } from "./api/client";
import type { ApiError } from "./api/types";
import StateMessage from "./StateMessage";

interface TaskFormProps {
  boardId: number;
  onAdded: () => void;
}

interface FormMessages {
  title?: string;
  description?: string;
  /** A failed POST that is not about one field. The text depends on the kind of the error. */
  form?: ApiError;
}

function messagesFor(error: ApiError): FormMessages {
  // A 400 response gives the message of the error body, near the field that it names.
  if (error.kind === "http" && error.status === 400 && error.detail !== undefined) {
    if (error.field === "title") {
      return { title: error.detail };
    }
    if (error.field === "description") {
      return { description: error.detail };
    }
  }
  return { form: error };
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
  // The pending guard. A ref changes at once. The saving state changes only after the next render.
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
          <StateMessage state="validation" id="task-title-message" text={messages.title} />
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
          <StateMessage
            state="validation"
            id="task-description-message"
            text={messages.description}
          />
        )}
      </div>
      <div className="form-actions">
        <button type="submit" disabled={saving}>
          {saving ? "Saving…" : "Add task"}
        </button>
      </div>
      {messages.form !== undefined && (
        <StateMessage state="write-error" action="add task" error={messages.form} />
      )}
    </form>
  );
}
