# Task Manager API

The REST API of the backend. This document gives the error contract. The endpoints that exist now are in [README.md](README.md).

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
| A value breaks a rule, for example a board name that is empty, has only spaces, or has more than 100 characters | 400 | `VALIDATION_FAILED` | The field name, for example `name` |
| The database rejects a value that has a named rule (`boards_name_not_blank`) | 400 | `VALIDATION_FAILED` | `name` |
| The request body is missing or is not valid JSON | 400 | `MALFORMED_REQUEST` | `null` |
| The origin of a browser request is not in `APP_CORS_ALLOWED_ORIGINS` | 403 | `CORS_REJECTED` | `null` |
| There is no endpoint for the path | 404 | `NOT_FOUND` | `null` |
| The HTTP method is not supported for the path. The `Allow` header lists the methods. | 405 | `METHOD_NOT_ALLOWED` | `null` |
| The content type is not supported. The `Accept` header lists the types. | 415 | `UNSUPPORTED_MEDIA_TYPE` | `null` |
| A database constraint with no mapping, or any other fault | 500 | `INTERNAL_ERROR` | `null` |

An input failure or a domain failure never gives 500. The service writes the details of a 500 fault to its log only.

New operations add their own rows to this table. The errors of tasks and of the board delete are not available yet.

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
