# Task Manager: backend

The Spring Boot service of the Task Manager. It keeps boards and tasks in PostgreSQL and has a REST API with seven endpoints.

| Document | Content |
| --- | --- |
| [API.md](API.md) | Each endpoint with a request, a success, and an error, and the one error body |
| [SCHEMA.md](SCHEMA.md) | The tables, the columns, the constraints, the indexes, and the reasons |

Assumption IDs (A1 to A18) are defined in the [root README](../README.md#assumptions).

## Prerequisites

- JDK 17 or newer. The build uses Java release 17 (`<java.version>17</java.version>`), also on a newer JDK.
- No Maven installation is necessary. The Maven wrapper (`./mvnw`) downloads Maven 3.9.16 on first use.
- Docker with Compose v2 (`docker compose version` must work) and a running Docker engine. It supplies PostgreSQL 18 (`postgres:18`) for the local database and for the integration tests (Testcontainers).
- Free ports: 5432 (the database) and 8080 (the backend).
- Internet access on first use, to download Maven, the dependencies, and the `postgres:18` image.

## Start

1. Start the database from the repository root: `docker compose up -d --wait db` (it returns when PostgreSQL is healthy).
2. Start the backend in this folder: `./mvnw spring-boot:run`. On Windows, use `mvnw.cmd` instead of `./mvnw` (not tested on Windows).

The backend listens on `http://localhost:8080`. Check it with `curl -i http://localhost:8080/api/boards`. It makes the schema itself at startup (see "Schema creation").

The settings come from environment variables. Each has a local default. The database defaults match `compose.yaml`.

| Variable | Default |
| --- | --- |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/taskmanager` |
| `SPRING_DATASOURCE_USERNAME` | `taskmanager` |
| `SPRING_DATASOURCE_PASSWORD` | `taskmanager` |
| `APP_CORS_ALLOWED_ORIGINS` | `http://localhost:5173` (a comma-separated list of browser origins) |

The default credentials are for local development only. The compose service binds port 5432 to `127.0.0.1`, and its data stays in a Docker volume after `docker compose down`.

## Schema creation

Flyway runs the versioned SQL migrations in `src/main/resources/db/migration` when the backend starts. An empty database gets the full schema (`V1`, `V2`, `V3`) with no manual step. To start again from an empty database, run `docker compose down -v` in the repository root (this deletes the data in the compose database), then step 1 of "Start". Hibernate is set to `ddl-auto=validate`: it only checks that the entities match the schema and never changes it. [SCHEMA.md](SCHEMA.md) gives the tables and the reasons.

Schema alternative that was examined and rejected: the task status is a `VARCHAR(20)` with a `CHECK`, not a lookup table or a PostgreSQL `ENUM`. Three fixed values do not need a join, and the `CHECK` shows the rule in the table definition. [SCHEMA.md](SCHEMA.md) gives more rejected alternatives.

## Test

| Command | What it does | Docker |
| --- | --- | --- |
| `./mvnw test` | Unit tests with fakes, and web slice tests | Not necessary |
| `./mvnw verify` | The unit tests, then the integration tests (`*IT`) with a PostgreSQL 18 container from Testcontainers. This is the full suite. | Necessary |

Last full run (`./mvnw verify`, Temurin 17.0.20.1, 2026-10-06): 136 unit and web slice tests and 63 integration tests, 0 failures. The same command on Temurin 21.0.12.1 gave the same counts.

Surefire runs `*Test` and `*Tests`. Failsafe runs `*IT`. Each integration test context gets its own disposable container, and the tests do not use the database of `docker compose`. The race tests use a pause gate in the test sources, two committed transactions, and time limits, without sleeps.

## Stack choice

I chose option A of the brief: Java 17 with Spring Boot 4.1. Java 17 is the version the brief prefers and the minimum that Spring Boot 4 supports, and the framework gives the layers that the brief asks for (controller, service, repository, model) with a mature test setup. The database is PostgreSQL, the preferred one, because its foreign keys and `CHECK` constraints let the database enforce the rules of the data, and `docker compose up -d db` starts it with one command.

## Selected stack and reasons

| Choice | Version | Reason |
| --- | --- | --- |
| Java release 17 | Temurin 17.0.20.1 tested | Release 17 is the minimum JDK. A newer JDK can build for release 17. A JDK 17 cannot build for release 21. |
| Spring Boot | 4.1.1 | The parent POM manages the versions of the dependencies. Hibernate 7.4.5, Flyway 12.4.0, PostgreSQL JDBC 42.7.13, and Jackson 3.1.5 come with it. |
| Maven wrapper | Maven 3.9.16 | No Maven installation is necessary on a new machine. |
| PostgreSQL | 18 (`postgres:18`) | Foreign key, `CHECK`, and identity columns give the rules of the data to the database. |
| Flyway with `ddl-auto=validate` | | The schema is in versioned SQL, and the backend makes it at startup. Hibernate does not change it. |
| Spring Data JPA | | `BoardRepository` and `TaskRepository` extend `Repository<T, ID>` with only the methods that the services use, so that a fake stays small. |
| Rule functions in `BoardRules` and `TaskRules` | | The services use them, so that a unit test examines the same rules as production. The entities are plain data structures. |
| One error body (RFC 9457 with `code` and `field`) | | Spring uses this format for its own errors. A client reads `code` and `field`. |
| Fakes and Testcontainers | Testcontainers 2.0.5 | A fake tests the service rules. A real PostgreSQL container tests the constraints, the transactions, and the queries. H2 and Mockito mocks do neither. |

Package layout: `id.raisal.taskmanager` has `board`, `task`, and `common` (`error`, `config`). A controller has no business rule. A service has no HTTP type.

## Limits

- Last write wins. There is no version check (optimistic locking) when two clients change the same task.
- There is no authentication, no pagination, and no caching.
- The service filters the tasks of a board by status in memory.
