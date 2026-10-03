#!/bin/sh
# healthcheck.sh — exit 0 when Postgres accepts connections AND the tasks table exists.
# Use in Dockerfile:  HEALTHCHECK CMD /usr/local/bin/healthcheck.sh
set -e

pg_isready -U "${POSTGRES_USER:-appuser}" -d "${POSTGRES_DB:-tasks}" -h 127.0.0.1 -q

psql -U "${POSTGRES_USER:-appuser}" -d "${POSTGRES_DB:-tasks}" -h 127.0.0.1 -tAc \
  "SELECT 1 FROM tasks LIMIT 1" > /dev/null
