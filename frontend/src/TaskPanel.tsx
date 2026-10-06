import { useCallback } from "react";
import { getTasks } from "./api/client";
import { readErrorText } from "./errorText";
import { useRequest } from "./hooks/useRequest";
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
  const readTasks = useCallback((signal: AbortSignal) => getTasks(boardId, signal), [boardId]);
  const { state, reload } = useRequest(readTasks);

  return (
    <>
      <div className="panel-actions">
        <button type="button" className="button-secondary" onClick={reload}>
          Reload
        </button>
      </div>
      <TaskForm boardId={boardId} onAdded={reload} />
      {state.status === "loading" && <output>Loading tasks…</output>}
      {state.status === "error" && (
        <p role="alert" className="message message-error">
          {readErrorText(state.error)}
        </p>
      )}
      {state.status === "success" && state.data.length === 0 && <p>No tasks yet.</p>}
      {state.status === "success" && state.data.length > 0 && (
        <TaskTable tasks={state.data} onChanged={reload} />
      )}
    </>
  );
}
