# Task Manager

A small task manager: boards that hold tasks. A task has a title, an optional description, and a status (TODO, IN_PROGRESS, or DONE).

This project is my solution to the "Mini Task Management Application" case study of a full stack developer assessment. It is a single-user application. It has no authentication, and it is not a collaborative editor.

It has two isolated services in one Git repository. They send data to each other only through HTTP. Nothing in the project needs a paid account, a credit card, or a licence key.

| Folder | Service | Start | Details |
| --- | --- | --- | --- |
| `backend/` | Spring Boot REST API with PostgreSQL | `./mvnw spring-boot:run` | [backend/README.md](backend/README.md) |
| `frontend/` | React user interface | `npm run dev` | [frontend/README.md](frontend/README.md) |

## Where to find what

| Item | Location |
| --- | --- |
| Prerequisites and versions | [backend/README.md](backend/README.md) and [frontend/README.md](frontend/README.md) |
| Clean-clone setup, start commands, URLs, and ports | "Quick start" below, and each service README |
| Schema creation from an empty database | [backend/README.md](backend/README.md), "Schema creation" |
| Full test commands | [backend/README.md](backend/README.md), "Test", and [frontend/README.md](frontend/README.md), "Checks (the full test commands)" |
| API contract with an error example | [backend/API.md](backend/API.md) |
| Schema: tables, columns, keys, constraints, and indexes | [backend/SCHEMA.md](backend/SCHEMA.md) |
| Selected stack and the reasons | [backend/README.md](backend/README.md) and [frontend/README.md](frontend/README.md) |
| Rejected schema alternative | [backend/README.md](backend/README.md), "Schema creation", and [backend/SCHEMA.md](backend/SCHEMA.md) |
| Assumptions A1 to A18 | "Assumptions" below |
| Trade-offs and work that is not complete | "Known limitations" and "Future work" below, and the limits in each service README |

## Quick start

Use three terminals, in a clone of this repository. You need Docker, JDK 17 or newer, and Node.js 20.19+ or 22.12+ (see the service READMEs for the versions that were tested).

```bash
# 1. The database, on 127.0.0.1:5432
docker compose up -d db

# 2. The backend, on http://localhost:8080
cd backend && ./mvnw spring-boot:run

# 3. The frontend, on http://localhost:5173
cd frontend && npm install && npm run dev
```

Open `http://localhost:5173`. Stop the services with `Ctrl+C`, and the database with `docker compose down` (its data stays in a Docker volume).

## Tests

| Where | Command | Result |
| --- | --- | --- |
| `backend/` | `./mvnw verify` | The unit tests and the integration tests with PostgreSQL in Testcontainers (needs Docker). `./mvnw test` runs only the unit tests and needs no Docker. |
| `frontend/` | `npm run lint`, `npm run format:check`, `npm run typecheck`, and `npm run build` | The quality checks. There are no automated frontend tests. |

## Independence checks

Each service operates when the other service is stopped.

| Check | Procedure | Correct result |
| --- | --- | --- |
| Backend alone | Stop the frontend. Do `curl -i http://localhost:8080/api/boards`. | 200 and a JSON array |
| Frontend alone | Stop the backend. Open `http://localhost:5173` in a browser. | The page loads and shows "Cannot reach the server at http://localhost:8080. …" with a Retry button. It is not empty. |

## Assumptions

The case study gives no rule for these points. Each row is a decision of this project. The code does what the row says.

| ID | Point | Assumption |
| --- | --- | --- |
| A1 | An unknown value in `?status=` | 400 with the error body and the field "status" |
| A2 | The status code when a board with tasks cannot be deleted | 409 with the code `BOARD_NOT_EMPTY` |
| A3 | Two boards with the same name | Permitted. A UNIQUE constraint is a rejected alternative. |
| A4 | Status changes | All changes between the three statuses are permitted, also from DONE back to TODO |
| A5 | The sequence of the items in a list | `created_at` from the first to the last, then `id` |
| A6 | Spaces and length | The service removes the spaces at the two ends and rejects an empty value. Limits: name 100, title 200, description 2000 characters. |
| A7 | The PATCH body | A JSON object with `status`. A missing or null status, an empty body, malformed JSON, or an unknown field gives 400. A body that is not an object also gives 400. |
| A8 | The ID type | BIGINT identity, sent as a JSON number |
| A9 | The time type | TIMESTAMPTZ. The API sends ISO-8601 UTC. The UI shows local time. |
| A10 | An empty description | NULL, also for a description with only spaces |
| A11 | 404 or 400 first | Malformed JSON 400, then the board or task 404, then the fields 400 |
| A12 | The source of the times | The service sets them from its clock. A new task uses one instant for `created_at` and `updated_at`. The database defaults are for SQL without the API. |
| A13 | Board delete in the UI | Not in the UI. The API has it. |
| A14 | The location of the status filter | The server, through `?status=` |
| A15 | The CORS origin | From the configuration (`APP_CORS_ALLOWED_ORIGINS`), with the default `http://localhost:5173` |
| A16 | The endpoint for the backend independence check | `GET /api/boards` |
| A17 | More than one submit, and a POST without a response | The UI disables the button and rejects a second submit while the form request is pending. It does not send a POST again by itself. Without a response, it tells the user that the board or task can be in the database, and to reload the list before a new submit. |
| A18 | A PATCH with the current status | `updated_at` does not change, because nothing changed |

## Known limitations

These are limits of the current build.

- **Last write wins.** There is no authentication, no realtime update, and no version check. When two users or two browser tabs change the same task, the last write stays (A4, A18). The UI does not show changes of other users until you read the list again ("Reload" or "Retry").
- **An unclear POST.** A POST that gets no answer can be in the database. The UI tells the user to reload the list before a new submit, and it never sends a POST again by itself (A17). The submit guard of a form covers one pending request in one tab. It does not prevent all duplicate writes, and the backend accepts boards with the same name (A3).
- **Ignored requests are not cancelled.** A request that the UI ignores after a board change still reaches the backend. For example, a task that you added on a board stays there, also if you changed the board before the answer came.
- **Small data only.** The service reads all tasks of a board and filters them by status in memory. There is no pagination, no caching, and no index on the status.
- **Tests.** The frontend has no automated tests. The backend has unit and integration tests. The browser flows were checked by hand.
- **Not in the selected scope.** A board delete in the UI, title and description edit, an "Open" filter, status counts, and a docker compose file for all processes.

## Future work

A version check (optimistic locking) for writes from different users, a task history, title and description edit, an `Idempotency-Key` header for POST requests, and a load test. Authentication, cloud deployment, and CI are also not done.

## Conventions

- `.editorconfig` and `.gitattributes` keep UTF-8 and LF line ends on all machines.
- Commit subjects use the form `type(scope): subject (IDs)`. The IDs are requirement or assumption IDs.
- `todo/` and `.locals/` hold local planning records. Git does not keep them.
