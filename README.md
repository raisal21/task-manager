# Task Manager

A small task manager: boards that hold tasks. A task has a title, an optional description, and a status (TODO, IN_PROGRESS, or DONE).

This project is my solution to the "Mini Task Management Application" case study of a full stack developer assessment. It is a single-user application. It has no authentication, and it is not a collaborative editor.

## Status

The backend sends the list of boards from PostgreSQL (`GET /api/boards`). The frontend shows that list, adds boards, and selects a board. It shows an error message when the backend is not available. There are no tasks yet.

## Layout

| Folder | Content |
| --- | --- |
| `backend/` | The Spring Boot REST API. See [backend/README.md](backend/README.md). |
| `frontend/` | The React user interface. See [frontend/README.md](frontend/README.md). |

The two services are isolated. Each service has its own manifest, README, and start command. They send data to each other only through HTTP.

## Quick start

Use three terminals. The details and prerequisites are in the service READMEs.

```bash
# 1. The database (needs Docker)
docker compose up -d db

# 2. The backend, on http://localhost:8080 (needs JDK 17 or newer)
cd backend && ./mvnw spring-boot:run

# 3. The frontend, on http://localhost:5173 (needs Node.js 20.19+ or 22.12+)
cd frontend && npm install && npm run dev
```

## Independence checks

Each service operates when the other service is stopped.

| Check | Procedure | Correct result |
| --- | --- | --- |
| Backend alone | Stop the frontend. Do `curl -i http://localhost:8080/api/boards`. | 200 and a JSON array |
| Frontend alone | Stop the backend. Open `http://localhost:5173` in a browser. | The page loads and shows "Cannot reach the server at http://localhost:8080. …". It is not empty. |

## Assumptions

The case study gives no rule for these points. Each row is a decision of this project. The code does what the row says. A2 and A13 come with the delete operations.

| ID | Point | Assumption |
| --- | --- | --- |
| A1 | An unknown value in `?status=` | 400 with the error body and the field "status" |
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
| A14 | The location of the status filter | The server, through `?status=` |
| A15 | The CORS origin | From the configuration (`APP_CORS_ALLOWED_ORIGINS`), with the default `http://localhost:5173` |
| A16 | The endpoint for the backend independence check | `GET /api/boards` |
| A17 | More than one submit, and a POST without a response | The UI disables the button and rejects a second submit while the form request is pending. It does not send a POST again by itself. Without a response, it tells the user that the board or task can be in the database, and to reload the list before a new submit. |
| A18 | A PATCH with the current status | `updated_at` does not change, because nothing changed |

## Scope

The selected scope is the core of the case study: boards, tasks, the REST API, the PostgreSQL schema, the React interface, tests, and documentation. Optional items are not part of this scope: frontend tests, a docker compose file for all processes, status counts, and an "Open" filter.

## Stack

- Backend: Java 17, Spring Boot 4.1, Maven wrapper, Spring Data JPA, Flyway.
- Database: PostgreSQL 18, started with docker compose.
- Frontend: React 19 with function components and hooks, TypeScript, Vite 8.3.2.

## Conventions

- `.editorconfig` and `.gitattributes` keep UTF-8 and LF line ends on all machines.
- Commit subjects use the form `type(scope): subject (IDs)`. The IDs are requirement or assumption IDs.
- `todo/` and `.locals/` hold local planning records. Git does not keep them.
