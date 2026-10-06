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

| Path                | Content                                                                            |
| ------------------- | ---------------------------------------------------------------------------------- |
| `src/api/config.ts` | The base URL of the backend                                                        |
| `src/api/types.ts`  | `Board`, and `ApiError` with the `kind` values `network`, `http`, and `unexpected` |
| `src/api/client.ts` | The only HTTP client. Components do not call `fetch`.                              |
| `src/App.tsx`       | The page layout                                                                    |

## Status

The page shows the layout only. It does not show boards yet.
