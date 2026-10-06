import { useRef, useState, type ChangeEvent } from "react";
import { changeTaskStatus, toApiError } from "./api/client";
import type { Task, TaskStatus } from "./api/types";
import { writeErrorText } from "./errorText";
import { formatDateTime } from "./format";
import { STATUS_LABELS, TASK_STATUSES } from "./taskStatus";

interface TaskRowProps {
  task: Task;
  /** Called after a write succeeded. The list reads again, so that the row shows the status from the server. */
  onChanged: () => void;
}

export default function TaskRow({ task, onChanged }: TaskRowProps) {
  const [writing, setWriting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  // The write guard of this row. A ref changes at once, so that a second change cannot start a second request.
  const pending = useRef(false);

  async function handleStatusChange(event: ChangeEvent<HTMLSelectElement>) {
    const next = event.target.value as TaskStatus;
    if (pending.current) {
      return;
    }
    pending.current = true;
    setWriting(true);
    setError(null);
    try {
      await changeTaskStatus(task.id, next);
      onChanged();
    } catch (failure) {
      // The select is controlled by task.status. So it shows the last status from the server again.
      setError(writeErrorText(toApiError(failure), "status change"));
    } finally {
      pending.current = false;
      setWriting(false);
    }
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
      </tr>
      {error !== null && (
        <tr className="row-error">
          <td colSpan={5}>
            <p role="alert" className="message message-error">
              {error}
            </p>
          </td>
        </tr>
      )}
    </>
  );
}
