# Homa

> Everyone deserves a well-cared home.

Homa is a marketplace that helps homeowners get the best homecare service providers. This branch builds the
backend fresh from the **Homa Business Rule** and its **development plan**:

- [`docs/homa-business-rule.md`](docs/homa-business-rule.md): every rule, with an ID (RES-1, PAY-8, ...) and a case.
- [`docs/homa-business-rule-development-plan.md`](docs/homa-business-rule-development-plan.md): bounded contexts,
  domain model, use cases, events, flows and the build order.
- [`docs/backend_module_discipline.md`](docs/backend_module_discipline.md): the layering rules every context follows.

The Notion pages are the source of truth; the copies in `docs/` were ingested on 2026-11-20.

## Layout

```text
pom.xml                          one Maven project: Java 21, Spring Boot, JUnit 5, ArchUnit, JaCoCo, PIT
src/main/java/com/homa/
  kernel/                        Money, Percentage, TimeInterval (JDK only)
  ie/port, ie/events/            integration event contracts
  reservation/ payment/ provider/ listing/ customer/ support/ rating/ conversation/ notification/
                                 one package root per bounded context, each with domain/, usecase/, infra/
  app/                           HomaApplication: the one deployable and composition root
src/test/java/com/homa/
  architecture/ArchitectureTest  the boundary rules (plan, "Source layout"), enforced on every build
  fixtures/HomaFixtures          §0 example setup: Aisyah, Kemas Home, the RM200 reservation
  <context>/...                  tests mirror the main packages; one intent-named test per rule ID
tools/quality_gates.py           CRAP gate, per-layer mutation gate and ratchet
quality/ratchet.json             last passing mutation score per layer
```

## Running the gates

```bash
mvn verify                          # unit tests, ArchUnit boundaries, mutation >= 80% overall (PIT)
python3 tools/quality_gates.py      # CRAP < 6; mutation >= 90% for kernel and each domain, >= 80% per usecase; ratchet
python3 tools/quality_gates.py --update   # after a pass: raise the ratchet
```

## Build progress

Vertical slices from the plan; inside each slice: domain, then usecase, then infra.

| Slice | Contexts | Rules | Status |
| --- | --- | --- | --- |
| 0. Skeleton | kernel, ie, app | PAY-8 (in `Money`) | Done |
| 1. Book and get paid | Customer, Listing (minimal), Reservation, Payment | CUS-1–3, OFR-1–6, OFR-9, OFR-10, RES-1–12, ACC-1–3, JOB-1–6, PAY-1–3 | In progress: domain layer |
| 2. Requests and edits | Reservation, Payment | REQ-1–9 | Not started |
| 3. Cancellations | Reservation, Payment | CAN-1–3, CCX-1–6, PCX-1–2, LMC-1–3, PAY-4–7 | Not started |
| 4. Team and roster | Provider, Reservation | PRV-1–3, PRV-6, HLP-1–8, ROS-1–8, NRS-1–3 | Not started |
| 5. Reports and no-shows | Support, Reservation, Payment | RPT-1–11, NSH-1–6, CHT-4 | Not started |
| 6. Compliance, suspensions and debt | Provider, Customer, Reservation, Payment | MON-1–4, PRV-4, PRV-5, CUS-4–6, SUS-1–3, ROS-9, ROS-10, PAY-9–11 | Not started |
| 7. Ratings and reliability | Rating, Listing | RTG-1–11 | Not started |
| 8. Chat and calendar sync | Conversation, Listing | CHT-1–3, CHT-5, OFR-7, OFR-8 | Not started |
