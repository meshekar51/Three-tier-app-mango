-- 02-seed.sql
-- Sample rows so the UI isn't empty on first load.

INSERT INTO tasks (title, done) VALUES
    ('write Dockerfile for frontend', FALSE),
    ('write Dockerfile for backend',  FALSE),
    ('write Dockerfile for db',       FALSE),
    ('create a docker network',       FALSE),
    ('docker compose up',             FALSE);
