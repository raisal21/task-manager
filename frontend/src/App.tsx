import { useState } from "react";
import BoardList from "./BoardList";
import TaskPanel from "./TaskPanel";

export default function App() {
  // The selected board lives here, in the nearest parent of all that need it.
  const [selectedBoardId, setSelectedBoardId] = useState<number | null>(null);

  return (
    <>
      <header className="app-header">
        <h1>Task Manager</h1>
      </header>
      <main className="app-main">
        <BoardList selectedBoardId={selectedBoardId} onSelect={setSelectedBoardId} />
        <TaskPanel boardId={selectedBoardId} />
      </main>
    </>
  );
}
