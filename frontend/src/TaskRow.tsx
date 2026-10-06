import type { Task } from "./api/types";
import { formatDateTime } from "./format";
import { STATUS_LABELS } from "./taskStatus";

export default function TaskRow({ task }: { task: Task }) {
  return (
    <tr>
      <th scope="row" className="task-title">
        {task.title}
      </th>
      <td>{task.description ?? <span className="muted">No description</span>}</td>
      <td>{STATUS_LABELS[task.status]}</td>
      <td>
        <time dateTime={task.createdAt}>{formatDateTime(task.createdAt)}</time>
      </td>
      <td>
        <time dateTime={task.updatedAt}>{formatDateTime(task.updatedAt)}</time>
      </td>
    </tr>
  );
}
