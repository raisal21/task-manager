import BoardList from "./BoardList";

export default function App() {
  return (
    <>
      <header className="app-header">
        <h1>Task Manager</h1>
      </header>
      <main className="app-main">
        <BoardList />
      </main>
    </>
  );
}
