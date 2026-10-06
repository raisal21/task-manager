import { useEffect, useState } from "react";
import { getBoards, toApiError } from "./api/client";
import { API_BASE_URL } from "./api/config";
import type { ApiError, Board } from "./api/types";

type BoardsState =
  | { status: "loading" }
  | { status: "success"; boards: Board[] }
  | { status: "error"; error: ApiError };

function errorText(error: ApiError): string {
  switch (error.kind) {
    case "network":
      return `Cannot reach the server at ${API_BASE_URL}. The server may be stopped, or the URL or CORS setting may be wrong.`;
    case "http":
      return `The server returned an error: ${error.detail ?? `status ${error.status}`}.`;
    default:
      return `Something went wrong: ${error.message}.`;
  }
}

function formatCreated(createdAt: string): string {
  return new Date(createdAt).toLocaleString();
}

export default function BoardList() {
  // The request starts when the component mounts, so the first state is "loading".
  const [state, setState] = useState<BoardsState>({ status: "loading" });

  useEffect(() => {
    // Read cleanup (P1). After the cleanup, the response of this request is not current.
    // Neither a success nor an error response of a request that is not current can change the state.
    const controller = new AbortController();
    let current = true;

    getBoards(controller.signal).then(
      (boards) => {
        if (current) {
          setState({ status: "success", boards });
        }
      },
      (error: unknown) => {
        if (current) {
          setState({ status: "error", error: toApiError(error) });
        }
      },
    );

    return () => {
      current = false;
      controller.abort();
    };
  }, []);

  return (
    <section aria-labelledby="boards-heading" className="panel">
      <h2 id="boards-heading">Boards</h2>
      {state.status === "loading" && <output>Loading boards…</output>}
      {state.status === "error" && (
        <p role="alert" className="message message-error">
          {errorText(state.error)}
        </p>
      )}
      {state.status === "success" && state.boards.length === 0 && (
        <p>No boards yet. Create one to start.</p>
      )}
      {state.status === "success" && state.boards.length > 0 && (
        <ul className="board-list">
          {state.boards.map((board) => (
            <li key={board.id}>
              <span className="board-name">{board.name}</span>
              <time className="muted" dateTime={board.createdAt}>
                {formatCreated(board.createdAt)}
              </time>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
