import type { Task } from "./api/types";
import TaskRow from "./TaskRow";

interface TaskTableProps {
  tasks: Task[];
  onChanged: () => void;
}

export default function TaskTable({ tasks, onChanged }: TaskTableProps) {
  return (
    <div className="table-scroll">
      <table className="task-table">
        <thead>
          <tr>
            <th scope="col">Title</th>
            <th scope="col">Description</th>
            <th scope="col">Status</th>
            <th scope="col">Created</th>
            <th scope="col">Updated</th>
          </tr>
        </thead>
        <tbody>
          {tasks.map((task) => (
            <TaskRow key={task.id} task={task} onChanged={onChanged} />
          ))}
        </tbody>
      </table>
    </div>
  );
}
