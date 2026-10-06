import { getBoards } from "./api/client";
import BoardForm from "./BoardForm";
import { formatDateTime } from "./format";
import { useRequest } from "./hooks/useRequest";
import StateMessage from "./StateMessage";

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
      {state.status === "loading" && <StateMessage state="loading" what="boards" />}
      {state.status === "error" && (
        <StateMessage state="read-error" error={state.error} onRetry={reload} />
      )}
      {state.status === "success" && state.data.length === 0 && (
        <StateMessage state="empty" what="boards" />
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
