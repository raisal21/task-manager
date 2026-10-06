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

The dev server listens on `http://localhost:5173`. The port is fixed (`strictPort`): the backend allows only this origin by default. The backend must listen on `http://localhost:8080`, or you set `VITE_API_BASE_URL`.

| Variable            | Default                 | Use                                                              |
| ------------------- | ----------------------- | ---------------------------------------------------------------- |
| `VITE_API_BASE_URL` | `http://localhost:8080` | Base URL of the backend. Vite reads it when it starts or builds. |

Copy `.env.example` to `.env` to change it. `src/api/config.ts` is the only file that reads this variable. After a change, restart `npm run dev`.

## Checks (the full test commands)

| Command                | What it does                                                    |
| ---------------------- | --------------------------------------------------------------- |
| `npm run lint`         | Oxlint, with the React, JSX accessibility, and TypeScript rules |
| `npm run format:check` | Oxfmt in check mode (`npm run format` writes the changes)       |
| `npm run typecheck`    | `tsc -b`, the TypeScript type check                             |
| `npm run build`        | The type check, then the Vite production build in `dist/`       |

There are no automated frontend tests. They are optional, and they are not in the selected scope. The behavior was checked in a browser against the real backend: add a board, select it, add a task, change its status, filter, delete the task, and stop the backend to see the error texts. Each of these steps works without a full page reload.

## Selected stack and reasons

| Item                       | Version        | Reason                                                                                                       |
| -------------------------- | -------------- | ------------------------------------------------------------------------------------------------------------ |
| React, React DOM           | 19.3.0         | Function components and hooks (a requirement of the case study)                                              |
| TypeScript                 | 6.0.3          | Types for the API. This is the `~6.0.2` range of the Vite `react-ts` template.                               |
| Vite                       | 8.3.2          | An exact version (no range in `package.json`). It gives the dev server and the build.                        |
| `@vitejs/plugin-react-swc` | 4.3.3          | The React plugin. The template adds `@vitejs/plugin-react`, and it is removed.                               |
| Oxlint, Oxfmt              | 1.87.0, 0.72.0 | Lint and format, each with its own command. The type check is separate.                                      |
| UI                         |                | Semantic HTML and light CSS. Two panels do not need a component library.                                     |
| HTTP                       |                | `fetch`, only in `src/api/client.ts`. Components do not call `fetch`.                                        |
| State                      |                | Local React state. The selected board is in `App`. `useRequest` came after the second list flow, not before. |

The lockfile `package-lock.json` has the full dependency tree.

## Functions

- **Boards.** The list (`GET /api/boards`), the selection of one board (`aria-current="true"`), and a form to add a board. The browser rejects an empty name. The message of a 400 response shows near the input. There is no board delete in the UI (A13).
- **Tasks of the selected board.** A table with Title, Description, Status, Created, and Updated (local time, the API sends UTC). A form above it adds a task (a title and an optional description).
- **Status.** Each row has a select ("To Do", "In Progress", "Done") that sends `PATCH /api/tasks/{id}`. The select keeps the last status from the server until the answer comes, and then the list reads again.
- **Delete.** Each row has a "Delete" button. The browser asks for a confirmation (`window.confirm`) first.
- **Filter.** The radio buttons "All", "To Do", "In Progress", and "Done" read the list with `?status=` (the server does the filter, A14). After each write, the list reads again with the current filter.

Both forms and each row have a local pending guard. During a request, the button is disabled ("Saving…"), and a second submit is ignored. The frontend never sends a POST again by itself (A17). If a POST gets no answer, the board or the task can be in the database. The form says so, keeps your input, and tells you to reload the list.

Only the latest read can change a list (`useRequest`): a new read stops the previous read, and a late response, a success or an error, is ignored. Each board has its own task panel, so a request of a previous board cannot change the form or the list of the new board.

## States and texts

All texts are in `src/StateMessage.tsx`. A text says only what the browser knows: what the server sent, or that the server did not answer.

| State                                                       | Text                                                                                                                                                                | Control |
| ----------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------- |
| Loading                                                     | "Loading boards…" or "Loading tasks…"                                                                                                                               |         |
| Read: no answer (backend stopped, wrong URL, or CORS fault) | "Cannot reach the server at {base URL}. The server may be stopped, or the URL or CORS setting may be wrong."                                                        | Retry   |
| Read: error response                                        | "The server returned an error: {detail}." The detail is the text of the error body.                                                                                 | Retry   |
| Read: an answer that the page cannot read                   | "Something went wrong: The server sent a response that this page cannot read."                                                                                      | Retry   |
| No boards                                                   | "No boards yet. Create one to start."                                                                                                                               |         |
| No tasks                                                    | "No tasks yet." With a filter: "No tasks with the status {status}."                                                                                                 |         |
| Board or task POST without an answer                        | "The server may have saved this board (task). Reload the list before you send it again." The form keeps the input.                                                  |         |
| Status change or delete without an answer                   | "The server did not answer, so the status change (or the delete) may or may not be saved. Reload the list to check." The row keeps its last status from the server. |         |
| Error response to a write                                   | "The server returned an error: {detail}." near the form or the row                                                                                                  |         |
| A form during its request                                   | The button is disabled and shows "Saving…"                                                                                                                          |         |
| A validation error                                          | The message of the error body, near the field                                                                                                                       |         |

"Retry" starts the same read again. It is only for a read that got an error. A POST, a status change, and a delete never get a Retry button, because the first request can be in the database. The buttons "Reload" in the two panels read the list at any time.

## Structure

| Path                                                                                                    | Content                                                                                                      |
| ------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------ |
| `src/api/`                                                                                              | `config.ts` (the base URL), `types.ts` (`Board`, `Task`, `ApiError`), and `client.ts` (the only HTTP client) |
| `src/hooks/useRequest.ts`                                                                               | The state of one read and its read guard                                                                     |
| `src/App.tsx`                                                                                           | The layout and the selected board                                                                            |
| `src/BoardList.tsx`, `src/BoardForm.tsx`                                                                | The board list and the form to add a board                                                                   |
| `src/TaskPanel.tsx`, `src/StatusFilter.tsx`, `src/TaskForm.tsx`, `src/TaskTable.tsx`, `src/TaskRow.tsx` | The task panel, the filter, the form, the table, and one row                                                 |
| `src/StateMessage.tsx`                                                                                  | All state texts                                                                                              |
| `src/format.ts`, `src/taskStatus.ts`                                                                    | The date helper and the status labels                                                                        |

## Limits

- A request that the UI ignores after a board change is not cancelled in the backend. A task that you added on a board stays there, also if you changed the board before the answer came.
- The guards cover one pending request in one browser tab. They do not prevent duplicate boards from two tabs or from a second submit after an unclear result. The backend does not reject duplicate board names (A3).
- Last write wins. The UI does not detect changes from other users or tabs. Use "Reload" or "Retry" to see them.
- There are no automated frontend tests.
