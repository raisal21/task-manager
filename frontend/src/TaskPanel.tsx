import { useCallback, useState } from "react";
import { getTasks } from "./api/client";
import { useRequest } from "./hooks/useRequest";
import StateMessage from "./StateMessage";
import StatusFilter, { type StatusFilterValue } from "./StatusFilter";
import TaskForm from "./TaskForm";
import TaskTable from "./TaskTable";

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
  // The read belongs to this board and this filter. A change of the filter makes a new read function.
  const readTasks = useCallback(
    (signal: AbortSignal) => getTasks(boardId, filter === "ALL" ? null : filter, signal),
    [boardId, filter],
  );
  const { state, reload } = useRequest(readTasks);

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
      {state.status === "success" && state.data.length === 0 && (
        <StateMessage state="empty" what="tasks" status={filter === "ALL" ? undefined : filter} />
      )}
      {state.status === "success" && state.data.length > 0 && (
        <TaskTable tasks={state.data} onChanged={reload} />
      )}
    </>
  );
}
