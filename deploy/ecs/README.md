# Homa on ECS/Fargate

The runtime decision is in [docs/decisions/0001-runtime-ecs-fargate.md](../../docs/decisions/0001-runtime-ecs-fargate.md).
This folder will hold the infrastructure as code. For now it only states what the task definition must provide.

## Image

Build the jar, then the image from the repository root, and push the same image to every environment:

```sh
mvn -B verify
docker build -t homa:<git-sha> .
```

The image runs the layered Spring Boot jar as the non-root user `homa` (uid 10001) and listens on port 8080.

## Environment

| Variable | Required | Default | Purpose |
| --- | --- | --- | --- |
| `HOMA_DB_URL` | yes | `jdbc:postgresql://localhost:5432/homa` (local only) | JDBC URL of the PostgreSQL database |
| `HOMA_DB_USERNAME` | yes | `homa` (local only) | Database user |
| `HOMA_DB_PASSWORD` | yes | none | Database password. Inject it from Secrets Manager with the container's `secrets`, never as a plain `environment` value |
| `HOMA_DB_POOL_SIZE` | no | `10` | Hikari pool size per task. Keep tasks × pool size below the database's connection limit. The relay needs 2 connections while it delivers |
| `PORT` | no | `8080` | HTTP port |
| `HOMA_SHUTDOWN_TIMEOUT` | no | `20s` | How long a stopping task finishes in-flight requests and the running relay batch. Keep it below the container's `stopTimeout` (30s by default) |
| `HOMA_LOG_FORMAT` | no | `ecs` | Structured console log format: `ecs`, `logstash` or `gelf` |
| `HOMA_OUTBOX_RELAY_ENABLED` | no | `true` | Run the outbox relay in this task. Safe to leave on in every task |
| `HOMA_OUTBOX_RELAY_FIXED_DELAY` | no | `PT1S` | Pause between relay runs (ISO-8601 duration) |
| `HOMA_OUTBOX_RELAY_BATCH_SIZE` | no | `100` | Most outbox rows one relay run handles |
| `JAVA_TOOL_OPTIONS` | no | `-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError` | JVM flags; the heap follows the task's memory |

Flyway migrates the database on startup. Every task may start at once: Flyway takes a lock, so only one applies a migration.

## Health checks

| Check | Path | Meaning |
| --- | --- | --- |
| Load balancer target group | `GET /actuator/health/readiness` | `200` when the task can serve: started, database reachable. It stops reporting `UP` once shutdown begins. Set the target group's deregistration delay so ECS drains the task before it is stopped |
| Container health check (optional) | `GET /actuator/health/liveness` | `200` while the process works. It ignores the database, so a database outage never makes ECS restart healthy tasks |

Only `health` is exposed under `/actuator`. The base image ships `curl`, so a container health check can be:

```json
"healthCheck": {
  "command": ["CMD-SHELL", "curl -fs http://localhost:8080/actuator/health/liveness || exit 1"],
  "interval": 15, "timeout": 5, "retries": 3, "startPeriod": 60
}
```

## Logs

Logs go to stdout, one JSON record per line (Elastic Common Schema). Use the `awslogs` log driver; nothing is written to the container's disk.

## Shutdown

ECS sends `SIGTERM`, waits `stopTimeout`, then sends `SIGKILL`. On `SIGTERM` the task stops accepting requests, finishes the ones in flight and the current relay batch, and closes the database pool. Work that is interrupted anyway is safe: an outbox row that was not marked published is delivered again by another task, and consumers ignore the repeat.
