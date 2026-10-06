# Task Manager: frontend

The React user interface of the Task Manager. It talks to the backend only through HTTP.

## Prerequisites

- Node.js `^20.19.0 || >=22.12.0` (the engine range of Vite 8.3.2) and npm. Tested with Node 24.12.0 and npm 11.6.2.
- The backend is not necessary to start the frontend. With the backend stopped, the page loads and shows an error message.

## Start

```bash
npm install
npm run dev
```

The dev server listens on `http://localhost:5173`. The port is fixed (`strictPort`): the backend allows only this origin by default.

## Configuration

| Variable            | Default                 | Use                                                              |
| ------------------- | ----------------------- | ---------------------------------------------------------------- |
| `VITE_API_BASE_URL` | `http://localhost:8080` | Base URL of the backend. Vite reads it when it starts or builds. |

Copy `.env.example` to `.env` to change it. `src/api/config.ts` is the only file that reads this variable. After a change, restart `npm run dev`.

## Quality commands

| Command                | What it does                                                    |
| ---------------------- | --------------------------------------------------------------- |
| `npm run lint`         | Oxlint, with the React, JSX accessibility, and TypeScript rules |
| `npm run format:check` | Oxfmt in check mode (`npm run format` writes the changes)       |
| `npm run typecheck`    | `tsc -b`, the TypeScript type check                             |
| `npm run build`        | The type check, then the Vite production build in `dist/`       |

## Selected stack

| Item                       | Version and cause                                               |
| -------------------------- | --------------------------------------------------------------- |
| React, React DOM           | 19.3.0, function components and hooks                           |
| TypeScript                 | 6.0.3 (the `~6.0.2` range of the Vite `react-ts` template)      |
| Vite                       | 8.3.2, an exact version (no range in `package.json`)            |
| `@vitejs/plugin-react-swc` | 4.3.3. The template adds `@vitejs/plugin-react`. It is removed. |
| Oxlint, Oxfmt              | 1.87.0 and 0.72.0. Lint and format, each with its own command.  |
| UI                         | Semantic HTML and light CSS. No component library.              |
| HTTP                       | `fetch`, only in `src/api/client.ts`                            |
| State                      | Local React state                                               |

The lockfile `package-lock.json` has the full dependency tree.

## Structure

| Path                                   | Content                                                                                 |
| -------------------------------------- | --------------------------------------------------------------------------------------- |
| `src/api/config.ts`                    | The base URL of the backend                                                             |
| `src/api/types.ts`                     | `Board`, and `ApiError` with the `kind` values `network`, `http`, and `unexpected`      |
| `src/api/client.ts`                    | The only HTTP client. Components do not call `fetch`.                                   |
| `src/App.tsx`                          | The page layout                                                                         |
| `src/hooks/useRequest.ts`              | The state of one read and its read guard. The board list and the task list both use it. |
| `src/BoardList.tsx`                    | The board list with its states, the selection, and the read guard                       |
| `src/BoardForm.tsx`                    | The form to add a board, with the pending guard                                         |
| `src/TaskPanel.tsx`                    | The task list of the selected board, with its read guard                                |
| `src/TaskTable.tsx`, `src/TaskRow.tsx` | The task table and one row                                                              |
| `src/StateMessage.tsx`                 | All state texts of the UI: loading, empty, read error, failed write, and validation     |
| `src/format.ts`, `src/taskStatus.ts`   | The date helper and the status labels                                                   |

## Functions

