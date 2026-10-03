# three-tier-app

Task board: **nginx frontend → Spring Boot (Java 21) API → Postgres 16**.
Each folder is a self-contained Docker build context. Dockerfiles are yours to write.

```
three-tier-app/
├── frontend/                       → build context for the web image
│   ├── index.html
│   ├── style.css
│   ├── app.js
│   ├── nginx.conf                  → /etc/nginx/conf.d/default.conf
│   └── .dockerignore
│
├── backend/                        → build context for the API image
│   ├── pom.xml
│   ├── src/main/java/com/demo/tasks/
│   │   ├── TasksApplication.java
│   │   └── TaskController.java
│   ├── src/main/resources/
│   │   └── application.properties
│   ├── .env.example
│   └── .dockerignore
│
├── db/                             → build context for the database image
│   ├── init/
│   │   ├── 01-schema.sql           → /docker-entrypoint-initdb.d/
│   │   └── 02-seed.sql             → /docker-entrypoint-initdb.d/
│   ├── config/
│   │   └── postgresql.conf         → /etc/postgresql/postgresql.conf
│   ├── healthcheck.sh              → /usr/local/bin/healthcheck.sh
│   ├── .env.example
│   └── .dockerignore
│
└── docs/
    └── screenshot.png              → UI reference (not part of any image)
```

## Run locally (no Docker)

Locally there's no nginx: Spring Boot serves the `frontend/` folder itself, so UI and API share one origin on **http://localhost:5000**.

```
 browser ──► localhost:5000 ──► Spring Boot ──┬── /          → files from ../frontend/
                                              └── /api/*     → TaskController ──► Postgres :5432
```

### 1. Install prerequisites

| Tool | Version | Get it |
|------|---------|--------|
| PostgreSQL | 16 | postgresql.org/download (Windows installer includes `psql`) |
| JDK | 21 | adoptium.net (Temurin 21) |
| Maven | 3.9+ | maven.apache.org/download.cgi |

Check:

```powershell
psql --version     # psql (PostgreSQL) 16.x
java -version      # openjdk version "21.x"
mvn -v             # Apache Maven 3.9.x
```

### 2. Create the database

Run from the project root. You'll be prompted for the `postgres` superuser password you set during install.

**Windows (PowerShell)**

```powershell
psql -h localhost -U postgres -c "CREATE USER appuser WITH PASSWORD 'apppass';"
psql -h localhost -U postgres -c "CREATE DATABASE tasks OWNER appuser;"

$env:PGPASSWORD = "apppass"
psql -h localhost -U appuser -d tasks -f db/init/01-schema.sql
psql -h localhost -U appuser -d tasks -f db/init/02-seed.sql
```

**Linux / macOS (bash)**

```bash
sudo -u postgres psql -c "CREATE USER appuser WITH PASSWORD 'apppass';"   # macOS Homebrew: drop "sudo -u postgres", use: psql postgres -c ...
sudo -u postgres psql -c "CREATE DATABASE tasks OWNER appuser;"

export PGPASSWORD=apppass
psql -h localhost -U appuser -d tasks -f db/init/01-schema.sql
psql -h localhost -U appuser -d tasks -f db/init/02-seed.sql
```

Expected output:

```
CREATE ROLE
CREATE DATABASE
CREATE TABLE
CREATE INDEX
INSERT 0 5
```

> `db/config/postgresql.conf` and `healthcheck.sh` are for the Docker image only. A local install uses its own config.

### 3. Start the backend (also serves the frontend)

**Windows (PowerShell)**

```powershell
cd backend
$env:DB_HOST = "localhost"
mvn spring-boot:run "-Dspring-boot.run.arguments=--spring.web.resources.static-locations=file:../frontend/"
```

**Linux / macOS (bash)**

```bash
cd backend
DB_HOST=localhost mvn spring-boot:run \
  -Dspring-boot.run.arguments="--spring.web.resources.static-locations=file:../frontend/"
```

`DB_HOST=localhost` is required. The default (`db`) is the Docker container name and won't resolve locally. Other `DB_*` defaults already match step 2.

First run downloads dependencies (1–3 min). Expected output (timestamps/PIDs trimmed):

```
  :: Spring Boot ::                (v3.3.4)

INFO  Starting TasksApplication using Java 21 ...
INFO  Tomcat initialized with port 5000 (http)
INFO  Root WebApplicationContext: initialization completed in 812 ms
INFO  Adding welcome page: URL [file:../frontend/index.html]
INFO  Tomcat started on port 5000 (http) with context path '/'
INFO  Started TasksApplication in 2.3 seconds (process running for 2.6)
```

When the first request hits the DB you'll also see `HikariPool-1 - Start completed.`

Leave this terminal running. `Ctrl+C` stops it.

### 4. Open the app

Go to **http://localhost:5000**

