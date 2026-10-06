import { getBoards } from "./api/client";
import BoardForm from "./BoardForm";
import { readErrorText } from "./errorText";
import { formatDateTime } from "./format";
import { useRequest } from "./hooks/useRequest";

interface BoardListProps {
  selectedBoardId: number | null;
  onSelect: (boardId: number) => void;
}

export default function BoardList({ selectedBoardId, onSelect }: BoardListProps) {
  const { state, reload } = useRequest(getBoards);

  return (
    <section aria-labelledby="boards-heading" className="panel">
      <div className="panel-header">
        <h2 id="boards-heading">Boards</h2>
        <button type="button" className="button-secondary" onClick={reload}>
          Reload
        </button>
      </div>
      {state.status === "loading" && <output>Loading boards…</output>}
      {state.status === "error" && (
        <p role="alert" className="message message-error">
          {readErrorText(state.error)}
        </p>
      )}
      {state.status === "success" && state.data.length === 0 && (
        <p>No boards yet. Create one to start.</p>
      )}
      {state.status === "success" && state.data.length > 0 && (
        <ul className="board-list">
          {state.data.map((board) => (
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
      <BoardForm onAdded={reload} />
    </section>
  );
}
