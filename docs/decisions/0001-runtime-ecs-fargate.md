# 0001. Run Homa as a long-running container on ECS/Fargate

- Status: accepted
- Date: 2026-11-20
- Decided by: the product owner, closing pre-kickoff review item 6 in the development plan

## Context

The plan ships one deployable (`com.homa.app`) holding every context, with an in-process event bus and a transactional outbox. It left one runtime question open for slice 0: AWS Lambda with SnapStart, or ECS/Fargate. Spring Boot's cold start puts a function's startup budget at risk, and the outbox needs something to relay its rows after each transaction commits.

## Decision

Homa runs as a long-running container on ECS/Fargate behind a load balancer.

- **Background work runs in process.** The outbox relay (`OutboxRelaySchedule`) and the time-driven schedulers (expiry, check-in lock, payout release, rating windows) run on Spring's scheduler inside every task. A scheduler trigger lands in a context's `infra/transport/messaging` and calls an ordinary usecase port, as before. No EventBridge rules are needed for them.
- **Every task runs every job.** Jobs must be safe to run in several tasks at once. The relay is, through `FOR UPDATE SKIP LOCKED` (see 0002). Each scheduler written later must use the same row locks or an equivalent claim.
- **Packaging.** A `Dockerfile` runs the layered Spring Boot jar as a non-root user. Configuration comes from environment variables, with the database password injected from Secrets Manager. Logs are structured JSON on stdout. Health is the actuator's readiness and liveness groups over HTTP, and shutdown is graceful. The task's contract is in `deploy/ecs/README.md`.
- **Health belongs to transport.** Probes come from Spring Boot actuator. No context's domain or usecase knows it is probed.

## Consequences

- There is no cold-start risk to manage. The discipline's startup gate becomes a deployment-speed concern, not a request-latency one.
- The relay delivers within about a second of commit (`homa.outbox.relay.fixed-delay`) without an external trigger.
- Tasks are billed while idle, unlike Lambda. That is acceptable at Homa's expected load.
- A stopped task can drop a relay batch or a scheduler run halfway. That is safe because delivery is at least once and every handler is idempotent (0002).
- Changing the runtime later changes only `app/` and `deploy/`, as the discipline requires. Splitting out Payment first (plan, Deployment) would move the relay's delivery onto a broker inside `infra/event` and `app/`.
