# Task Manager: backend

The Spring Boot service of the Task Manager. It has the board endpoints `GET /api/boards` and `POST /api/boards`, the task endpoints `GET` and `POST /api/boards/{boardId}/tasks` and `PATCH /api/tasks/{taskId}`, and keeps its data in PostgreSQL.

## Prerequisites

- JDK 17 or newer. The build uses Java release 17 (`<java.version>17</java.version>`), also on a newer JDK.
- No Maven installation is necessary. The Maven wrapper (`./mvnw`) downloads Maven 3.9.16 on first use.
- Docker, for the local database (`docker compose`) and for the integration tests (Testcontainers).
- Internet access on first use, to download Maven, the dependencies, and the `postgres:18` image.

## Start

1. Start the database from the repository root: `docker compose up -d db`.
2. Start the backend in this folder: `./mvnw spring-boot:run`.

The backend listens on `http://localhost:8080`. It makes the schema itself at startup (see "Schema creation").

The settings come from environment variables. Each has a local default. The database defaults match `compose.yaml`.

| Variable | Default |
| --- | --- |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/taskmanager` |
| `SPRING_DATASOURCE_USERNAME` | `taskmanager` |
| `SPRING_DATASOURCE_PASSWORD` | `taskmanager` |
| `APP_CORS_ALLOWED_ORIGINS` | `http://localhost:5173` (a comma-separated list of browser origins) |

The default credentials are for local development only. The compose service binds port 5432 to `127.0.0.1`.

## Schema creation

See [SCHEMA.md](SCHEMA.md) for the tables, the constraints, and the reasons.

Flyway runs the versioned SQL migrations in `src/main/resources/db/migration` when the backend starts. An empty database gets the full schema, with no manual step. Hibernate is set to `ddl-auto=validate`: it only checks that the entities match the schema and never changes it.

| Migration | Content |
| --- | --- |
| `V1__create_boards.sql` | The `boards` table: identity `id`, `name VARCHAR(100) NOT NULL`, `created_at TIMESTAMPTZ NOT NULL` |
| `V2__boards_name_not_blank.sql` | The constraint `boards_name_not_blank`: `CHECK (name ~ '\S')`. The database also rejects a name with only spaces or tabs. |
| `V3__create_tasks.sql` | The `tasks` table with the foreign key `fk_tasks_board` (`ON DELETE RESTRICT`), the checks `tasks_title_not_blank` and `tasks_status_valid`, and the index `tasks_board_id_idx`. |

## API

### `GET /api/boards`

Sends all boards as a JSON array, ordered by `createdAt` from the first to the last, then by `id` (A5).

```bash
curl -i http://localhost:8080/api/boards
```

```json
[{"id":1,"name":"Sprint 1","createdAt":"2026-10-06T09:27:16.907806Z"}]
```

`createdAt` is an ISO-8601 UTC time. Failures use the error body in [API.md](API.md).

### `POST /api/boards`

Adds a board. The body is a JSON object with `name`. The service removes the spaces at the two ends of the name. The name must not be empty, and it can have 100 characters at most (A6). The service sets `createdAt` from its clock (A12).

```bash
curl -i -H 'Content-Type: application/json' -d '{"name":"  Sprint 1  "}' http://localhost:8080/api/boards
```

Response `201`:

```json
{"id":3,"name":"Sprint 1","createdAt":"2026-10-06T09:43:17.154600Z"}
```

A missing name, an empty name, a name with only spaces, or a name with more than 100 characters gives `400` with `Content-Type: application/problem+json`. All failures use this body. See [API.md](API.md) for the members and the codes.

```json
{"type":"about:blank","title":"Bad Request","status":400,"detail":"Name is required.","instance":"/api/boards","code":"VALIDATION_FAILED","field":"name"}
```

### CORS

The browser frontend runs on another origin than the API. The backend answers requests from the origins in `APP_CORS_ALLOWED_ORIGINS` for the paths under `/api/` (methods GET, POST, PATCH, and DELETE). A request from any other origin gets 403 with the error body (`CORS_REJECTED`). CORS does not protect the API from other clients, such as curl.

## Test

| Command | What it does | Docker |
| --- | --- | --- |
| `./mvnw test` | Unit tests with fakes, and the web slice test | Not necessary |
| `./mvnw verify` | The unit tests, then the integration tests (`*IT`) with a PostgreSQL 18 container from Testcontainers | Necessary |

Surefire runs `*Test` and `*Tests`. Failsafe runs `*IT`. Each integration test context gets its own disposable container.

## Selected versions

| Item | Value |
| --- | --- |
| Spring Boot | 4.1.1 (parent `spring-boot-starter-parent`, it manages the dependency versions) |
| Java release | 17 |
| Base package | `id.raisal.taskmanager` |
| Build tool | Maven 3.9.16, through the committed wrapper |
| Data | Spring Data JPA with Hibernate 7.4.5, Flyway 12.4.0, PostgreSQL JDBC 42.7.13 |
| Database | PostgreSQL 18 (`postgres:18`) |
| Test | Testcontainers 2.0.5 (`testcontainers-postgresql`), `spring-boot-testcontainers` |
| Tested JDK | Temurin 17.0.20.1 (`./mvnw test` and `./mvnw verify`, 2026-10-06) |

Full proof on a clean clone with JDK 17 comes with the last ticket of the plan. Only the JDK listed above has been tested so far.

## Status

The backend lists and adds boards, and lists and adds the tasks of a board (see [API.md](API.md)). It changes the status of a task (`PATCH /api/tasks/{taskId}`). It cannot delete a task or a board yet. All failures use the one error body of [API.md](API.md).
