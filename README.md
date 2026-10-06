# Task Manager

A small task manager: boards that hold tasks. A task has a title, an optional description, and a status (TODO, IN_PROGRESS, or DONE).

This project is my solution to the "Mini Task Management Application" case study of a full stack developer assessment. It is a single-user application. It has no authentication, and it is not a collaborative editor.

## Status

Not runnable yet. This repository has only the layout and the conventions.

## Layout

| Folder | Content |
| --- | --- |
| `backend/` | The Spring Boot REST API. See [backend/README.md](backend/README.md). |
| `frontend/` | The React user interface. See [frontend/README.md](frontend/README.md). |

The two services are isolated. Each service gets its own manifest, README, and start command. They send data to each other only through HTTP.

## Planned stack

- Backend: Java 17, Spring Boot, Maven wrapper, Spring Data JPA, Flyway.
- Database: PostgreSQL, started with docker compose.
- Frontend: React with function components and hooks, TypeScript, Vite.

## Conventions

- `.editorconfig` and `.gitattributes` keep UTF-8 and LF line ends on all machines.
- Commit subjects use the form `type(scope): subject (IDs)`. The IDs are requirement or assumption IDs.
- `todo/` and `.locals/` hold local planning records. Git does not keep them.