![task board UI](docs/screenshot.png)

What you should see:

- Status line **green**: `backend: ok | db: connected`
- The 5 seeded tasks (the screenshot has 2 marked done and 1 added)
- **Type + `add`** creates a task · **click a task** toggles done (strike-through) · **`x`** deletes it
- Refresh the page: changes persist (they're in Postgres)

### 5. Check the API directly (optional)

Use `curl.exe` on Windows (plain `curl` in PowerShell 5 is an alias for `Invoke-WebRequest`).

```bash
curl -s http://localhost:5000/api/health
```
```json
{"status":"ok","db":"connected"}
```

```bash
curl -s http://localhost:5000/api/tasks
```
```json
[{"id":1,"title":"write Dockerfile for frontend","done":false,"created_at":"2026-10-03T16:01:12.345+00:00"},{"id":2,"title":"write Dockerfile for backend","done":false,"created_at":"2026-10-03T16:01:12.345+00:00"}, ...]
```

Write calls (bash; on Windows use the UI for these, since JSON quoting in PowerShell is painful):

```bash
curl -s -X POST  localhost:5000/api/tasks   -H "Content-Type: application/json" -d '{"title":"learn docker"}'
# → 201  {"id":6,"title":"learn docker","done":false,"created_at":"..."}

curl -s -X PATCH localhost:5000/api/tasks/6 -H "Content-Type: application/json" -d '{"done":true}'
# → 200  {"id":6,"title":"learn docker","done":true,"created_at":"..."}

curl -s -o /dev/null -w "%{http_code}\n" -X DELETE localhost:5000/api/tasks/6
# → 204
```

### Troubleshooting

| You see | Cause | Fix |
|---------|-------|-----|
| Status red: `backend: unreachable` | Backend not running, or you opened `index.html` by double-click (`file://`) | Start step 3, use `http://localhost:5000` |
| Status red: `db: down` | `DB_HOST` not set to `localhost`, Postgres service stopped, or wrong password | Set `DB_HOST`, start Postgres, re-check step 2 |
| `Port 5000 was already in use` | Another app on 5000 (macOS: AirPlay Receiver) | Add `--server.port=5050` to the run arguments, open `localhost:5050` |
| Page loads but no styling/JS | Wrong `static-locations` path | Run the command from inside `backend/`, keep the trailing `/` in `file:../frontend/` |
| `relation "tasks" does not exist` | Init scripts not run | Re-run the two `psql -f` lines in step 2 |

## Docker: what each image needs

| Image    | Base image | Port | Build / run |
|----------|------------|------|-------------|
| frontend | `nginx:alpine` | 80 | Copy html/css/js → `/usr/share/nginx/html/`, `nginx.conf` → `/etc/nginx/conf.d/default.conf` |
| backend  | build `maven:3.9-eclipse-temurin-21`, run `eclipse-temurin:21-jre` | 5000 | `mvn package -DskipTests` → `target/app.jar`, run `java -jar app.jar` |
| db       | `postgres:16-alpine` | 5432 | Copy `init/` → `/docker-entrypoint-initdb.d/`, `config/postgresql.conf` → `/etc/postgresql/`, `healthcheck.sh` → `/usr/local/bin/`. Start with `postgres -c config_file=/etc/postgresql/postgresql.conf` |

## Env vars (see each folder's `.env.example`)

| Image   | Vars |
|---------|------|
| db      | `POSTGRES_DB=tasks` `POSTGRES_USER=appuser` `POSTGRES_PASSWORD=apppass` |
| backend | `DB_HOST=db` `DB_PORT=5432` `DB_NAME=tasks` `DB_USER=appuser` `DB_PASSWORD=apppass` |

Backend defaults already match, so env vars are optional unless you change them.

## Gotchas

- **Names**: nginx proxies `/api/` to `http://backend:5000`. Backend connects to host `db`. Container names (or compose service names) must be `backend` and `db`.
- **Network**: all three on one user-defined network (`docker network create`). The default bridge has no DNS.
- **Init scripts run once**: only when the data volume is empty. Changed the SQL? `docker volume rm` it.
- **Backend multi-stage**: copy `pom.xml` → `mvn dependency:go-offline` → copy `src/` → `mvn package`. The dependency layer gets cached and the final JRE image is ~250 MB instead of ~800 MB.
- **Backend start order**: the API starts even if the db isn't ready. Status line shows `db: down` until Postgres is up.

## API

| Method | Path | Body |
|--------|------|------|
| GET    | /api/health     | – |
| GET    | /api/tasks      | – |
| POST   | /api/tasks      | `{"title": "..."}` |
| PATCH  | /api/tasks/{id} | `{"done": true}` |
| DELETE | /api/tasks/{id} | – |

## Done when

Open `http://localhost:<frontend-port>` → status line green (`db: connected`) and 5 seeded tasks show.
