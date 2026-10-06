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

| Path                                                     | Content                                                                                 |
| -------------------------------------------------------- | --------------------------------------------------------------------------------------- |
| `src/api/config.ts`                                      | The base URL of the backend                                                             |
| `src/api/types.ts`                                       | `Board`, and `ApiError` with the `kind` values `network`, `http`, and `unexpected`      |
| `src/api/client.ts`                                      | The only HTTP client. Components do not call `fetch`.                                   |
| `src/App.tsx`                                            | The page layout                                                                         |
| `src/hooks/useRequest.ts`                                | The state of one read and its read guard. The board list and the task list both use it. |
| `src/BoardList.tsx`                                      | The board list with its states, the selection, and the read guard                       |
| `src/BoardForm.tsx`                                      | The form to add a board, with the pending guard                                         |
| `src/TaskPanel.tsx`                                      | The task list of the selected board, with its read guard                                |
| `src/TaskTable.tsx`, `src/TaskRow.tsx`                   | The task table and one row                                                              |
| `src/errorText.ts`, `src/format.ts`, `src/taskStatus.ts` | The text of a failed read, the date helper, and the status labels                       |

## Functions

| Function                  | How it works                                                                                                                                                                                                                                                  |
| ------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Show boards               | `BoardList` reads `GET /api/boards`. The button "Reload" reads the list again.                                                                                                                                                                                |
| Select a board            | A click on a board name selects it. The selected board has `aria-current="true"`. `App` keeps the selected board ID.                                                                                                                                          |
| Add a board               | `BoardForm` below the list. The browser rejects an empty name or a name with only spaces ("Name is required."). The backend rules (100 characters) show their message near the input.                                                                         |
| Show the tasks of a board | `TaskPanel` shows a table with the columns Title, Description, Status, Created, and Updated, for the selected board. A board change shows the tasks of the new board. Times are in local time (the API sends UTC). The button "Reload" reads the tasks again. |
| Add a task                | `TaskForm` above the table: a title (necessary) and a description (optional). The browser rejects an empty title or a title with only spaces. A 400 response shows its message near the field that it names. After 201, the task list is read again.          |

Both forms have a local pending guard. When you submit, the button is disabled and shows "Saving…", and a second submit is ignored until the request ends. The frontend never sends a POST again by itself.

If a POST gets no response, the board or the task can be in the database. The form shows "The server may have saved this board. Reload the list before you send it again." (or the same text for a task) and keeps your input. Use "Reload" to see the list, then submit again if it is not there.

The task form belongs to the selected board. A board change makes a new form for the new board, with empty fields. The request of the previous board can still end. Its result does not change the new form, and it does not start a read of the list.

States of the list:

| State                                     | Text                                                                                                         |
| ----------------------------------------- | ------------------------------------------------------------------------------------------------------------ |
| Loading                                   | "Loading boards…"                                                                                            |
| Backend stopped, wrong URL, or CORS fault | "Cannot reach the server at {base URL}. The server may be stopped, or the URL or CORS setting may be wrong." |
| Error response                            | "The server returned an error: {detail}."                                                                    |
| No boards                                 | "No boards yet. Create one to start."                                                                        |

`useRequest` gives each list its state (loading, success, or error) and its read guard. Only the latest read can change the state. Each new read stops the previous read, and a response of a read that is not the latest is ignored, for a success and for an error. After the component unmounts, no read is current, and a reload does nothing.

The frontend does not delete boards. The API has `DELETE /api/boards/{id}` for that (A13).

## Limits

- The UI cannot change the status of a task or delete a task yet.
- A request that the UI ignores after a board change is not cancelled in the backend. A task that you added on a board stays there, also if you changed the board before the answer came.
- The guard of the form covers one pending request in one browser tab. It does not prevent duplicate boards from two tabs or from a second submit after an unclear result. The backend does not reject duplicate board names (A3).
- Last write wins. The UI does not detect changes from other users or tabs. Use "Reload" to see them.
