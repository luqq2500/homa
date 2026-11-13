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
  catalogue/domain/             job list, tool list, service area, homepage showcase
  policy/domain/                Homa-set values and their versions
  account/domain/               customer and business provider accounts, team members, and the results reported by the KYC provider
  offering/domain/              a provider's listing and the rules for going live
  availability/domain/          a provider's windows, synced busy blocks, reservation holds and buffer
  reservation/domain/           enquiries and the reservation lifecycle, with every time and money rule
  payment/domain/               payments, fines, payouts and fine deductions
  resolution/domain/            the admins' case queue and decisions
  conversation/domain/          chats for enquiries and reservations
  discovery/domain/             search criteria and exact, close or no matches
  feedback/domain/              customers' ratings of providers and helpers, and the averages shown
  ie/port/                      one integration event interface per publishing context
  ie/events/                    the integration event records
src/test/java/                  the same packages, one test class per feature
docs/                           the product description, the domain stories, the plans, the module discipline, the domain audit and the domain closing report
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
mvn verify                          # unit tests, dependency rule (ArchUnit), mutation >= 80% overall (PIT)
python3 tools/quality_gates.py      # CRAP < 6 per method; mutation >= 90% for kernel and each domain, >= 80% for each usecase; ratchet
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
| 15 | Verified for the whole job: a provider is requested, accepts and is found in search only for jobs that end by their re-verification due date | Done |
| 16 | Provider types, team roles (helpers and supervisors), accepted documents, face check-in, and adding a team member through a lawful-employment confirmation and eKYC | Done |
| 17 | Verification on the spot: eKYC on the provider's phone with the person present, with no time limit | Done |
| 18 | Business verification through eKYB, including the owner's or director's eKYC; a business starts with no team members | Done |
| 19 | Business providers only; verification, re-verification and face checks handled by a KYC provider, with Homa keeping the marketplace rules | Done |
| 20 | Ongoing monitoring and annual re-verification: "re-verification needed by a date", serious flags, and a flagged provider's reservations disputed and cancelled with a full refund | Done |
| 21 | A flagged provider: admins cancel or restore each disputed reservation, undecided ones cancel at the booked start, and payouts are held for review | Done |
| 22 | Getting ready for the usecase phase: a retired job or tool comes off the listings, a request is paid before the provider can accept it and ends when its payment fails, and five events (`ServiceAreaPublishedIE`, `RequestCancelledIE`, `PaymentFailedIE`, `PaymentSecuredIE`, `SearchPerformedIE`) | Done |

The domain phase is complete: see `docs/domain-closing-report.md`, and its addenda for `feedback`, team membership, re-verification, verified for the whole job, provider types and face check-in, and verification on the spot.

### Usecase phase

Not started. The usecase plan is still to be written, following the backend module discipline and the
domain plan's conventions. An earlier start was removed so the phase begins fresh from the domain as it
now stands; it is in git history up to `170c533`.

### Decisions taken while building

All of them are confirmed by the product owner, and recorded in the domain plan under "Decisions made
while building" (`docs/homa-domain-plan.md`). The plan's method blocks and object tables match the code.
The re-verification decisions are listed under "Change after the domain phase: periodic
re-verification" in the plan, and are confirmed too.
The decisions for provider types, team roles, documents, face check-in and adding a team member are
listed under "Decided while building: provider types, ..." in the plan, and are confirmed too.
