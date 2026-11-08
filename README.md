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
  availability/domain/          a provider's windows, synced busy blocks, reservation holds and buffer
  reservation/domain/           enquiries and the reservation lifecycle, with every time and money rule
  payment/domain/               payments, fines, payouts and fine deductions
  resolution/domain/            the founders' case queue and decisions
  conversation/domain/          chats for enquiries and reservations
  discovery/domain/             search criteria and exact, close or no matches
  ie/port/                      one integration event interface per publishing context
  ie/events/                    the integration event records
src/test/java/                  the same packages, one test class per feature
docs/                           the product description, the domain plan and the module discipline
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
| 6 | `reservation`, all seven slices | Done |
| 7 | `payment` | Done |
| 8 | `resolution` | Done |
| 9 | `conversation` | Done |
| 10 | `discovery` | Done |
| 11 | `ie/` integration event contracts | Done |

The domain phase is complete. Next by the discipline: the usecase layer.

### Decisions taken while building (to fold back into the plan)

Confirmed by the product owner: customers have a name; `HomaLimits` for hours and helpers; a live
listing whose verified team becomes empty is no longer live; every decision listed under the
reservation and payment details below; touching
windows, touching blocks and the later-start tie-break in availability, plus a buffer between jobs;
`Helper.remove()`.

Open questions answered: #1 the customer no-show wait starts from the later of the booked start and
check-in; #2 percentages round half-up and the refund is computed last; #3 editing a request does not
reset its expiry; #6 the lead is one of the named helpers, and a provider who is their own helper can
be the lead; #8 a request keeps the offering snapshot it was made with; #4 near the chosen time means within 2
hours either side.

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
  - A provider can set a buffer between jobs. A new slot can't start within the buffer after a held
    slot, or end within it before one. The buffer is kept around reservation holds only, not around
    synced busy blocks, and changing it doesn't affect slots already held.
  - `nearestAvailableStart` searches in 15-minute steps outward from the wanted start, up to the given
    distance either side, and takes the later start when two are equally near.
- **Reservation details:**
  - `reservation` defines its own `OfferingSnapshot` and `PolicySnapshot`, with only what it needs.
  - The payment method is a separate argument to `Reservation.request`, not part of
    `ReservationDetails`, because an enquiry has the same details but no payment.
  - The provider's late cancellation fine applies inside the policy's 24-hour fee window.
  - Times at the edges:
    - A request is expired at its expiry time: accepting at Friday 09:00 is rejected.
    - The check-in period includes its last moment (10:15), so a provider no-show is marked only
      after it.
    - The no-show report window and the dispute window exclude their end, the moment the money is
      released.
  - Customer cancellation:
    - Cancelling a reserved booking after the check-in period with no check-in is rejected. The
      reservation ends as a provider no-show instead.
    - Cancelling after an early check-in, even before the booked start, counts as a customer no-show.
  - After completion, the customer can't cancel, the provider can't cancel, and no customer no-show
    can be reported.
  - Reports and decisions:
    - Either party can still file their one report while a dispute from a report or a missed
      completion is open. A challenged no-show takes no reports.
    - For a challenged no-show, "release" and "partial refund" are measured against the no-show fee.
    - `Reservation.resolve` takes `now`, so the settlement has a release time.
  - **Payment phase note:** a customer no-show settlement is released when the report window ends.
    If it is challenged, Homa's decision gives a final settlement that replaces it.
  - The team reminder is a new Homa-set value, 24 hours (confirmed). `isTeamReminderDue` is true while
    the booking is reserved and the team is not named, from 24 hours before the booked start until the
    start, when the team locks.
