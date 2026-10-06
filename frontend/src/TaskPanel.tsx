import { useCallback, useEffect, useRef, useState } from "react";
import { getTasks, toApiError } from "./api/client";
import type { ApiError, Task } from "./api/types";
import { readErrorText } from "./errorText";
import TaskForm from "./TaskForm";
import TaskTable from "./TaskTable";

type TasksState =
  | { status: "loading" }
  | { status: "success"; tasks: Task[] }
  | { status: "error"; error: ApiError };

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
  // The request starts when the component mounts, so the first state is "loading".
  const [state, setState] = useState<TasksState>({ status: "loading" });
  // The read guard (P1). Only the latest read of this board can change the state.
  const latestRead = useRef<AbortController | null>(null);
  // False after this board instance is removed. A late result of a form of this board must not start a read.
  const active = useRef(false);

  const load = useCallback(() => {
    if (!active.current) {
      return;
    }
    latestRead.current?.abort();
    const controller = new AbortController();
    latestRead.current = controller;

    getTasks(boardId, controller.signal).then(
      (tasks) => {
        if (latestRead.current === controller) {
          setState({ status: "success", tasks });
        }
      },
      (error: unknown) => {
        if (latestRead.current === controller) {
          setState({ status: "error", error: toApiError(error) });
        }
      },
    );
  }, [boardId]);

  useEffect(() => {
    active.current = true;
    load();
    // After the cleanup, no read is current: a late response cannot change the state.
    return () => {
      active.current = false;
      latestRead.current?.abort();
      latestRead.current = null;
    };
  }, [load]);

  return (
    <>
      <div className="panel-actions">
        <button type="button" className="button-secondary" onClick={load}>
          Reload
        </button>
      </div>
      <TaskForm boardId={boardId} onAdded={load} />
      {state.status === "loading" && <output>Loading tasks…</output>}
      {state.status === "error" && (
        <p role="alert" className="message message-error">
          {readErrorText(state.error)}
        </p>
      )}
      {state.status === "success" && state.tasks.length === 0 && <p>No tasks yet.</p>}
      {state.status === "success" && state.tasks.length > 0 && <TaskTable tasks={state.tasks} />}
    </>
  );
}
