# Task Manager schema

The PostgreSQL schema of the backend. Flyway makes it from the migrations in `src/main/resources/db/migration` when the backend starts. The migrations are the source. This document gives the reason for each choice.

| Migration | Content |
| --- | --- |
| `V1__create_boards.sql` | The `boards` table |
| `V2__boards_name_not_blank.sql` | The constraint `boards_name_not_blank` |
| `V3__create_tasks.sql` | The `tasks` table, its constraints, and its index |

## tasks

| Column | Type | Null | Default | Reason |
| --- | --- | --- | --- | --- |
| `id` | `BIGINT GENERATED ALWAYS AS IDENTITY` | no | identity | The primary key. A short number is easy to type in curl. The API sends it as a JSON number. |
| `board_id` | `BIGINT` | no | none | The board of the task. A task always has a board. |
| `title` | `VARCHAR(200)` | no | none | The title. 200 characters at most (A6). |
| `description` | `VARCHAR(2000)` | yes | none | An optional description. 2000 characters at most (A6). An empty description is `NULL` (A10). |
| `status` | `VARCHAR(20)` | no | `'TODO'` | `TODO`, `IN_PROGRESS`, or `DONE`. |
| `created_at` | `TIMESTAMPTZ` | no | `now()` | The creation time. The service sets it from its clock. The default is for SQL without the API. |
| `updated_at` | `TIMESTAMPTZ` | no | `now()` | The time of the last change. A new task has the same value as `created_at`. |

### Constraints and index

| Name | Rule | Reason |
| --- | --- | --- |
| `tasks_pkey` | Primary key on `id` | One row for each task |
| `fk_tasks_board` | Foreign key `board_id` to `boards (id)`, `ON DELETE RESTRICT` | A task cannot point to a board that does not exist (R3), also for SQL without the API. The database does not delete a board that has tasks (R4). |
| `tasks_title_not_blank` | `CHECK (title ~ '\S')` | The title has at least one character that is not white space. The test `btrim(title) <> ''` would let a title with only tabs through. |
| `tasks_status_valid` | `CHECK (status IN ('TODO', 'IN_PROGRESS', 'DONE'))` | Only the three statuses are possible. |
| `tasks_board_id_idx` | Index on `tasks (board_id)` | It serves the task list of one board and the foreign key check when a board is deleted. |

No index covers `status`. The status filter is a small list for one board. The service filters that list, and no SQL query uses the status.

### Rejected alternatives

- **`ON DELETE CASCADE`.** It would delete the tasks together with the board without any message. `RESTRICT` keeps the tasks until a user deletes them. The rule holds for all clients, also for SQL without the API.
- **A lookup table or a PostgreSQL `ENUM` for the status.** Three fixed values do not need a join. The `CHECK` shows the rule in the table definition.

## boards

| Column | Type | Null | Default | Reason |
| --- | --- | --- | --- | --- |
| `id` | `BIGINT GENERATED ALWAYS AS IDENTITY` | no | identity | The primary key |
| `name` | `VARCHAR(100) NOT NULL` | no | none | The name. 100 characters at most (A6). |
| `created_at` | `TIMESTAMPTZ` | no | `now()` | The creation time. The service sets it from its clock. |

| Name | Rule | Reason |
| --- | --- | --- |
| `boards_pkey` | Primary key on `id` | One row for each board |
| `boards_name_not_blank` | `CHECK (name ~ '\S')` | The database also rejects a name with only spaces or tabs. It is the backstop for the rule in the service. |

Board names are not unique (A3). Two boards can have the same name.
