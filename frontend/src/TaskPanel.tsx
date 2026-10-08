import { useCallback, useState } from "react";
import { getTasks } from "./api/client";
import { useRequest } from "./hooks/useRequest";
import StateMessage from "./StateMessage";
import StatusFilter from "./StatusFilter";
import TaskForm from "./TaskForm";
import TaskTable from "./TaskTable";
import { serverStatus, visibleTasks, type StatusFilterValue } from "./taskFilter";

interface TaskPanelProps {
  boardId: number | null;
}

export default function TaskPanel({ boardId }: TaskPanelProps) {
  return (
    <section aria-labelledby="tasks-heading" className="panel">
      <h2 id="tasks-heading">Tasks</h2>
      {boardId === null ? (
        <p>Select a board to see its tasks.</p>
      ) : (
        // The key gives each board its own instance, with its own state and its own requests.
        // A board change removes the previous instance, and a late response of that instance cannot change anything.
        <BoardTasks key={boardId} boardId={boardId} />
      )}
    </section>
  );
}

function BoardTasks({ boardId }: { boardId: number }) {
  const [filter, setFilter] = useState<StatusFilterValue>("ALL");
  // The read belongs to this board and to the status that the server filters. A change of that status makes a
  // new read function. All and Open read the same list (Open is cut on the client), so a switch between them
  // shows the list that is already loaded.
  const status = serverStatus(filter);
  const readTasks = useCallback(
    (signal: AbortSignal) => getTasks(boardId, status, signal),
    [boardId, status],
  );
  const { state, reload } = useRequest(readTasks);
  const tasks = state.status === "success" ? visibleTasks(state.data, filter) : [];

  return (
    <>
      <div className="panel-actions">
        <button type="button" className="button-secondary" onClick={reload}>
          Reload
        </button>
      </div>
      <TaskForm boardId={boardId} onAdded={reload} />
      <StatusFilter value={filter} onChange={setFilter} />
      {state.status === "loading" && <StateMessage state="loading" what="tasks" />}
      {state.status === "error" && (
        <StateMessage state="read-error" error={state.error} onRetry={reload} />
      )}
      {state.status === "success" && tasks.length === 0 && (
        <StateMessage state="empty" what="tasks" status={filter === "ALL" ? undefined : filter} />
      )}
      {state.status === "success" && tasks.length > 0 && (
        <TaskTable tasks={tasks} onChanged={reload} />
      )}
    </>
  );
}
