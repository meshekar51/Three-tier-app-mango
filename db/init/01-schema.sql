-- 01-schema.sql
-- Copy the whole init/ folder to /docker-entrypoint-initdb.d/ in the postgres image.
-- Files run in alphabetical order, ONLY when the data directory is empty (first start).

CREATE TABLE IF NOT EXISTS tasks (
    id          SERIAL       PRIMARY KEY,
    title       VARCHAR(200) NOT NULL,
    done        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_tasks_done ON tasks (done);
