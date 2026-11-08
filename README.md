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
  account/domain/               customer and provider accounts, helpers, phone and eKYC verification
  offering/domain/              a provider's listing and the rules for going live
  availability/domain/          a provider's windows, synced busy blocks and reservation holds
src/test/java/                  the same packages, one test class per feature
tools/quality_gates.py          CRAP gate, per-context mutation gate and ratchet
quality/ratchet.json            last passing mutation score per context
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
python3 tools/quality_gates.py      # CRAP < 6 per method, mutation >= 90% per context, ratchet
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
| 6 | `reservation`, slice by slice | Next |
| 7–10 | `payment`, `resolution`, `conversation`, `discovery` | To do |
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
- **Account details:**
  - Customers register with a name as well as a phone number and address, because the spec says
    providers see "the customer's name, address and reservation details".
  - Phone numbers are Malaysian mobiles (`+601` then 8 or 9 digits).
  - The SMS code (open question 5) is 6 digits and expires 5 minutes after it is sent, exactly at the
    5-minute mark. After 5 wrong codes, a new code is needed.
  - An eKYC result is `VERIFIED` or `REJECTED`; a rejected provider can be verified on a later attempt.
  - A provider is their own helper by default.
  - `Helper.remove()` is added, so `HelperRemovedIE` has a domain method behind it.
- **Offering details:**
  - `setHours` and `setHelpers` both take `HomaLimits`, offering's own view of Homa's duration range and
    helpers per search, since the 10-helper cap is a Homa-set value.
  - A listing offers at least one job and covers at least one city. Bringing no tools is allowed.
  - A separate travel fee is above zero; charging none means travel is included.
  - When the team shrinks, max helpers shrinks to the team size, and min helpers with it if needed. A
    growing team doesn't widen the range. A live listing whose verified team becomes empty is no
    longer live.
  - Go-live lists every missing requirement, including the provider's own verification and the
    helpers' verification. Only a live listing gives an `OfferingSnapshot`, and a live listing doesn't
    go live twice.
- **Availability details:**
  - Windows that touch or overlap count as one stretch.
  - A busy block or hold that only touches a slot (ending at 10:00 for a 10:00 start) leaves it
    available.
  - A hold also needs the slot to be inside the windows.
  - Each reservation holds one slot, and releasing a reservation that holds nothing is an error.
  - `nearestAvailableStart` searches in 15-minute steps outward from the wanted start, up to the given
    distance either side, and takes the later start when two are equally near.
- **Ids are Strings.** Following `XId().generate()` returning a `String`, signatures the plan writes
  with an id type, such as `Helper.add(ProviderId provider, ...)`, take a `String` id.
- **Jobs and tools have no `equals`.** They are only reached through their list, keyed by code.
