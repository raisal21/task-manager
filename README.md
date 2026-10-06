# Task Manager

A small task manager: boards that hold tasks. A task has a title, an optional description, and a status (TODO, IN_PROGRESS, or DONE).

This project is my solution to the "Mini Task Management Application" case study of a full stack developer assessment. It is a single-user application. It has no authentication, and it is not a collaborative editor.

## Status

The backend sends the list of boards from PostgreSQL (`GET /api/boards`). The frontend shows that list, or an error message when the backend is not available. Boards cannot be added yet, and there are no tasks yet.

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
