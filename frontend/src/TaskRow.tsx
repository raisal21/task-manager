import { useRef, useState, type ChangeEvent } from "react";
import { changeTaskStatus, deleteTask, toApiError } from "./api/client";
import type { ApiError, Task, TaskStatus } from "./api/types";
import { formatDateTime } from "./format";
import StateMessage from "./StateMessage";
import { STATUS_LABELS, TASK_STATUSES } from "./taskStatus";

interface TaskRowProps {
  task: Task;
  /** Called after a write succeeded. The list reads again, so that the row shows the status from the server. */
  onChanged: () => void;
}

export default function TaskRow({ task, onChanged }: TaskRowProps) {
  const [writing, setWriting] = useState(false);
  // The failed write (a status change or a delete). The row shows its text.
  const [failure, setFailure] = useState<{
    action: "status change" | "delete";
    error: ApiError;
  } | null>(null);
  // The write guard of this row. A ref changes at once, so that a second change cannot start a second request.
  const pending = useRef(false);

  async function handleStatusChange(event: ChangeEvent<HTMLSelectElement>) {
    const next = event.target.value as TaskStatus;
    if (pending.current) {
      return;
    }
    pending.current = true;
    setWriting(true);
    setFailure(null);
    try {
      await changeTaskStatus(task.id, next);
      onChanged();
    } catch (error) {
      // The select is controlled by task.status. So it shows the last status from the server again.
      setFailure({ action: "status change", error: toApiError(error) });
    } finally {
      pending.current = false;
      setWriting(false);
    }
  }

  async function handleDelete() {
    if (pending.current) {
      return;
    }
    if (!window.confirm(`Delete the task "${task.title}"? This cannot be undone.`)) {
      return;
    }
    pending.current = true;
    setWriting(true);
    setFailure(null);
    try {
      await deleteTask(task.id);
    } catch (error) {
      setFailure({ action: "delete", error: toApiError(error) });
      pending.current = false;
      setWriting(false);
      return;
    }
    // The task is deleted. The controls stay disabled until the list reads again and this row goes away.
    onChanged();
  }

  return (
    <>
      <tr>
        <th scope="row" className="task-title">
          {task.title}
        </th>
        <td>{task.description ?? <span className="muted">No description</span>}</td>
        <td>
          <select
            aria-label={`Status of ${task.title}`}
            value={task.status}
            disabled={writing}
            onChange={(event) => void handleStatusChange(event)}
          >
            {TASK_STATUSES.map((status) => (
              <option key={status} value={status}>
                {STATUS_LABELS[status]}
              </option>
            ))}
          </select>
        </td>
        <td>
          <time dateTime={task.createdAt}>{formatDateTime(task.createdAt)}</time>
        </td>
        <td>
          <time dateTime={task.updatedAt}>{formatDateTime(task.updatedAt)}</time>
        </td>
        <td>
          <button
            type="button"
            className="button-danger"
            aria-label={`Delete ${task.title}`}
            disabled={writing}
            onClick={() => void handleDelete()}
          >
            Delete
          </button>
        </td>
      </tr>
      {failure !== null && (
        <tr className="row-error">
          <td colSpan={6}>
            <StateMessage state="write-error" action={failure.action} error={failure.error} />
          </td>
        </tr>
      )}
    </>
  );
}
