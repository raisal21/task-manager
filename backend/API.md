# Task Manager API

The REST API of the backend. It has seven endpoints and one error body. Assumption IDs such as A5 or A11 are defined in the [root README](../README.md#assumptions).

| Item | Rule |
| --- | --- |
| Base URL | `http://localhost:8080`. The frontend gets it from `VITE_API_BASE_URL`. |
| Format | JSON. A request with a body has the header `Content-Type: application/json`. Another content type gives 415. |
| IDs | Numbers (`BIGINT`), sent as JSON numbers (A8) |
| Times | ISO-8601 in UTC, with up to microsecond precision, for example `2026-10-06T09:43:17.154600Z`. Trailing zeros of the fraction can be missing. (A9) |
| Lists | In the order of `createdAt` from the first to the last, then `id` (A5). There is no pagination. |
| CORS | The backend answers browser requests from the origins in `APP_CORS_ALLOWED_ORIGINS` (default `http://localhost:5173`) for all paths under `/api/` |
| Errors | One JSON body for all failures. See "Errors" at the end of this document. |

| Method and path | Success | Errors |
| --- | --- | --- |
| `GET /api/boards` | 200, an array of boards | |
| `POST /api/boards` | 201, the new board | 400 |
| `DELETE /api/boards/{boardId}` | 204 | 400, 404, 409 |
| `GET /api/boards/{boardId}/tasks` | 200, an array of tasks | 400, 404 |
| `POST /api/boards/{boardId}/tasks` | 201, the new task | 400, 404 |
| `PATCH /api/tasks/{taskId}` | 200, the changed task | 400, 404 |
| `DELETE /api/tasks/{taskId}` | 204 | 400, 404 |

A board is `{"id", "name", "createdAt"}`. A task is `{"id", "boardId", "title", "description", "status", "createdAt", "updatedAt"}`. The `description` is `null` when there is none. The `status` is `TODO`, `IN_PROGRESS`, or `DONE`.

## Endpoints

### `GET /api/boards`

Sends all boards as a JSON array.

```bash
curl -i http://localhost:8080/api/boards
```

Response `200`:

```json
[{"id":1,"name":"Sprint 1","createdAt":"2026-10-06T09:27:16.907806Z"}]
```

A browser request from an origin that is not in `APP_CORS_ALLOWED_ORIGINS` gets 403:

```bash
curl -i -H 'Origin: http://evil.example' http://localhost:8080/api/boards
```

```json
{"type":"about:blank","title":"Forbidden","status":403,"detail":"The origin of the request is not allowed.","instance":"/api/boards","code":"CORS_REJECTED","field":null}
```

### `POST /api/boards`

Adds a board. The body is a JSON object with `name`. The service removes the spaces at the two ends of the name. The name must not be empty, and it can have 100 characters at most (A6). The service sets `createdAt` from its clock (A12). Two boards can have the same name (A3).

```bash
curl -i -H 'Content-Type: application/json' -d '{"name":"  Sprint 1  "}' http://localhost:8080/api/boards
```

Response `201`:

```json
{"id":3,"name":"Sprint 1","createdAt":"2026-10-06T09:43:17.154600Z"}
```

A missing name, an empty name, a name with only spaces, or a name with more than 100 characters gives 400:

```json
{"type":"about:blank","title":"Bad Request","status":400,"detail":"Name is required.","instance":"/api/boards","code":"VALIDATION_FAILED","field":"name"}
```

### `DELETE /api/boards/{boardId}`

Deletes a board that has no tasks. A board with tasks is not deleted (A2). Delete its tasks first. The UI has no board delete (A13).

```bash
curl -i -X DELETE http://localhost:8080/api/boards/2
```

| Result | Status | Body |
| --- | --- | --- |
| The board has no tasks and is deleted | 204 | none |
| The board has one or more tasks, in any status | 409 | The error body, `BOARD_NOT_EMPTY`. The board and its tasks stay. |
| `boardId` is not a number | 400 | The error body, `VALIDATION_FAILED`, field `boardId` |
| There is no board with this ID | 404 | The error body, `NOT_FOUND` |

```json
{"type":"about:blank","title":"Conflict","status":409,"detail":"Board 1 has tasks. Delete its tasks first.","instance":"/api/boards/1","code":"BOARD_NOT_EMPTY","field":null}
```

Two layers keep this rule. The service checks the tasks first. The database foreign key `fk_tasks_board` (`ON DELETE RESTRICT`) is the backstop for SQL without the API, and for a task that another request adds between the check and the delete. In that case the answer is also 409, and nothing is deleted.

### `GET /api/boards/{boardId}/tasks`

Sends the tasks of one board as a JSON array. The tasks are in the order of `createdAt` from the first to the last, then `id` (A5). A board without tasks gives `[]`.

The optional query parameter `status` keeps only the tasks with that status. The value must be `TODO`, `IN_PROGRESS`, or `DONE`, in capital letters. The server does the filter (A14).

```bash
curl -i 'http://localhost:8080/api/boards/1/tasks?status=DONE'
```

Response `200`:

```json
[{"id":2,"boardId":1,"title":"No description","description":null,"status":"DONE","createdAt":"2026-10-06T10:25:23.527488Z","updatedAt":"2026-10-06T10:25:23.527488Z"}]
```

| Order | Failure | Status | `code` | `field` |
| --- | --- | --- | --- | --- |
| 1 | `boardId` is not a number | 400 | `VALIDATION_FAILED` | `boardId` |
| 2 | There is no board with this ID | 404 | `NOT_FOUND` | `null` |
| 3 | The value of `status` is not one of the three statuses (A1) | 400 | `VALIDATION_FAILED` | `status` |

```json
{"type":"about:blank","title":"Not Found","status":404,"detail":"There is no board with ID 999999.","instance":"/api/boards/999999/tasks","code":"NOT_FOUND","field":null}
```

### `POST /api/boards/{boardId}/tasks`

Adds a task to a board. The body is a JSON object with `title` and an optional `description`.

| Field | Rule |
| --- | --- |
| `title` | Required. The service removes the spaces at the two ends. It must not be empty and can have 200 characters at most (A6). |
| `description` | Optional. The service removes the spaces at the two ends. A missing, empty, or only-spaces description becomes `null` (A10). It can have 2000 characters at most (A6). |

The new task has the status `TODO`. `createdAt` and `updatedAt` have the same value, from the clock of the service (A12). Times are ISO-8601 UTC.

```bash
curl -i -H 'Content-Type: application/json' \
  -d '{"title":"  Write docs  ","description":"  First task  "}' \
  http://localhost:8080/api/boards/1/tasks
```

Response `201`:

```json
{"id":1,"boardId":1,"title":"Write docs","description":"First task","status":"TODO","createdAt":"2026-10-06T10:25:23.488702Z","updatedAt":"2026-10-06T10:25:23.488702Z"}
```

Errors, in this sequence (A11):

| Order | Failure | Status | `code` | `field` |
| --- | --- | --- | --- | --- |
| 1 | The body is missing, is not valid JSON, or is not a JSON object | 400 | `MALFORMED_REQUEST` | `null` |
| 1 | `boardId` is not a number | 400 | `VALIDATION_FAILED` | `boardId` |
| 2 | There is no board with this ID | 404 | `NOT_FOUND` | `null` |
| 3 | The title or the description breaks a rule | 400 | `VALIDATION_FAILED` | `title` or `description` |

If another request deletes the board between the board check and the insert, the foreign key of the database rejects the insert and the answer is also 404 `NOT_FOUND`. No task row stays.

```json
{"type":"about:blank","title":"Not Found","status":404,"detail":"There is no board with ID 999999.","instance":"/api/boards/999999/tasks","code":"NOT_FOUND","field":null}
```

### `PATCH /api/tasks/{taskId}`

Changes the status of a task. This is the only change that the API allows for a task (title and description edit is future work). The body is a JSON object with exactly one field, `status` (A7):

```bash
curl -i -X PATCH -H 'Content-Type: application/json' -d '{"status":"IN_PROGRESS"}' http://localhost:8080/api/tasks/1
```

Response `200` with the task:

```json
{"id":1,"boardId":1,"title":"Task","description":null,"status":"IN_PROGRESS","createdAt":"2026-10-06T10:45:45.984281Z","updatedAt":"2026-10-06T10:45:46.015887Z"}
```

- All changes between the three statuses are permitted, also from `DONE` back to `TODO` (A4).
- If the status changes, the service sets `updatedAt` from its clock. If the new status is the same as the old status, nothing changes, and `updatedAt` stays the same (A18).
- The request is idempotent: the same body again gives the same task.

Errors, in this sequence (A11):

| Order | Failure | Status | `code` | `field` |
| --- | --- | --- | --- | --- |
| 1 | The body is missing, is not valid JSON, is not a JSON object, or has a `status` that is an object or a list | 400 | `MALFORMED_REQUEST` | `null` |
| 1 | `taskId` is not a number | 400 | `VALIDATION_FAILED` | `taskId` |
| 2 | There is no task with this ID | 404 | `NOT_FOUND` | `null` |
| 3 | The body has a field other than `status` | 400 | `VALIDATION_FAILED` | The name of the first unknown field |
| 4 | `status` is missing or `null` | 400 | `VALIDATION_FAILED` | `status` |
| 5 | `status` is not exactly `TODO`, `IN_PROGRESS`, or `DONE` | 400 | `VALIDATION_FAILED` | `status` |

```json
{"type":"about:blank","title":"Bad Request","status":400,"detail":"Unknown field 'title'. Only 'status' can be changed.","instance":"/api/tasks/1","code":"VALIDATION_FAILED","field":"title"}
```

### `DELETE /api/tasks/{taskId}`

Deletes one task.

```bash
curl -i -X DELETE http://localhost:8080/api/tasks/1
```

| Result | Status | Body |
| --- | --- | --- |
| The task is deleted | 204 | none |
| `taskId` is not a number | 400 | The error body, `VALIDATION_FAILED`, field `taskId` |
| There is no task with this ID. This is also the answer for a second delete of the same task. | 404 | The error body, `NOT_FOUND` |

```json
{"type":"about:blank","title":"Not Found","status":404,"detail":"There is no task with ID 1.","instance":"/api/tasks/1","code":"NOT_FOUND","field":null}
```

## Errors

All failures use one JSON body with the content type `application/problem+json` (RFC 9457 Problem Details, with the extension members `code` and `field`). This includes the failures that Spring makes before a controller starts, for example malformed JSON, an unknown path, an incorrect method, an unsupported media type, and a rejected CORS origin.

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Name is required.",
  "instance": "/api/boards",
  "code": "VALIDATION_FAILED",
  "field": "name"
}
```

| Member | Type | Meaning |
| --- | --- | --- |
| `type` | string | Always `about:blank`. For this type, `title` is the HTTP status phrase. |
| `title` | string | The HTTP status phrase, for example `Bad Request` |
| `status` | number | The HTTP status code |
| `detail` | string | A message for a person. It is not for program logic. A server fault always gets a general message. |
| `instance` | string | The path of the request |
| `code` | string | The stable code for the cause, from the table below. Clients use this member for program logic. |
| `field` | string or null | The input field when the failure is about one field. Otherwise `null`. The member is always there. |

### Codes

| Failure | Status | `code` | `field` |
| --- | --- | --- | --- |
| A value breaks a rule, for example a board name that is empty, has only spaces, or has more than 100 characters, or a PATCH body has a field that is not allowed | 400 | `VALIDATION_FAILED` | The field name, for example `name`, `title`, or `status` |
| A value in the path or in the query has the wrong type, for example `/api/boards/abc/tasks` | 400 | `VALIDATION_FAILED` | The name of the parameter, for example `boardId` or `taskId` |
| The database rejects a value that has a named rule (`boards_name_not_blank`, `tasks_title_not_blank`, `tasks_status_valid`) | 400 | `VALIDATION_FAILED` | `name`, `title`, or `status` |
| The request body is missing or is not valid JSON | 400 | `MALFORMED_REQUEST` | `null` |
| The origin of a browser request is not in `APP_CORS_ALLOWED_ORIGINS` | 403 | `CORS_REJECTED` | `null` |
| There is no endpoint for the path, or there is no board or task with the ID in the path | 404 | `NOT_FOUND` | `null` |
| A board that has tasks cannot be deleted (A2) | 409 | `BOARD_NOT_EMPTY` | `null` |
| The HTTP method is not supported for the path. The `Allow` header lists the methods. | 405 | `METHOD_NOT_ALLOWED` | `null` |
| The content type is not supported. The `Accept` header lists the types. | 415 | `UNSUPPORTED_MEDIA_TYPE` | `null` |
| A database constraint with no mapping, or any other fault | 500 | `INTERNAL_ERROR` | `null` |

An input failure or a domain failure never gives 500. The service writes the details of a 500 fault to its log only.

New operations add their own rows to this table.

### Examples

An unknown path:

```bash
curl -i http://localhost:8080/api/nope
```

```json
{"type":"about:blank","title":"Not Found","status":404,"detail":"There is no endpoint for this path.","instance":"/api/nope","code":"NOT_FOUND","field":null}
```

A method that is not supported:

```bash
curl -i -X PUT -H 'Content-Type: application/json' -d '{}' http://localhost:8080/api/boards
```

```json
{"type":"about:blank","title":"Method Not Allowed","status":405,"detail":"Method 'PUT' is not supported.","instance":"/api/boards","code":"METHOD_NOT_ALLOWED","field":null}
```
