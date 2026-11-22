# Homa

> Everyone deserves a well-cared home.

Homa is a marketplace that helps homeowners get the best homecare service providers. This branch builds the
backend fresh from the **Homa Business Rule** and its **development plan**:

- [`docs/homa-business-rule.md`](docs/homa-business-rule.md): every rule, with an ID (RES-1, PAY-8, ...) and a case.
- [`docs/homa-business-rule-development-plan.md`](docs/homa-business-rule-development-plan.md): bounded contexts,
  domain model, use cases, events, flows and the build order.
- [`docs/backend_module_discipline.md`](docs/backend_module_discipline.md): the layering rules every context follows.
- [`docs/decisions/`](docs/decisions/): runtime, persistence and the product owner's answers to open questions.

The Notion pages are the source of truth; the copies in `docs/` were ingested on 2026-11-22 (rules version 1.6). The rules open with three
principles (P1 paid is committed, P2 a change is a new booking, P3 a reservation keeps what it was booked with), and
§13 lists every Homa-set value; each value has one owner, a `<Context>Values` class in that context's domain.

## Layout

```text
pom.xml                          one Maven project: Java 21, Spring Boot, Spring JDBC + Flyway, Testcontainers, ArchUnit, JaCoCo, PIT
src/main/java/com/homa/
  kernel/                        Money, Percentage, TimeInterval, BusinessCalendar (JDK only)
  ie/port, ie/events/            integration event contracts
  reservation/ payment/ provider/ listing/ customer/ verification/ support/ rating/ conversation/ notification/
                                 one package root per bounded context, each with domain/, usecase/, infra/
  app/                           HomaApplication: the one deployable and composition root
  app/outbox/                    transactional outbox and relay: cross-context delivery, at least once, ordered per aggregate
src/test/java/com/homa/
  architecture/ArchitectureTest  the boundary rules (plan, "Source layout"), enforced on every build
  fixtures/HomaFixtures          §0 example setup: Aisyah, Kemas Home, the RM200 reservation
  values/HomaSetValuesTest       every row of §13 Homa-set values checked against its one owning constant
  <context>/...                  tests mirror the main packages; at least one test per rule ID, named in Homa's
                                 terms (camelCase), with the rule ID in @Tag and its case in @DisplayName
src/main/resources/db/migration Flyway migrations (PostgreSQL)
docs/decisions/                  ADRs: runtime (ECS/Fargate), persistence (Spring JDBC), outbox and idempotency
deploy/ecs/                      how the container runs on ECS/Fargate; Dockerfile at the root
tools/quality_gates.py           CRAP gate, per-layer mutation gate and ratchet
quality/ratchet.json             last passing mutation score per layer
```

CI (`.github/workflows/ci.yml`) runs both commands below on every push.

## Running the gates

```bash
mvn verify                          # unit tests, ArchUnit boundaries, Postgres ITs (needs Docker), mutation >= 80% overall (PIT)
python3 tools/quality_gates.py      # CRAP < 6; mutation >= 90% for kernel and each domain, >= 80% per usecase; ratchet
python3 tools/quality_gates.py --update   # after a pass: raise the ratchet
```

## Build progress

Vertical slices from the plan; inside each slice: domain, then usecase, then infra.

| Slice | Contexts | Rules | Status |
| --- | --- | --- | --- |
| 0. Skeleton | kernel, ie, app | PAY-8 (in `Money`) | Done: outbox, `ProcessedMessageStore`, ECS/Fargate packaging ([ADRs](docs/decisions/)) |
| 1. Book and get paid | Customer, Verification (minimal), Provider (minimal), Listing (minimal), Reservation, Payment | CUS-1–3, CUS-7, CUS-10, IDN-1, IDN-2, PRV-1–3, HLP-1–4, ROS-1–3, ROS-6, OFR-1–6, OFR-9–11, RES-1–12, ACC-1–3, JOB-1–6, PAY-1–3 | Domain layer: being aligned to rules v1.6 (owners, Verification); usecase and infra next |
| 2. Requests and withdrawals | Reservation, Payment | REQ-1–5 | `WITHDRAWN` and `withdraw()` in the domain; the rest not started |
| 3. Cancellations | Reservation, Payment | CAN-1–3, CCX-1–6, PCX-1–2, LMC-1–3, PAY-4–7 | Not started |
| 4. Team and roster | Provider, Reservation | PRV-6, PRV-11, HLP-5–10, ROS-4, ROS-5, ROS-7, ROS-8, NRS-1–3 | Not started |
| 5. Reports and no-shows | Support, Reservation, Payment | RPT-1–11, NSH-1–6, CHT-4 | Not started |
| 6. Compliance, suspensions and debt | Provider, Verification, Support, Customer, Reservation, Payment | MON-1–6, PRV-4, PRV-5, PRV-7–10, PRV-12, CUS-4–6, CUS-8, CUS-9, SUS-1–5, ROS-9, ROS-10, PAY-9–12 | Not started (CUS-4 and CUS-6 partly in the domain) |
| 7. Ratings and reliability | Rating, Listing | RTG-1–11 | Not started |
| 8. Chat and calendar sync | Conversation, Listing | CHT-1–3, CHT-5, OFR-7, OFR-8 | Not started |
| 9. Data retention | Payment, Support, Rating, Conversation, Reservation, Customer | DAT-1–15, IDN-3–5 | `BusinessCalendar.endOfYear` (DAT-2) in the kernel and the 30-day purge of technical rows; the rest not started |
| 10. External service failures | Payment, Provider, Listing, sign-in in every context | EXT-1–7 | Not started |
