# CLIMS Asset Management — Local Setup Guide

This guide shows how to clone the repository, run MySQL locally (native or Docker), run the Spring Boot backend, and test the HTTP endpoints. It is written for Windows / PowerShell (pwsh) users but includes Linux/macOS equivalents where applicable.

## What I inspected
- Java version required: 21 (from `backend/backend/pom.xml`)
- Backend configuration: `backend/backend/target/classes/application*.properties` — the default runtime `application.properties` expects a MySQL DB at `jdbc:mysql://localhost:3306/clims` by default, but the `dev` profile uses an in-memory H2 DB unless you override environment variables.
- Flyway is enabled and will run migrations on startup; the database must exist before the application runs.

## 1) Clone the repo
Open PowerShell and run:

```powershell
# Clone (HTTPS)
git clone https://github.com/Elbowg/CLIMS-Asset-Management-System-224-.git
cd CLIMS-Asset-Management-System-224--1
```

Replace the URL with your SSH remote if you prefer.

## 2) Prerequisites
- Java 21 JDK installed and on PATH (check with `java -version`).
- (Optional) Maven, though the project includes the Maven wrapper (`mvnw.cmd`) under `backend/backend`.
- MySQL 8+ (local install) or Docker (recommended for quick setup).
- PowerShell (pwsh) — commands below are pwsh-compatible.

## 3) Set up MySQL
You can either run MySQL locally (install) or use Docker. The app expects a database named `clims` by default.

Option A — Docker (quick):

```powershell
# Run a MySQL container with a clims database and a password
docker run --name clims-mysql -e MYSQL_ROOT_PASSWORD=secret -e MYSQL_DATABASE=clims -p 3306:3306 -d mysql:8.0
```

Notes:
- This creates database `clims` and exposes port 3306.
- Use `secret` as the password in examples below or change it and adapt env vars.

Option B — Native MySQL (Windows installer / service):

1. Install MySQL 8+ and start the service.
2. Open a PowerShell MySQL client or MySQL Workbench and run (use root or a privileged account):

```sql
CREATE DATABASE clims CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'clims_user'@'localhost' IDENTIFIED BY 'clims_password';
GRANT ALL PRIVILEGES ON clims.* TO 'clims_user'@'localhost';
FLUSH PRIVILEGES;
```

If you want to use `root` with no password (matching defaults in `application.properties`), you can skip creating a user, but it's not recommended.

Important: Flyway will run migrations on startup but will not create the `clims` DB for you — create the DB before you start the app.

## 4) Configure environment variables (PowerShell)
Set the DB connection info and optional JWT secret. You have two options: set them in the shell for the session, or set persistent user environment variables.

Temporary (current session only):

```powershell
$env:DB_URL = 'jdbc:mysql://localhost:3306/clims?useSSL=false&serverTimezone=UTC'
$env:DB_USERNAME = 'root'            # or clims_user
$env:DB_PASSWORD = 'secret'          # or clims_password
$env:JWT_SECRET = 'your-base64-256bit-secret-here'
```

Persistent (PowerShell):

```powershell
setx DB_URL "jdbc:mysql://localhost:3306/clims?useSSL=false&serverTimezone=UTC"
setx DB_USERNAME "root"
setx DB_PASSWORD "secret"
setx JWT_SECRET "your-base64-256bit-secret-here"
```

Notes:
- The default `application.properties` falls back to `jdbc:mysql://localhost:3306/clims` and `root` with empty password if env vars are not set.
- For development you can keep the default H2 in-memory DB (see `application-dev.properties`) — set the `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` env vars if you want the `dev` profile to use MySQL.

## 5) Build and run the Spring Boot backend
There are two convenient options: use the included Maven wrapper or run the VS Code tasks configured in the workspace.

From repo root (PowerShell):

```powershell
# Move into backend folder
cd .\backend\backend

# Build (skip tests)
.\mvnw.cmd -DskipTests package

# Run (dev profile environment is used by the VS Code run task; below runs the app with default profile)
.\mvnw.cmd spring-boot:run
```

Or use the provided VS Code task "Backend: Run (dev)" which runs the app with `SPRING_PROFILES_ACTIVE=dev` environment variable. When started with the `dev` profile, the app defaults to an in-memory H2 DB unless you override environment variables.

