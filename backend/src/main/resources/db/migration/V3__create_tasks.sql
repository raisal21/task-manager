-- A task belongs to one board. RESTRICT: the database does not delete a board that has tasks (dec_03).
-- The constraints are named, so that the error mapping and the tests can tell which rule failed.
CREATE TABLE tasks (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    board_id    BIGINT       NOT NULL CONSTRAINT fk_tasks_board REFERENCES boards (id) ON DELETE RESTRICT,
    title       VARCHAR(200) NOT NULL CONSTRAINT tasks_title_not_blank CHECK (title ~ '\S'),
    description VARCHAR(2000),
    status      VARCHAR(20)  NOT NULL DEFAULT 'TODO'
                CONSTRAINT tasks_status_valid CHECK (status IN ('TODO', 'IN_PROGRESS', 'DONE')),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- It serves the task list of a board and the foreign key check when a board is deleted (dec_08).
CREATE INDEX tasks_board_id_idx ON tasks (board_id);
