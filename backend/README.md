# Task Manager: backend

The Spring Boot service of the Task Manager. It is a shell at this time: it starts, but it has no endpoint and no database.

## Prerequisites

- JDK 17 or newer. The build uses Java release 17 (`<java.version>17</java.version>`), also on a newer JDK.
- No Maven installation is necessary. The Maven wrapper (`./mvnw`) downloads Maven 3.9.16 on first use.
- Internet access on first use, to download Maven and the dependencies.

## Start

```bash
./mvnw spring-boot:run
```

The service listens on `http://localhost:8080`.

## Test

```bash
./mvnw test
```

## Selected versions

| Item | Value |
| --- | --- |
| Spring Boot | 4.1.1 (parent `spring-boot-starter-parent`, it manages the dependency versions) |
| Java release | 17 |
| Base package | `id.raisal.taskmanager` |
| Build tool | Maven 3.9.16, through the committed wrapper |
| Dependencies | `spring-boot-starter-webmvc`, and `spring-boot-starter-webmvc-test` for tests |
| Tested JDK | Temurin 17.0.20.1 (`./mvnw test`: 1 test, no failures, 2026-10-06) |

Full proof on a clean clone with JDK 17 comes with the last ticket of the plan. Only the JDK listed above has been tested so far.

## Status

There is no endpoint, no database, and no error body yet. The context test only shows that the application context starts.