- **Payment details:**
  - `capture` runs on acceptance for both methods. A card is charged; an FPX payment, already held, is
    kept for the booking.
  - Edits and endings before acceptance are only allowed while the card is authorised or the FPX payment
    is held. An FPX edit collects an increase or refunds a decrease; a card edit just moves the
    authorisation.
  - A card request that ends before acceptance moves to `AUTHORISATION_RELEASED`, a fifth state beside
    the spec's four, since nothing was charged.
  - `settle` takes `now`. Refunds go out at `now`; the provider's earning and Homa's fee are released
    from the settlement's release time.
  - A payment is settled exactly once, and the settlement must add up to its total. **Usecase note:**
    send a customer no-show settlement to `payment` only when the report window ends unchallenged. If
    it is challenged, Homa's decision is the only settlement sent.
  - Fines (open question #7): a payout records its `PayoutKind`, and fines come off payouts for
    completed jobs only. They are taken oldest first, up to the cap, which rounds in the provider's
    favour.
  - A fine is waived once, with a reason. A fully deducted fine has nothing left to waive.
- **Resolution details:** a case is opened with the amount at stake: the provider price for a job, or
  the customer no-show fee for a challenged no-show. A partial refund must be within that amount.
  Evidence can be added while the case is open, for example when the other party also reports.
- **Conversation details:**
  - `Conversation.start(Party, ...)` lets the rule "only the customer starts a chat" be enforced;
    `startByCustomer` is the usual entry point.
  - A chat is linked to a reservation once.
  - Only an enquiry's chat is archived, once. A reservation's chat stays open.
- **Discovery details:**
  - `SearchCriteria.of` takes `SearchLimits`, discovery's own view of the Homa-set values.
  - A `ProviderCandidate` carries `nearestFreeStart`, the answer from availability's
    `nearestAvailableStart` within 2 hours either side (open question #4, confirmed). Equal to the chosen start means free at the chosen time; another start means a
    close match; empty means no match.
  - A match result lists every reason it is not exact.
- **Integration events (`ie/`):**
  - Seven publishing contexts each have an event interface in `ie/port`, and the 36 events in the
    plan's table are records in `ie/events`. Availability, conversation and discovery only listen.
  - Every event carries `occurredAt`, which the usecase supplies.
  - The rule (confirmed, and the same in `docs/`): events carry primitives, Strings, kernel types and
    JDK value types (`Instant`, `Duration`, immutable `List`/`Set`); never domain objects; no `Optional`
    fields. Enums go across as their names. Collections are copied in each record's
    compact constructor. A contract test checks every field's type.
  - Rather than nullable fields, events are split so every value is always present:
    - Listing events carry `travelIncluded` and a `travelFee` that is RM0 when travel is included.
    - `ListingChangedIE` is for live listings only. `ListingWentOfflineIE` covers a live listing that
      stops being live.
    - `EnquiryRequestedIE` (from `Enquiry.markRequested`) links the chat, instead of an optional
      enquiry id on `ReservationRequestedIE`.
    - `CaseDecidedIE.providerReceives` is always set, from `Case.providerReceives`.
  - `HomaPolicyChangedIE` carries every Homa-set value, so listeners keep a full copy. The listing
    events carry the whole listing that search needs. `ReportFiledIE`, `NoShowChallengedIE` and
    `CompletionDeadlineMissedIE` carry the amount at stake for the case.
  - One event per outcome (confirmed): `ProviderVerifiedIE` and `HelperVerifiedIE` mean verified,
    nothing else. A rejection has its own event, `ProviderVerificationRejectedIE` or
    `HelperVerificationRejectedIE`, so the provider is told to redo eKYC.
  - The tests check that each event can't be built with a required field missing, can't be changed
    after it is built, and belongs to its context's interface. ArchUnit checks that `ie` imports only
    `kernel` and Java and that no domain imports `ie`. The mutation gate doesn't apply to `ie`.
- **Ids are Strings.** Following `XId().generate()` returning a `String`, signatures the plan writes
  with an id type, such as `Helper.add(ProviderId provider, ...)`, take a `String` id.
- **Jobs and tools have no `equals`.** They are only reached through their list, keyed by code.