If you want to run the packaged JAR:

```powershell
# After building
java -jar target\backend-0.0.1-SNAPSHOT.jar
```

The app runs on port 8080 by default (`server.port=8080`).

## 6) What happens on startup
- Flyway executes migrations from `classpath:db/migration` to create the schema/tables.
- If the DB schema does not match `spring.jpa.hibernate.ddl-auto=validate`, startup will fail with an Hibernate validation exception — ensure Flyway successfully ran migration scripts against your `clims` DB.

## 7) Test endpoints
Open a new PowerShell window or use curl. The following examples assume the backend runs on `http://localhost:8080`.

Health check (Actuator):

```powershell
Invoke-RestMethod -Uri http://localhost:8080/actuator/health
```

OpenAPI JSON (export):

```powershell
# View API docs JSON
Invoke-RestMethod -Uri http://localhost:8080/v3/api-docs | ConvertTo-Json -Depth 10

# Export to docs/openapi/openapi-v1.json (task exists in the workspace as "Export OpenAPI v1")
Invoke-WebRequest -Uri 'http://localhost:8080/v3/api-docs' -OutFile '.\docs\openapi\openapi-v1.json'
```

Swagger UI in browser:

- http://localhost:8080/swagger-ui/index.html

Example: Get assets (replace with concrete API paths from OpenAPI if different):

```powershell
Invoke-RestMethod -Uri http://localhost:8080/api/assets -Method GET -Headers @{ Authorization = 'Bearer <your-jwt>' }
```

If you prefer curl (WSL or curl for Windows):

```powershell
curl http://localhost:8080/actuator/health
curl http://localhost:8080/v3/api-docs -o docs/openapi/openapi-v1.json
```

## 8) Postman collection and OpenAPI
- The repository contains a Postman collection at `backend/backend/postman_collection.json` that you can import into Postman.
- A generated OpenAPI file is available at `docs/openapi/openapi-v1.json` — you can also re-export it from a running instance as shown above.

## 9) Troubleshooting
- Flyway/Hibernate validation errors: ensure the `clims` database exists and that Flyway can run migrations. Check application logs (console or `logs/` when enabled) for Flyway messages.
- Cannot connect to MySQL: ensure MySQL is listening on the expected port and the credentials match. Use `mysql -u root -p -h 127.0.0.1 -P 3306` to test connectivity.
- Java version mismatch: the project uses Java 21. If you have an older JDK, install Java 21 or point `JAVA_HOME` to a Java 21 installation.
- If you see the dev H2 DB and want MySQL in dev: set `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` environment variables before starting the app or edit `application-dev.properties` (only for local convenience).

## 10) Useful commands summary (PowerShell)

```powershell
# Clone
git clone https://github.com/Elbowg/CLIMS-Asset-Management-System-224-.git
cd CLIMS-Asset-Management-System-224--1\backend\backend

# Start local MySQL (docker)
docker run --name clims-mysql -e MYSQL_ROOT_PASSWORD=secret -e MYSQL_DATABASE=clims -p 3306:3306 -d mysql:8.0

# Set env vars for current session
$env:DB_URL = 'jdbc:mysql://localhost:3306/clims?useSSL=false&serverTimezone=UTC'
$env:DB_USERNAME = 'root'
$env:DB_PASSWORD = 'secret'

# Build and run
.\mvnw.cmd -DskipTests package
.\mvnw.cmd spring-boot:run

# Health check
Invoke-RestMethod -Uri http://localhost:8080/actuator/health

# Export OpenAPI
Invoke-WebRequest -Uri 'http://localhost:8080/v3/api-docs' -OutFile '.\docs\openapi\openapi-v1.json'
```

## Where to go next
- Import `backend/backend/postman_collection.json` into Postman for pre-configured requests.
- If you want a Docker Compose setup that includes MySQL and the backend, let me know — I can add a minimal `docker-compose.yml` to the repo.

---

If you'd like, I can now:
- Add a `docker-compose.yml` to run MySQL + the backend together, or
- Add a short PowerShell script to create the `clims` DB and user, or
- Create a VS Code launch/task entry to run the backend with environment variables preconfigured.

Tell me which of those you'd like next and I will implement it.