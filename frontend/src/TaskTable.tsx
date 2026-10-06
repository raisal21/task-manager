import type { Task } from "./api/types";
import TaskRow from "./TaskRow";

export default function TaskTable({ tasks }: { tasks: Task[] }) {
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
            <TaskRow key={task.id} task={task} />
          ))}
        </tbody>
      </table>
    </div>
  );
}
