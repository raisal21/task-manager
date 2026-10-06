import { useCallback, useEffect, useRef, useState } from "react";
import { getBoards, toApiError } from "./api/client";
import type { ApiError, Board } from "./api/types";
import BoardForm from "./BoardForm";
import { readErrorText } from "./errorText";
import { formatDateTime } from "./format";

type BoardsState =
  | { status: "loading" }
  | { status: "success"; boards: Board[] }
  | { status: "error"; error: ApiError };

interface BoardListProps {
  selectedBoardId: number | null;
  onSelect: (boardId: number) => void;
}

export default function BoardList({ selectedBoardId, onSelect }: BoardListProps) {
  // The request starts when the component mounts, so the first state is "loading".
  const [state, setState] = useState<BoardsState>({ status: "loading" });
  // The read guard (P1). Only the latest read can change the state. A read that is not the latest
  // is not current: its success and its error response are both ignored.
  const latestRead = useRef<AbortController | null>(null);

  const load = useCallback(() => {
    latestRead.current?.abort();
    const controller = new AbortController();
    latestRead.current = controller;

    getBoards(controller.signal).then(
      (boards) => {
        if (latestRead.current === controller) {
          setState({ status: "success", boards });
        }
      },
      (error: unknown) => {
        if (latestRead.current === controller) {
          setState({ status: "error", error: toApiError(error) });
        }
      },
    );
  }, []);

  useEffect(() => {
    load();
    // After the cleanup, no read is current: a late response cannot change the state.
    return () => {
      latestRead.current?.abort();
      latestRead.current = null;
    };
  }, [load]);

  return (
    <section aria-labelledby="boards-heading" className="panel">
      <div className="panel-header">
        <h2 id="boards-heading">Boards</h2>
        <button type="button" className="button-secondary" onClick={load}>
          Reload
        </button>
      </div>
      {state.status === "loading" && <output>Loading boards…</output>}
      {state.status === "error" && (
        <p role="alert" className="message message-error">
          {readErrorText(state.error)}
        </p>
      )}
      {state.status === "success" && state.boards.length === 0 && (
        <p>No boards yet. Create one to start.</p>
      )}
      {state.status === "success" && state.boards.length > 0 && (
        <ul className="board-list">
          {state.boards.map((board) => (
            <li key={board.id}>
              <button
                type="button"
                className="board-button"
                aria-current={board.id === selectedBoardId ? "true" : undefined}
                onClick={() => onSelect(board.id)}
              >
                {board.name}
              </button>
              <time className="muted" dateTime={board.createdAt}>
                {formatDateTime(board.createdAt)}
              </time>
            </li>
          ))}
        </ul>
      )}
      <BoardForm onAdded={load} />
    </section>
  );
}