| Function                  | How it works                                                                                                                                                                                                                                                                                                                                                                                                      |
| ------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Show boards               | `BoardList` reads `GET /api/boards`. The button "Reload" reads the list again.                                                                                                                                                                                                                                                                                                                                    |
| Select a board            | A click on a board name selects it. The selected board has `aria-current="true"`. `App` keeps the selected board ID.                                                                                                                                                                                                                                                                                              |
| Add a board               | `BoardForm` below the list. The browser rejects an empty name or a name with only spaces ("Name is required."). The backend rules (100 characters) show their message near the input.                                                                                                                                                                                                                             |
| Show the tasks of a board | `TaskPanel` shows a table with the columns Title, Description, Status, Created, and Updated, for the selected board. A board change shows the tasks of the new board. Times are in local time (the API sends UTC). The button "Reload" reads the tasks again.                                                                                                                                                     |
| Add a task                | `TaskForm` above the table: a title (necessary) and a description (optional). The browser rejects an empty title or a title with only spaces. A 400 response shows its message near the field that it names. After 201, the task list is read again.                                                                                                                                                              |
| Change the status         | Each row has a select with "To Do", "In Progress", and "Done". A change sends `PATCH /api/tasks/{id}`. The select keeps the last status from the server until the answer comes, and then the list reads again. On an error, the row keeps its status and shows the error text under it.                                                                                                                           |
| Delete a task             | Each row has a "Delete" button. The browser asks for a confirmation (`window.confirm`) with the task title. After a confirmed delete, the controls of the row stay disabled until the list reads again and the row goes away. On an error, the row stays and shows the error text under it.                                                                                                                       |
| Filter by status          | Radio buttons "All", "To Do", "In Progress", and "Done" above the table. A change reads the list again with `?status=` (the server does the filter, A14). While the new list loads, the table shows "Loading tasks…" and not the data of the previous filter. After a status change, a delete, or an add, the list reads again with the current filter, so a task that no longer has the filtered status is gone. |

Both forms have a local pending guard. When you submit, the button is disabled and shows "Saving…", and a second submit is ignored until the request ends. The frontend never sends a POST again by itself.

If a POST gets no response, the board or the task can be in the database. The form shows "The server may have saved this board. Reload the list before you send it again." (or the same text for a task) and keeps your input. Use "Reload" to see the list, then submit again if it is not there.

The task form belongs to the selected board. A board change makes a new form for the new board, with empty fields. The request of the previous board can still end. Its result does not change the new form, and it does not start a read of the list.

## States and texts

All texts of the UI are in `src/StateMessage.tsx`. A text says only what the browser knows: what the server sent, or that the server did not answer.

| State                                              | Text                                                                                                                                                                | Control |
| -------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------- |
| Loading                                            | "Loading boards…" or "Loading tasks…"                                                                                                                               |         |
| Backend stopped, wrong URL, or CORS fault (a read) | "Cannot reach the server at {base URL}. The server may be stopped, or the URL or CORS setting may be wrong."                                                        | Retry   |
| Error response (a read)                            | "The server returned an error: {detail}." The detail is the text of the error body.                                                                                 | Retry   |
| An answer that the page cannot read (a read)       | "Something went wrong: The server sent a response that this page cannot read."                                                                                      | Retry   |
| No boards                                          | "No boards yet. Create one to start."                                                                                                                               |         |
| No tasks on a board                                | "No tasks yet." With a status filter: "No tasks with the status {status}."                                                                                          |         |
| A board POST without an answer                     | "The server may have saved this board. Reload the list before you send it again." The form keeps the input.                                                         |         |
| A task POST without an answer                      | "The server may have saved this task. Reload the list before you send it again." The form keeps the input.                                                          |         |
| A status change or a delete without an answer      | "The server did not answer, so the status change (or the delete) may or may not be saved. Reload the list to check." The row keeps its last status from the server. |         |
| An error response to a write                       | "The server returned an error: {detail}." near the form or the row                                                                                                  |         |
| A form during its request                          | The button is disabled and shows "Saving…"                                                                                                                          |         |
| A validation error                                 | The message of the error body, near the field                                                                                                                       |         |

"Retry" starts the same read again. It is only for a read that got an error. A POST, a status change, and a delete never get a Retry button, because the first request can be in the database (A17). The buttons "Reload" in the two panels read the list again at any time.

`useRequest` gives each list its state (loading, success, or error) and its read guard. Only the latest read can change the state. Each new read stops the previous read, and a response of a read that is not the latest is ignored, for a success and for an error. When the read function changes (another filter), the state is "loading" at once. `reload` always starts the current read, also when a write that began under an earlier filter ends later. After the component unmounts, no read is current, and a reload does nothing.

The frontend does not delete boards. The API has `DELETE /api/boards/{id}` for that (A13).

## Limits

- A request that the UI ignores after a board change is not cancelled in the backend. A task that you added on a board stays there, also if you changed the board before the answer came.
- The guard of the form covers one pending request in one browser tab. It does not prevent duplicate boards from two tabs or from a second submit after an unclear result. The backend does not reject duplicate board names (A3).
- Last write wins. The UI does not detect changes from other users or tabs. Use "Reload" to see them.
