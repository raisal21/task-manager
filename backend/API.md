# Task Manager API

The REST API of the backend. The endpoints come first, then the error contract.

## Endpoints

The boards endpoints (`GET /api/boards` and `POST /api/boards`) are in [README.md](README.md).

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
| 1 | The body is missing or is not valid JSON, or `boardId` is not a number | 400 | `MALFORMED_REQUEST`, or `VALIDATION_FAILED` for `boardId` | `null`, or `boardId` |
| 2 | There is no board with this ID | 404 | `NOT_FOUND` | `null` |
| 3 | The title or the description breaks a rule | 400 | `VALIDATION_FAILED` | `title` or `description` |

If another request deletes the board between the board check and the insert, the foreign key of the database rejects the insert and the answer is also 404 `NOT_FOUND`. No task row stays.

```json
{"type":"about:blank","title":"Not Found","status":404,"detail":"There is no board with ID 999999.","instance":"/api/boards/999999/tasks","code":"NOT_FOUND","field":null}
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
| A value breaks a rule, for example a board name that is empty, has only spaces, or has more than 100 characters | 400 | `VALIDATION_FAILED` | The field name, for example `name` or `title` |
| A value in the path or in the query has the wrong type, for example `/api/boards/abc/tasks` | 400 | `VALIDATION_FAILED` | The name of the parameter, for example `boardId` |
| The database rejects a value that has a named rule (`boards_name_not_blank`, `tasks_title_not_blank`, `tasks_status_valid`) | 400 | `VALIDATION_FAILED` | `name`, `title`, or `status` |
| The request body is missing or is not valid JSON | 400 | `MALFORMED_REQUEST` | `null` |
| The origin of a browser request is not in `APP_CORS_ALLOWED_ORIGINS` | 403 | `CORS_REJECTED` | `null` |
| There is no endpoint for the path, or there is no board with the ID in the path | 404 | `NOT_FOUND` | `null` |
| The HTTP method is not supported for the path. The `Allow` header lists the methods. | 405 | `METHOD_NOT_ALLOWED` | `null` |
| The content type is not supported. The `Accept` header lists the types. | 415 | `UNSUPPORTED_MEDIA_TYPE` | `null` |
| A database constraint with no mapping, or any other fault | 500 | `INTERNAL_ERROR` | `null` |

An input failure or a domain failure never gives 500. The service writes the details of a 500 fault to its log only.

New operations add their own rows to this table. The errors of the status change and of the deletes are not available yet.

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
