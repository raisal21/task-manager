# Task Manager schema

The PostgreSQL 18 schema of the backend. Flyway makes it from the migrations in `src/main/resources/db/migration` when the backend starts. The migrations are the source. This document gives the tables, the columns, the constraints, the indexes, and the reason for each choice.

| Migration | Content |
| --- | --- |
| `V1__create_boards.sql` | The `boards` table |
| `V2__boards_name_not_blank.sql` | The constraint `boards_name_not_blank` |
| `V3__create_tasks.sql` | The `tasks` table, its constraints, and its index |

A board has many tasks. A task has exactly one board (`tasks.board_id` points to `boards.id`).

## boards

| Column | Type | Null | Default | Reason |
| --- | --- | --- | --- | --- |
| `id` | `BIGINT GENERATED ALWAYS AS IDENTITY` | no | identity | The primary key. A short number is easy to type in curl. The API sends it as a JSON number (A8). |
| `name` | `VARCHAR(100)` | no | none | The name. 100 characters at most (A6). |
| `created_at` | `TIMESTAMPTZ` | no | `now()` | The creation time. The service sets it from its clock. The default is for SQL without the API (A9, A12). |

| Name | Rule | Reason |
| --- | --- | --- |
| `boards_pkey` | Primary key on `id` | One row for each board |
| `boards_name_not_blank` | `CHECK (name ~ '\S')` | The name has at least one character that is not white space. The database also rejects a name with only spaces or tabs. It is the backstop for the rule in the service. |

Board names are not unique (A3). Two boards can have the same name.

## tasks

| Column | Type | Null | Default | Reason |
| --- | --- | --- | --- | --- |
| `id` | `BIGINT GENERATED ALWAYS AS IDENTITY` | no | identity | The primary key (A8) |
| `board_id` | `BIGINT` | no | none | The board of the task. A task always has a board. |
| `title` | `VARCHAR(200)` | no | none | The title. 200 characters at most (A6). |
| `description` | `VARCHAR(2000)` | yes | none | An optional description. 2000 characters at most (A6). An empty description is `NULL` (A10). |
| `status` | `VARCHAR(20)` | no | `'TODO'` | `TODO`, `IN_PROGRESS`, or `DONE` |
| `created_at` | `TIMESTAMPTZ` | no | `now()` | The creation time. The service sets it from its clock. The default is for SQL without the API. |
| `updated_at` | `TIMESTAMPTZ` | no | `now()` | The time of the last status change. A new task has the same value as `created_at` (A12). |

| Name | Rule | Reason |
| --- | --- | --- |
| `tasks_pkey` | Primary key on `id` | One row for each task |
| `fk_tasks_board` | Foreign key `board_id` to `boards (id)`, `ON DELETE RESTRICT` | A task cannot point to a board that does not exist (R3), also for SQL without the API. The database does not delete a board that has tasks (R4). `RESTRICT` checks at once and cannot be deferred. |
| `tasks_title_not_blank` | `CHECK (title ~ '\S')` | The title has at least one character that is not white space. The test `btrim(title) <> ''` would let a title with only tabs through. |
| `tasks_status_valid` | `CHECK (status IN ('TODO', 'IN_PROGRESS', 'DONE'))` | Only the three statuses are possible. |

## Indexes

| Index | Table and columns | Reason |
| --- | --- | --- |
| `boards_pkey` | `boards (id)` | The primary key |
| `tasks_pkey` | `tasks (id)` | The primary key |
| `tasks_board_id_idx` | `tasks (board_id)` | It serves the task list of one board (`WHERE board_id = ?`). It also serves the foreign key check when a board is deleted, because PostgreSQL does not make an index for a foreign key. The index starts with `board_id`, so that one index serves both queries. |

There is no other index, for these reasons:

- **`boards`.** The board list reads the whole table in the order of `created_at`, then `id`. It has no filter, and the table is small for a single user. Sorting a small table costs less than keeping an index for the order.
- **`tasks (status)` and `tasks (board_id, status)`.** The status filter is a short list for one board. The service filters that list, and no SQL query has a condition on `status`. An index would cost writes and help no query.
- **`tasks (created_at)`.** The task list is ordered inside one board, which `tasks_board_id_idx` has already narrowed.

## Types

- IDs are `BIGINT` identity columns (`GENERATED ALWAYS`): the database gives the value, and an insert cannot set it (A8).
- Times are `TIMESTAMPTZ`. The API sends them as ISO-8601 in UTC, and the UI shows local time (A9). PostgreSQL keeps microseconds, so the service cuts the clock value to microseconds before it saves it.
- The status is `VARCHAR(20)` with a `CHECK`, not an enumeration type.

## Rejected alternatives

- **A lookup table or a PostgreSQL `ENUM` for the status (dec_04).** Three fixed values do not need a join, and a lookup table would add a foreign key and a second table to maintain. An `ENUM` type is harder to change in a migration than a `CHECK`. The `CHECK` shows the rule in the table definition.
- **`ON DELETE CASCADE` for the tasks of a board (dec_03).** It would delete the tasks together with the board without any message. `RESTRICT` keeps the tasks until a user deletes them. The rule holds for all clients, also for SQL without the API.
- **`btrim(column) <> ''` for the name and title checks (dec_07).** It trims only spaces, so a text with only tabs gets through. The regular expression `~ '\S'` finds any character that is not white space.
- **A composite index `tasks (board_id, status)` (dec_08).** No query filters by status in SQL, so the second column would not be used.
- **A unique constraint on the board name (A3).** The case study gives no such rule. Two boards with the same name are permitted.

## Check the schema in a database

With the database from `docker compose up -d db` and the backend started once, in the repository root:

```bash
docker compose exec db psql -U taskmanager -d taskmanager -c '\d boards' -c '\d tasks'
```

The output has the same columns, types, defaults, constraints, and indexes as this document, and the table `flyway_schema_history` has the three migrations. PostgreSQL 18 also lists each `NOT NULL` rule as a named constraint. This document gives them in the column "Null".
