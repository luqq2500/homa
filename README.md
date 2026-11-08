# Homa

> Everyone deserves a well-cared home.

Homa helps homeowners find, reserve and get trusted home service providers. This repository holds the
backend, built context by context following the backend module discipline: **domain first**, then
usecase, then infra.

## Layout

```text
pom.xml                         one Maven project: Java 21, JUnit 5, ArchUnit, JaCoCo, PIT
src/main/java/
  kernel/                       shared kernel: Money, Percentage (Java standard library only)
  catalogue/domain/             job list, tool list, service area, homepage shelves
  policy/domain/                Homa-set values and their versions
  account/domain/               customer and provider accounts, helpers, phone and eKYC verification, re-verification
  offering/domain/              a provider's listing and the rules for going live
  availability/domain/          a provider's windows, synced busy blocks, reservation holds and buffer
  reservation/domain/           enquiries and the reservation lifecycle, with every time and money rule
  payment/domain/               payments, fines, payouts and fine deductions
  resolution/domain/            the founders' case queue and decisions
  conversation/domain/          chats for enquiries and reservations
  discovery/domain/             search criteria and exact, close or no matches
  feedback/domain/              customers' ratings of providers and helpers, and the averages shown
  ie/port/                      one integration event interface per publishing context
  ie/events/                    the integration event records
src/test/java/                  the same packages, one test class per feature
docs/                           the product description, the plans, the module discipline, the domain audit and the domain closing report
tools/quality_gates.py          CRAP gate, per-context mutation gate and ratchet
quality/ratchet.json            last passing mutation score per layer
```

Each bounded context is a top-level package, with its layers below it: `catalogue.domain` now, and
`catalogue.usecase` and `catalogue.infra` later, each with its own `exception` package. An ArchUnit
test in each context's test package is the dependency-rule gate. It checks that the domain imports only
itself, `kernel` and `java..`, so no context imports another. `kernel` imports only `java..`.

When a new context is added, add its package to PIT's `targetClasses` and `targetTests` in `pom.xml`;
the gate script fails if one is missing.

## Running the gates

```bash
mvn verify                          # unit tests, dependency rule (ArchUnit), mutation >= 90% (PIT)
python3 tools/quality_gates.py      # CRAP < 6 per method; mutation >= 90% for kernel and each domain (usecase >= 80% later); ratchet
python3 tools/quality_gates.py --update   # after a pass: raise the ratchet
```

Reports: `target/site/jacoco/` (coverage) and `target/pit-reports/` (mutation).

## Domain phase progress

Build order from the domain plan:

| Step | Context | Status |
|---|---|---|
| 0 | `kernel` | Done |
| 1 | `catalogue` | Done |
| 2 | `policy` | Done |
| 3 | `account` | Done |
| 4 | `offering` | Done |
| 5 | `availability` | Done |
| 6 | `reservation`, all seven slices | Done |
| 7 | `payment` | Done |
| 8 | `resolution` | Done |
| 9 | `conversation` | Done |
| 10 | `discovery` | Done |
| 11 | `ie/` integration event contracts | Done |
| 12 | `feedback`, added after closing: two Homa-set values in `policy`, the `feedback` domain, `RatingSubmittedIE` and `RatingHiddenIE` | Done |
| 13 | Team membership, changed after closing: helpers join a provider's team only once their eKYC is verified; `HelperInvitedIE`, `HelperJoinedTeamIE` and `HelperInviteRejectedIE` | Done |
| 14 | Periodic re-verification, changed after closing: every 3 months, with lapsing, early re-checks and listing pause; eight new events | Done |

The domain phase is complete: see `docs/domain-closing-report.md`, and its addenda for `feedback`, team membership and re-verification.

### Usecase phase

Paused: the repository holds the domain modules only for now. The draft plan,
`docs/homa-usecase-plan.md`, covers the conventions, each context's usecases, how one context gets
another's data, the required rules R1 to R5, and open questions U1 to U7. It waits for review.

### Decisions taken while building

All of them are confirmed by the product owner, and recorded in the domain plan under "Decisions made
while building" (`docs/homa-domain-plan.md`). The plan's method blocks and object tables match the code.
The re-verification decisions are listed under "Change after the domain phase: periodic
re-verification" in the plan, and are confirmed too.
