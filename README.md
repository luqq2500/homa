# Homa

> Everyone deserves a well-cared home.

Homa helps homeowners find, reserve and get trusted home service providers. This repository holds the
backend, built context by context following the backend module discipline: **domain first**, then
usecase, then infra.

## Layout

```text
pom.xml                 parent build: Java 21, JUnit 5, ArchUnit, JaCoCo, PIT
kernel/                 shared kernel: Money, Percentage (Java standard library only)
catalogue/              job list, tool list, service area, homepage shelves
policy/                 Homa-set values and their versions
tools/quality_gates.py  CRAP gate and mutation ratchet
quality/ratchet.json    last passing mutation score per module
```

Each bounded context is its own Maven module. A context depends on `kernel` only, never on another
context, so context isolation is enforced by the compiler. Inside a context, the layers are packages
(`com.homa.<context>.domain`, later `usecase` and `infra`), and an ArchUnit test in each module checks
that the domain imports only itself, `kernel` and `java..`.

## Running the gates

```bash
mvn verify                          # unit tests, dependency rule (ArchUnit), mutation >= 90% (PIT)
python3 tools/quality_gates.py      # CRAP < 6 per method, mutation ratchet
python3 tools/quality_gates.py --update   # after a pass: raise the ratchet
```

Reports: `<module>/target/site/jacoco/` (coverage) and `<module>/target/pit-reports/` (mutation).

## Domain phase progress

Build order from the domain plan:

| Step | Context | Status |
|---|---|---|
| 0 | `kernel` | Done |
| 1 | `catalogue` | Done |
| 2 | `policy` | Done |
| 3 | `account` | Next |
| 4–10 | `offering`, `availability`, `reservation`, `payment`, `resolution`, `conversation`, `discovery` | To do |
| 11 | `ie/` integration event contracts | To do, after every context passes its gates |

### Decisions taken while building (to fold back into the plan)

- **`HomaPolicy.initial(Instant now)`** takes the time version 1 takes effect, following the "time is
  passed in" convention. Each version records when it took effect, and a change cannot take effect
  before the version in force.
- **Snapshots across contexts.** A domain cannot import another context's domain, so `reservation`
  cannot take policy's `PolicySnapshot` or offering's `OfferingSnapshot` types directly. Each consuming
  context defines its own snapshot value holding just what it needs, and its usecase layer fills it in.
- **`City` per context.** `City` is not in the kernel. Each context that needs one defines its own,
  as `catalogue` does.
- **Rules added from the spec:**
  - The Flexible tier stays at 0%, because the spec defines Flexible as free cancellation.
  - The provider no-show fine is never below the late cancellation fine ("not showing up is never
    cheaper than cancelling late").
  - The no-show ceiling is checked in both directions: a tier fee can't rise above the customer
    no-show fee, and the customer no-show fee can't drop below a tier fee.
- **Catalogue details:** codes are upper-case words joined by underscores (`KITCHEN_GREASE`); a job or
  tool is retired only once and its code is never reused; shelves are ordered from 1. A preset search
  needs at least one job, hour and helper. Homa's upper limits (6 hours, 10 helpers) can change, so
  they are checked when the search runs, in `discovery`.
- **Jobs and tools have no `equals`.** They are only reached through their list, keyed by code.
