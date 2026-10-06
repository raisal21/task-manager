import { useCallback, useState } from "react";
import { getTasks } from "./api/client";
import { readErrorText } from "./errorText";
import { useRequest } from "./hooks/useRequest";
import StatusFilter, { type StatusFilterValue } from "./StatusFilter";
import { STATUS_LABELS } from "./taskStatus";
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
      {state.status === "loading" && <output>Loading tasks…</output>}
      {state.status === "error" && (
        <p role="alert" className="message message-error">
          {readErrorText(state.error)}
        </p>
      )}
      {state.status === "success" && state.data.length === 0 && (
        <p>
          {filter === "ALL"
            ? "No tasks yet."
            : `No tasks with the status ${STATUS_LABELS[filter]}.`}
        </p>
      )}
      {state.status === "success" && state.data.length > 0 && (
        <TaskTable tasks={state.data} onChanged={reload} />
      )}
    </>
  );
}
