# Homa Usecase Development Plan

This plan covers the **usecase phase** (Backend Module Discipline §8, step 2): the `usecase` layer of each bounded context, and the shared `ie/` integration event contracts that carry every cross-context call. Infra comes after; this plan only fixes the ports infra must satisfy and the rules it must keep.

**Status:** draft for the product owner's review. Nothing is built from it yet.
**Written:** 13 November 2026, on `claude/usecase-plan-domain-audit-prm4bv`.
**Sources, in order of authority:** `docs/homa-product-description.md` (the principal source of truth), `docs/backend_module_discipline.md`, `docs/homa-domain-plan.md`, `docs/domain-audit.md`, `docs/domain-closing-report.md`, `docs/homa-domain-stories.md`. When two of them disagree, the earlier one wins and the later one is corrected. Section 2 lists where that happened.

## Contents

1. What this plan decides, in one page
2. Is the domain ready? The readiness assessment
3. Decisions to confirm
4. Conventions
5. How a context gets another context's data
6. `ie/` in scope: rules, ledger corrections, new and changed events
7. Usecases per context
8. Required rules
9. Scheduled work
10. The admin console
11. Ports out and rules for the infra phase
12. Build order and definition of done
13. Testing approach and quality gates
14. Open questions for the product owner
15. Product coverage check

---

## 1. What this plan decides, in one page

- **The domain is ready to build on, with changes.** All 11 domain layers match the spec rule by rule (per the audit), but the usecase phase found **14 findings** (section 2). Seven need a small domain change (F1, F2, F3, F4, F6, F10, F11), four are changes to `ie/` or the plan's tables only (F5, F7, F8, F9), and three are a gap in the spec or contexts the domain plan never defined (F12 to F14: `notification`, `tracking`, `privacy`, and one measure that cannot be computed). None invalidates existing code or tests. Each is small, comes with its tests, and is ordered into **Step 0** or into the slice that needs it.
- **Contexts talk only through `ie/`.** An adapter in `app/` that lets reservation and discovery ask availability directly is rejected (Discipline §4, §5 rule 4). Availability instead publishes the provider's free time, and reservation and discovery keep their own projection of it (decision D1).
- **One usecase, one in port, one DTO pair.** Roughly **150 usecases** and **190 event handlers** (60 of them in `tracking`) across 14 contexts. Every event has one handler per consuming context.
- **Build in 16 slices** (section 12), in the order that makes "I booked help, and it turned up" work first: catalogue and policy, account, offering, availability, then reservation and payment together, then the rest of the lifecycle, resolution, feedback, and last notification, tracking and privacy.
- **Gates:** every `usecase` layer must pass unit tests, CRAP below 6, mutation at least 80% with the ratchet, and an ArchUnit dependency test; `ie/` keeps its contract tests (Discipline §9).
- **What I could not do in this session:** run the build. Maven cannot download its dependencies through this session's proxy, so the test counts and gate results quoted from the audit and closing report are theirs, not mine. Step 0 starts by re-running them.

---

## 2. Is the domain ready? The readiness assessment

### 2.1 Verdict

**Ready with changes.** I read the product description, the discipline, the domain plan, the audit, the closing report and the source of every context, then walked each product journey through the ports the domain offers. The 11 domain layers and `kernel/` hold every business rule the spec states, and the audit's 301 scenario-to-test mappings are credible. The gaps are all at the **seams**: a usecase needs something from another context, or needs a rule to be callable at a time the domain does not yet allow. Those seams only show once usecases are written, which is why they were missed.

| Layer | Verdict | Findings |
|---|---|---|
| `kernel` | Change (small) | F1 (a shared time range) |
| `catalogue`, `policy`, `feedback` | Ready | none |
| `account` | Change (small) | F2 |
| `offering` | Change (small) | F11 |
| `availability` | Change | F1 |
| `discovery` | Change | F1 |
| `reservation` | Change | F1, F3, F10 |
| `payment` | Change | F6 |
| `resolution` | Change | F4 |
| `conversation` | Ready (one event missing in `ie/`) | F5 |
| `ie/` | Change | F2, F4 to F9 and the listener table (F7) |
| `notification`, `tracking`, `privacy` | **Missing** | F13, F12 |

### 2.2 Findings

Each finding says what the spec needs, what the code does today (with where I looked), the change, and the slice it blocks. The product description decides every one of them.

| # | Finding | Evidence | Change (test first) | Blocks |
|---|---|---|---|---|
| **F1** | **Availability is invisible to reservation and discovery.** The spec makes availability part of requesting and accepting ("Homa checks the provider's availability when the request is made and again when it is accepted"; search shows "when a provider can actually work"). The domain plan decided that usecases check it (G3), but a usecase may not import `availability`. Discovery's `ProviderCandidate.nearestFreeStart` is computed by `ProviderCalendar.nearestAvailableStart`, and `Reservation.request` takes no availability at all | `ProviderCandidate.java`, `Reservation.request`, `ProviderCalendar`; domain plan "Decisions made while building", "Availability at request and at acceptance"; "aligning the product spec, domain plan and code", item 17 (taken time), which assumes "availability says is no longer free" | See D1. `kernel`: `TimeRange` (moved up from `availability`, which keeps its window, busy block and hold types). `availability`: `ProviderCalendar.freeTime(from, until)`, windows minus busy blocks minus holds widened by the buffer. `reservation`: `ProviderFreeTime` value (`isFreeFor`, `without`). `discovery`: `FreeTime` value (`isFreeFor`, `nearestStart`, with the same step, tie and bookable-start rules as `nearestAvailableStart`). `ie/`: `AvailabilityIE`, `ProviderFreeTimeChangedIE`, `HoldRefusedIE` | Slices 4, 5, 7 |
| **F2** | **The customer's address is not published.** A request copies the address, with its map pin, because check-in compares the device with it; a new address "is used for new searches and requests" | `CustomerRegisteredIE(customerId, name, city)`; no event for `CustomerAccount.changeAddress`, which also returns `void` where `rename` and `changeAccessNotes` return whether anything changed | `account`: `changeAddress` returns `boolean`. `ie/`: `CustomerRegisteredIE` gains street, latitude, longitude; new `CustomerAddressChangedIE` | Slice 5 |
| **F3** | **The customer no-show fee has no domain method that releases it.** "A customer no-show fee is released when the no-show report window ends without the customer reporting it." `reportCustomerNoShow` returns the settlement once; nothing recomputes it at the window's end, and the domain plan's usecase note says it must be sent only then | `Reservation.reportCustomerNoShow` (releaseAt = now + window); `closeDisputeWindow` exists for jobs but no sibling for no-shows; the audit lists "the end of the no-show report window" as a scheduler with no call to make | `reservation`: `closeNoShowReportWindow(now) : Settlement`, refused before the window ends, after a challenge, and a second time. With it every settlement releases at once, so `payment` never holds a future release | Slice 10 |
| **F4** | **The 72-hour decision target is nowhere in the domain.** "The customer and the provider see the exact date and time it is due"; the admin console "shows how long is left"; a measure is "share of cases decided within 72 hours". The 7-star table said "None: the queue already has each case's age", which gives an age, not a due time for both parties | `Case.java` (only `QUEUE_ORDER` by age); `ie/` has no case-opened event, so reservation and notification cannot show the due time | `resolution`: `Case.decisionDueAt()` = opened + 72 hours, and `Case.isOverdue(now)`. `ie/`: `CaseOpenedIE(caseId, reservationId, source, safety, decisionDueAt)` | Slice 11 |
| **F5** | **A direct request has no event to open its chat.** `Conversation.openForReservation` exists and is tested, but the ledger gives conversation only the enquiry events. `ReservationRequestedIE` goes to payment alone | Domain plan ledger, conversation rows; `Conversation.java` | `ie/`: `DirectRequestMadeIE(reservationId, customerId, providerId)`, published instead of nothing when a request has no enquiry. Conversation opens the chat on it. Not `ReservationRequestedIE` with an optional enquiry id, because events carry no `Optional` | Slice 8 |
| **F6** | **A payout has no executed state.** The spec has payouts per reservation, a provider "Income" view with payouts, and admin tracking of "failed ... payouts". `PayoutStatus` is only `PAYABLE`, `HELD`, `WITHHELD`, so nothing stops a payout being paid twice or lets a failed one be seen | `PayoutStatus.java`, `Payout.java` | `payment`: `PAID_OUT` and `FAILED` states, `Payout.markPaidOut(now)`, `markFailed(reason)`, `retry()`; a payout is paid out once. `ie/`: `PayoutPaidOutIE`, `PayoutFailedIE` | Slice 9 |
| **F7** | **The ledger's "listened to by" column is incomplete.** Usecases need listeners the table does not name | Section 6.3 lists every correction. The largest: `feedback` listens to nothing in the table, and `reservation` is not shown listening to the listing events it copies its `OfferingSnapshot` from | Correct the table in the domain plan (documentation only) | Slice 1 |
| **F8** | **Two events use an empty string as "not there".** `ReverificationRequiredIE` and `RecheckFailedIE` say "memberId is empty when it is the business's own". The event rule is to split, so reservation and discovery, which care only about the business, would have to inspect a sentinel | `ReverificationRequiredIE.java`, `RecheckFailedIE.java` | `ie/`: split into business and team-member events (section 6.2) | Slice 2 |
| **F9** | **Three events lack a value a consumer needs.** `TeamMemberAddedIE` has no photo reference, so reservation cannot show "who is coming"; `ProviderVerifiedIE` and `ProviderReverifiedIE` have the SSM number but not the registered business name that admin Providers and the Privacy policy name; `PayoutAccountReadyIE` has no payout account reference, so `payment` cannot pay anyone | The three event records; product description "Providers" and "What Homa collects" | `ie/`: add `photoRef`, `registeredBusinessName`, `payoutAccountRef` | Slices 2, 5, 9 |
| **F10** | **An enquiry has no closed state to announce once.** `EnquiryClosedIE` carries "`Enquiry.isClosed` turning true", but `isClosed(now)` is a pure query, so a scheduler would publish it every run | `Enquiry.java` | `reservation`: `Enquiry.close(now) : boolean`, true once, then false | Slice 5 |
| **F11** | **Offering would decide provider readiness in a usecase.** `ProviderListing.goLive(boolean providerReady)` is fed from four events (verified, lapsed, flagged, payout ready). The rule "ready = verified, not lapsed, not flagged, payout ready" lives in `account` today, and offering cannot call it | `ProviderAccount.isReadyForGoLive`, `ProviderListing.goLive`; the discipline says a usecase that starts deciding means the rule belongs in the domain | `offering`: a `ProviderReadiness` value built from those four facts, with `isReady()` | Slice 3 |
| **F12** | **Closing an account and answering data requests are not modelled.** The spec: "ask to close their account and delete their data"; admin "Data requests: handle ... and record each one"; "deleted or anonymised after the account is closed, except records Homa must keep by law". No context owns the rules, and the spec is silent on closing with open reservations or unpaid fines (question Q2) | No account-closing method anywhere; domain plan: "Privacy data requests: a workflow for the usecase layer" | New `privacy` context, domain first (decision D3) | Slice 15 |
| **F13** | **Three contexts the spec needs have no home.** Notifications (the audit's section 2b names the senders as missing), the admin tracking views and measures, and privacy requests. The ledger lists "(notifications)" and "(admin tracking)" as listeners without a context behind them | Domain plan "Not in the domain phase"; audit 2b | New `notification`, `tracking`, `privacy` contexts (D3). Each gets a short domain slice before its usecases | Slices 13 to 15 |
| **F14** | **One goal measure cannot be computed as designed.** "Newcomers book: share of first-time searchers who make a first reservation within 7 days" needs searches linked to a person, but the Privacy policy says searches are kept "with nothing that identifies the customer", and `SearchPerformedIE` carries nothing | Product description, Goals and Privacy; `SearchPerformedIE.java` | Product decision first (Q1); then `SearchPerformedIE` and the measure follow | Slice 14 |

Two more points are not gaps in the domain but belong on the list:

- **F15, tooling.** `tools/quality_gates.py` already gates the `usecase` layer at 80% and `pom.xml` says to add each layer's package to PIT's `targetClasses` and `targetTests`. Slice 1 does that for `*.usecase`, adds one ArchUnit test per context's usecase layer, and widens `IntegrationEventContractTest`'s allowed types for `TimeRange`.
- **F16, results are not ordered by the spec.** "Browse: see providers who match exactly, plus close matches" says nothing about the order within each group (question Q3).

### 2.3 What I checked and found in order

So the next reader does not repeat it: the money invariants and rounding (`Settlement.of`, refund computed last); every time boundary named in the spec (check-in 10:15, response deadline, fee window, rating window) against its test; the cancellation count and "jobs done" being derivable from events (`FineChargedIE`, `SettlementDecidedIE`, `ReservationCompletedIE`); the flag takeover of a job dispute; the Instant book paths; chargebacks reaching `payment` through `ChargebackDecidedIE`; and the 82 events against the 82 rows of the ledger (names match one to one).

### 2.4 Step 0: changes before the first slice

Done domain first, one commit each, each with its Gherkin scenarios and tests, in this order. Each stays inside the existing gates (mutation 90% for domain and `kernel`, ratchet at 100%).

| Step | Change | Findings |
|---|---|---|
| 0.1 | Re-run `mvn verify` and `tools/quality_gates.py` on a machine that can resolve dependencies; record the result in the audit | — |
| 0.2 | `kernel.TimeRange` and `TimeRanges` (interval algebra only: contains, overlaps, subtract, merge); `availability` uses them | F1 |
| 0.3 | `availability`: `ProviderCalendar.freeTime`; `reservation`: `ProviderFreeTime`; `discovery`: `FreeTime` | F1 |
| 0.4 | `account`: `changeAddress` returns whether it changed | F2 |
| 0.5 | `reservation`: `closeNoShowReportWindow`, `Enquiry.close` | F3, F10 |
| 0.6 | `resolution`: `Case.decisionDueAt`, `isOverdue` | F4 |
| 0.7 | `payment`: `Payout.markPaidOut`, `markFailed`, `retry` | F6 |
| 0.8 | `offering`: `ProviderReadiness` | F11 |
| 0.9 | `ie/`: every new and changed event in section 6.2, each with its contract test; extend the allowed field types by `TimeRange` | F1 to F9 |
| 0.10 | Docs: correct the domain plan's ledger (6.3), add the new scenarios to the plan and the audit, update the README | F7 |

Steps 0.2 to 0.5 can run in parallel with slices 1 and 2, because those do not use them. 0.6 to 0.8 must finish before slices 3, 9 and 11.

---

## 3. Decisions to confirm

These are choices I made so the plan could proceed. Each states what I recommend, what else was possible, and what changes if you decide otherwise. The first two shape most of the plan.

**D1. Availability reaches other contexts as an event-fed projection.**
- *Recommended.* Availability publishes `ProviderFreeTimeChangedIE`: the provider's free ranges from now to the booking horizon (windows minus busy blocks minus holds widened by the buffer), plus the buffer. It republishes on any calendar change, on a calendar sync, and daily. Reservation and discovery each keep a projection per provider. Reservation checks it at request and at acceptance, and on accepting a booking subtracts that booking and its buffer from its own projection **in the same transaction**, so two acceptances for one provider cannot both pass. It then declines the provider's other open requests that no longer fit (`declineForTakenTime`). Availability still holds the slot on `ReservationAcceptedIE`; if that hold is refused (a synced calendar event arrived in between), the booking **stands**, because the customer is already booked, and the provider is told through `HoldRefusedIE`.
- *Rejected: an adapter in `app/` that calls availability.* It breaks Discipline §4 and §5 rule 4: the only shared surface between contexts is `ie/`.
- *Rejected: a synchronous handler that vetoes the acceptance.* It works in one deployable but cannot survive the move to a broker (§10, Deployment Granularity).
- *If declined:* reservation and discovery cannot check availability at all, and G3 cannot be met.

**D2. Pages that mix contexts are composed at the edge, in `app/`, read-only.**
- *Recommended.* The provider page (profile, team with photos and stars, ratings, stats), the search result cards, the inbox with provider names, and the admin case file each need data from several contexts. A small composition layer in `app/` calls each context's **query ports** and merges the DTOs. It holds no rules and no state. Anything that **decides** something (request, accept, search matching) uses an event-fed projection, never a composed read.
- *Alternative.* Projections for every page in `discovery`, which needs profile names, team photos and rating summaries on more events.
- *If declined:* add `RatingSummaryChangedIE` and profile fields to the listing events (about four more event changes), and discovery grows a provider-page projection.

**D3. Three new bounded contexts, each with a real domain.**
- *Recommended.* `notification` (what is sent, to whom, in which language, once), `tracking` (the goals and measures, plus the admin views), `privacy` (data requests and account closing). Making `tracking` and `privacy` contexts **without a domain** would be an exception to "exactly three layers" and "domain first" (Discipline §2, §8) that the discipline does not allow, and each has real rules, so each gets a short domain slice first.
- *If declined:* the notification matrix (section 7.12) is still needed, and would be split across the existing contexts' handlers.

**D4. Events leave a context through a transactional outbox.**
- Discipline §10 allows "synchronous within the transaction" or "a durable outbox". I choose the outbox, with an in-process dispatcher at launch, so moving to a broker later changes only `infra/event`. The outbox row's id is the message id used for de-duplication, so `ie/` records need no event id field and stay free of behaviour.

**D5. One in-port call is one transaction, applied from outside the usecase.**
- A usecase may not import Spring. The transaction around each `usecase.port.in` call is applied by an aspect in `infra`, wired in `app/`, with no `@Transactional` in the usecase layer.

**D6. The 72-hour decision target is a constant of `resolution`, not a 27th Homa-set value.**
- The spec lists it in the Dispute Policy, not in the Homa-set Values table. See Q6.

**D7. Actors.** `Guest` (anyone, with or without an account), `Customer`, `Provider`, `Admin` (the admin console; the spec says admins), `Homa` (the platform's scheduled steps), and the external services that report back: `Kyc`, `Stripe`, `Calendar`. The stories use "Homa" for the platform and "Admins" for people, and this plan follows them.

**D8. Reminders are published by reservation.**
- *Recommended.* The rules "6 hours before the start while the team is not named" and "30 minutes after the booked end" already live in `Reservation.isTeamReminderDue` and `isCompletionReminderDue`. A scheduled `HomaRemindProviders` in reservation calls them and publishes `TeamReminderDueIE` and `CompletionReminderDueIE`; notification only sends them. This adds two events that rule 6.1(3) would not otherwise allow, because notification is their only consumer.
- *Alternative.* Notification recomputes the due moments from `ReservationRequestedIE`, `TeamNamedIE` and the policy, which copies the rule into a second context.

---

## 4. Conventions

### Layout (Discipline §3)

```text
<context>/usecase/
├── port/in/        one interface per usecase, and one per integration event handler
├── port/out/       repositories, the context's single IE publisher, projections, outbound clients
├── dto/            a request and a response record per usecase, never shared
├── exception/      orchestration failures, such as a record not found
└── <Impl>.java     usecase and handler implementations
```

### Naming (Discipline §6)

The implementation is named for the actor's action, and the interface for the actor of the action:

| Implementation | Interface | Method |
|---|---|---|
| `CustomerRequestReservation` | `CustomerReservationRequester` | `request(RequestReservationRequest) : RequestReservationResponse` |
| `ProviderAcceptRequest` | `ProviderRequestAccepter` | `accept(...)` |
| `AdminRetireJob` | `AdminJobRetirer` | `retire(...)` |
| `KycRecordBusinessResult` | `KycBusinessResultRecorder` | `record(...)` |
| `HandleListingChangedIE` | `ListingChangedIEHandler` | `handle(ListingChangedIE)` |

Request and response records are named `<Intent>Request` and `<Intent>Response`; a usecase with no input has a response only. Methods say their intent (`accept`, `retire`, `record`), never `execute` or `handle` for a command.

### The rules every usecase follows

1. **Rules stay in the domain.** A usecase loads, calls the domain, saves, publishes. If it starts deciding something, that rule moves to the domain (this is why F11 exists).
2. **Time:** usecases take a `java.time.Clock` and pass `Instant now` into the domain. Ids come from the domain's `XId().generate()`.
3. **Dependencies:** a usecase imports only its own context's `domain` and `usecase`, `kernel/`, `ie/` and the JDK. No Spring, no cloud SDK, no other context. An ArchUnit test per context checks it.
4. **One in port per implementation,** and nobody else implements it. One handler per event per consuming context; one `<Context>IEPublisher` per context.
5. **Events:** a usecase maps the domain's result to the `ie/` record and publishes it through the context's one publisher. Domain objects never leave the usecase: DTOs hold primitives, Strings, kernel and JDK value types, like events.
6. **Order of a change:** validate, call the domain, save, publish, record the audit entry. A refused change saves, publishes and records nothing. Save, outbox rows and audit entry commit together (D4, D5).
7. **Nothing changed, nothing done.** When the domain says a change changed nothing (`rename` to the same name, a repeated `UNCHANGED` result), the usecase saves, publishes and records nothing, and answers with things as they are.
8. **Errors:** domain exceptions pass through unchanged. Usecase exceptions cover orchestration only (`NotFound`, `ChangedMeanwhile`, `PhoneAlreadyRegistered`, `ProviderNotFree`).
9. **Admin actions:** every `Admin*` usecase that changes something takes `adminId` in its request, requires it (`AdminRequiredException`), and records the action through the context's own `AdminAuditLog` out port with the reason and time. A reason is required for a decision about a person, money or a provider's listing (`AdminReasonRequiredException`); plain curation (adding or renaming a job) records who, what and when only. `infra/security` sets `adminId` from the signed-in admin, never from the request body.
10. **Queries** are usecases too (their own port and DTO). They never publish, and never change state.
11. **Handlers are idempotent.** An event can arrive twice. Infra de-duplicates by outbox id; the handler also tolerates a repeat on its own (the domain's `UNCHANGED`, or "already held"). Every handler has a "delivered twice" test.
12. **Projections** are written only by handlers, behind a `port/out` interface named for what it keeps (`ProviderFreeTimeProjection`), and read by usecases through the same port.

---

## 5. How a context gets another context's data

Contexts keep their own copies of exactly what they need, fed by handlers. This table is the build list for every projection; the events are in the ledger (section 6).

| Consumer | Keeps | Fed by |
|---|---|---|
| offering | active jobs and tools | `JobAddedIE`, `JobRetiredIE`, `ToolAddedIE`, `ToolRetiredIE` |
| offering | Homa's hours limits | `HomaPolicyChangedIE` |
| offering | the cities Homa serves | `ServiceAreaPublishedIE` |
| offering | provider readiness (verified, lapsed, flagged, payout ready) | `ProviderVerifiedIE`, `ProviderVerificationRejectedIE`, `ProviderReverifiedIE`, `ProviderVerificationLapsedIE`, `ProviderFlaggedIE`, `PayoutAccountReadyIE` |
| offering | each provider's team, by role | `TeamMemberAddedIE`, `TeamMemberRoleChangedIE`, `TeamMemberReverifiedIE`, `TeamMemberLapsedIE`, `TeamMemberRemovedIE` |
| reservation | each live listing's offering, and whether it is paused | `ListingWentLiveIE`, `ListingChangedIE`, `ListingWentOfflineIE`, `ListingPausedIE`, `ListingResumedIE` |
| reservation, discovery | the policy in force | `HomaPolicyChangedIE` |
| reservation, discovery | each business's verified-until date | `ProviderVerifiedIE`, `ProviderReverifiedIE`, `BusinessReverificationRequiredIE` |
| reservation | each provider's free time | `ProviderFreeTimeChangedIE`, and its own acceptances (D1) |
| reservation | each customer's name, address and access notes | `CustomerRegisteredIE`, `CustomerRenamedIE`, `CustomerAddressChangedIE`, `CustomerAccessNotesChangedIE` |
| reservation | each team member's name, role and photo | `TeamMemberAddedIE`, `TeamMemberRoleChangedIE`, `TeamMemberRemovedIE`, `TeamMemberLapsedIE`, `TeamMemberReverifiedIE` |
| reservation | each case's due time | `CaseOpenedIE` |
| reservation, payment | whether a business is seriously flagged now | `ProviderFlaggedIE`, `ProviderReverifiedIE` |
| discovery | each live listing's search fields, and each provider's free time | the listing events, `ProviderFreeTimeChangedIE` |
| payment | who a reservation's provider and customer are | `ReservationRequestedIE`, `SettlementDecidedIE` |
| payment | each provider's payout account | `PayoutAccountReadyIE` |
| feedback | each reservation's customer, provider, named team and completion time | `ReservationRequestedIE`, `TeamNamedIE`, `TeamMemberDroppedIE`, `ReservationCompletedIE` |
| feedback | the rating window and minimum ratings to show | `HomaPolicyChangedIE` |
| conversation | the customer's name; an enquiry's customer and provider | `CustomerRegisteredIE`, `CustomerRenamedIE`, `EnquiryCreatedIE` |
| resolution | nothing beyond the events that open and close cases | `ReportFiledIE`, `NoShowChallengedIE`, `CompletionDeadlineMissedIE`, `ReservationFlaggedIE` |
| notification | recipients and their parties | `CustomerRegisteredIE`, `ReservationRequestedIE`, `ListingWentLiveIE` (for the provider id) and the matrix in 7.12 |
| tracking | its own views | the events in 7.13 |

Three limits on the table: a projection holds **facts the owner published**, never a rule computed from them (the one exception was F11, now fixed in the domain); a decision never reads a D2 composed page; and the free-time projection is the only one reservation updates itself.

---

## 6. `ie/` in scope

### 6.1 Rules (unchanged, plus three)

The existing rules stand: events carry primitives, Strings, kernel types and JDK value types, never domain objects, no `Optional`; collections are copied in the compact constructor; one event per outcome; one interface per publishing context; `ie/` imports only `kernel/`; no domain imports `ie/`. Added by this plan:

1. **Every event has a publishing usecase and at least one handler or a named non-context reader.** The contract test already proves each record can only be built whole. A new check, `EveryEventHasAPublisherAndAConsumer`, reads a table kept in the usecase plan's test-support and fails when an event has neither; "(notifications)" and "(tracking)" count once those contexts exist.
2. **A new event needs a new domain method behind it** (the plan's rule "an event with no domain method means a method is missing") or a stated reason (section 6.2 gives one for each).
3. **A field is added to an event only if a consumer in this plan reads it.** Nothing is speculative.

### 6.2 New and changed events

The ledger today has **82** events and 9 event interfaces. Step 0 adds **11** events and **1** interface (`AvailabilityIE`), changes **6** records, and replaces **2** events with 4 (the split in F8): **91** events afterwards. D8 would add 2 more (93).

**New events (11) and the new interface**

| Event | Publisher | Carries | Consumers | Behind | Finding |
|---|---|---|---|---|---|
| `AvailabilityIE` (interface) | availability | `occurredAt` | — | availability becomes a publishing context | F1 |
| `ProviderFreeTimeChangedIE` | availability | `providerId`, `free : List<TimeRange>`, `buffer : Duration`, `until : Instant` | reservation, discovery | `ProviderCalendar.freeTime` | F1 |
| `HoldRefusedIE` | availability | `reservationId`, `providerId` | notification, tracking | a hold refused with `SlotNotAvailableException` | F1 |
| `CustomerAddressChangedIE` | account | `customerId`, `street`, `city`, `latitude`, `longitude` | reservation | `CustomerAccount.changeAddress` returning `true` | F2 |
| `DirectRequestMadeIE` | reservation | `reservationId`, `customerId`, `providerId` | conversation | `Reservation.request` with no enquiry | F5 |
| `CaseOpenedIE` | resolution | `caseId`, `reservationId`, `source`, `safety`, `decisionDueAt` | reservation, notification, tracking | `Case.open`, `Case.decisionDueAt` | F4 |
| `PayoutPaidOutIE` | payment | `payoutId`, `providerId`, `net` | notification, tracking | `Payout.markPaidOut` | F6 |
| `PayoutFailedIE` | payment | `payoutId`, `providerId`, `reason` | notification, tracking | `Payout.markFailed` | F6 |
| `BusinessReverificationRequiredIE` | account | `providerId`, `reason`, `verifiedUntil` | reservation, discovery, notification | `ProviderAccount.requireReverification` | F8 |
| `MemberReverificationRequiredIE` | account | `providerId`, `memberId`, `reason`, `verifiedUntil` | notification | `TeamMember.requireReverification` | F8 |
| `BusinessRecheckFailedIE` | account | `providerId` | notification | `ProviderAccount.recordKybResult` returning `RECHECK_FAILED` | F8 |
| `MemberRecheckFailedIE` | account | `providerId`, `memberId` | notification | `TeamMember.recordKycResult` returning `RECHECK_FAILED` | F8 |

(That is 11 events: the four in the last four rows are the split of F8.)

**Removed (2):** `ReverificationRequiredIE` and `RecheckFailedIE`, replaced by the four split events above.

**Changed (6)**: a field added to an existing record. The contract test's builder for each changes with it.

| Event | Change | Why | Finding |
|---|---|---|---|
| `CustomerRegisteredIE` | add `street`, `latitude`, `longitude` | reservation needs the pinned address | F2 |
| `TeamMemberAddedIE` | add `photoRef` | "who is coming" shows the photo | F9 |
| `ProviderVerifiedIE`, `ProviderReverifiedIE` | add `registeredBusinessName` | admin Providers view; the Privacy policy names it | F9 |
| `PayoutAccountReadyIE` | add `payoutAccountRef` | `payment` pays the connected account | F9 |
| `CaseDecidedIE` | add `decidedWithinTarget : boolean` | the "decided within 72 hours" measure, answered by the domain and not recomputed by tracking | F4 |

`SearchPerformedIE` changes only if Q1 is answered with a pseudonymous search id. `ListingWentLiveIE` and `ListingChangedIE` do not change (D2).

**Proposed, outside Step 0, each needing an answer:** `TeamReminderDueIE` and `CompletionReminderDueIE` (D8); `MessagePostedIE` (Q4); `DataErasureRequestedIE` and the `PrivacyIE` interface (slice 15).

### 6.3 Corrections to the domain plan's "listened to by" column (F7)

These are not new events; the plan's table does not name a listener the usecases need.

| Event | Add listener | Because |
|---|---|---|
| `ListingWentLiveIE`, `ListingChangedIE`, `ListingWentOfflineIE` | reservation | copies the live listing's offering for requests, and whether it is paused (R2) |
| `TeamMemberAddedIE`, `TeamMemberLapsedIE`, `TeamMemberRemovedIE`, `TeamMemberReverifiedIE` | reservation | keeps the team directory, and drops a lapsed or removed person from jobs (R5) |
| `CustomerRegisteredIE`, `CustomerRenamedIE` | reservation | providers see the customer's name on the reservation |
| `ReservationRequestedIE`, `TeamNamedIE`, `TeamMemberDroppedIE`, `ReservationCompletedIE`, `HomaPolicyChangedIE` | feedback | feedback listens to nothing today; it needs the reservation's parties, named team, completion time, rating window and minimum ratings |
| `ProviderReverifiedIE` | payment | new payouts stop being held after the provider passes again (R10) |
| `ReservationFlaggedIE` | resolution | a case taken over by the flag is closed |
| `ReservationRequestedIE`, `SettlementDecidedIE` | payment | which provider and customer a reservation belongs to |
| `JobRenamedIE`, `JobRegroupedIE`, `ToolRenamedIE` | remove discovery | names are composed at the edge (D2) |
| every event marked "(notifications)" | notification | the matrix in 7.12 |
| every event marked "(admin tracking)", "(admin)", "(provider page)" | tracking | 7.13; the provider page is composed (D2) |

### 6.4 The contract test, and what each slice must do to `ie/`

- `IntegrationEventContractTest` keeps its three checks (allowed field types, copied collections, nulls refused) and gains `TimeRange` in the allowed types. Its builders are the only place that knows each event's required fields.
- `IntegrationEventArchitectureTest` keeps "`ie` imports only `kernel` and Java" and "no domain imports `ie`", and gains "no context's **usecase** imports another context's usecase or domain" (the ArchUnit rule in 13.2).
- A slice that publishes an event adds the publisher method and a usecase test that asserts the exact record published. A slice that consumes an event adds the handler and a test that feeds it the record **built with the contract test's builder**, so a change to an event breaks every consumer's test at once.

---

## 7. Usecases per context

Notation: *impl* names follow section 4; **→** lists the events published; the domain calls are the domain plan's method names. "Q" means a query (no events, no state change). Every usecase that is not a query saves with an optimistic version check (rule I1, section 11).

### 7.1 `catalogue` (publishes `CatalogueIE`)

| Usecase | Does | → |
|---|---|---|
| `AdminAddJob`, `AdminAddTool` | `JobList.add`, `ToolList.add` | `JobAddedIE`, `ToolAddedIE` |
| `AdminRenameJob`, `AdminRenameTool` | `rename`; nothing published if unchanged | `JobRenamedIE`, `ToolRenamedIE` |
| `AdminRegroupJob` | `regroup`; nothing if unchanged | `JobRegroupedIE` |
| `AdminRetireJob` | needs a reason; `JobList.retire` **and** `Showcase.withdrawJob` in one transaction | `JobRetiredIE` |
| `AdminRetireTool` | needs a reason; `ToolList.retire` | `ToolRetiredIE` |
| `AdminFeatureSearch`, `AdminRenameFeaturedSearch`, `AdminMoveFeaturedSearch`, `AdminRemoveFeaturedSearch` | the `Showcase` methods; the offered jobs come from the job list | none |
| `AdminViewShowcase` (Q) | every featured search, with whether it is shown | — |
| `GuestViewShowcase` (Q) | shown searches only, in order | — |
| `GuestViewJobList`, `GuestViewToolList`, `GuestViewServiceArea` (Q) | active jobs by group, active tools, the cities | — |
| `AdminViewJobList`, `AdminViewToolList` (Q) | including retired | — |
| `HomaLaunchServiceArea` | at startup, once: `ServiceArea.launch` | `ServiceAreaPublishedIE` |

Handlers: none. Out ports: `JobListRepository`, `ToolListRepository`, `ServiceAreaRepository`, `ShowcaseRepository`, `CatalogueIEPublisher`, `AdminAuditLog`. Rules: R11 (a retirement needs a reason; renames and regroups keep codes).

### 7.2 `policy` (publishes `PolicyIE`)

| Usecase | Does | → |
|---|---|---|
| `HomaLaunchPolicy` | at startup, once: `HomaPolicy.initial(now)` | `HomaPolicyChangedIE` (version 1) |
| `AdminChangePolicyValue` | one value at a time, with a reason: `HomaPolicy.change(value, now)`; refused unless the version is exactly one more than the version in force | `HomaPolicyChangedIE` (full copy of all 26 values) |
| `AdminViewPolicy`, `AdminViewPolicyHistory` (Q) | the current values; every version | — |

Handlers: none. The request names the value and carries the number or duration as primitives; the usecase builds the `PolicyValue`. Out ports: `HomaPolicyRepository`, `PolicyIEPublisher`, `AdminAuditLog`.

### 7.3 `account` (publishes `AccountIE`)

**Phone and customer**

| Usecase | Does | → |
|---|---|---|
| `GuestStartPhoneVerification` | `PhoneVerification.start`; sends the 6-digit code through `SmsSender` | none |
| `GuestConfirmPhone` | `PhoneVerification.confirm`; wrong code, expiry and five attempts are the domain's | none |
| `CustomerRegisterAccount` | needs a confirmed phone, a name and a pinned address; one customer account per phone (`PhoneAlreadyRegistered`) | `CustomerRegisteredIE` |
| `CustomerRename` | `rename`; nothing if unchanged | `CustomerRenamedIE` |
| `CustomerChangeAddress` | `changeAddress`; nothing if unchanged (F2) | `CustomerAddressChangedIE` |
| `CustomerChangeAccessNotes` | `changeAccessNotes` | `CustomerAccessNotesChangedIE` |
| `CustomerViewAccount` (Q) | name, phone, address, notes | — |

**Business provider**

| Usecase | Does | → |
|---|---|---|
| `ProviderRegisterBusiness` | `ProviderAccount.register(verified phone)`; one business account per phone | none |
| `ProviderStartKyb` | asks the KYC provider (`IdentityVerificationService`) for a hosted KYB session and returns its address | none |
| `ProviderViewVerification` (Q) | KYB status, verified-until date, last verified, each member's due date | — |
| `ProviderStartBusinessReverification` | the owner re-verifies on the spot: a hosted session | none |
| `ProviderSetUpPayoutAccount` | starts Stripe Connect onboarding; returns the link | none |
| `StripeRecordPayoutAccountReady` | webhook; `recordPayoutAccountReady()`; nothing if it was already ready | `PayoutAccountReadyIE` (with `payoutAccountRef`) |

**Team**

| Usecase | Does | → |
|---|---|---|
| `ProviderStartAddingTeamMember` | role and a live photo (stored through `PhotoStorage`); only for a verified, not lapsed business; starts the member's KYC session on the provider's phone | `MemberVerificationStartedIE` |
| `ProviderConfirmTeamMember` | the lawful-employment confirmation for the **named** person, with the terms version and time; then `TeamMember.add` | `TeamMemberAddedIE` |
| `ProviderChangeTeamMemberRole` | `changeRole`; no new check; nothing if unchanged | `TeamMemberRoleChangedIE` |
| `ProviderRemoveTeamMember` | `remove` | `TeamMemberRemovedIE` |
| `ProviderStartMemberReverification` | a hosted session, person present | none |
| `ProviderViewTeam` (Q) | the team, waiting for verification, waiting for confirmation | — |

**The KYC provider reporting back** (one usecase per report; each checks the webhook signature and skips a repeat, rule R9)

| Usecase | Domain call | → by outcome |
|---|---|---|
| `KycRecordBusinessResult` | `ProviderAccount.recordKybResult` | `VERIFIED` → `ProviderVerifiedIE`; `REJECTED` → `ProviderVerificationRejectedIE`; `REVERIFIED` → `ProviderReverifiedIE`; `RECHECK_FAILED` → `BusinessRecheckFailedIE`; `UNCHANGED` → nothing |
| `KycRecordBusinessLapse` | `lapse` | `ProviderVerificationLapsedIE` |
| `KycRecordBusinessReverificationRequired` | `requireReverification` | `BusinessReverificationRequiredIE` |
| `KycRecordBusinessFlag` | `recordSeriousFlag` | `ProviderFlaggedIE` |
| `KycRecordMemberVerificationResult` | `MemberVerification.recordKycResult(result, photoMatch)` | `VERIFIED` → `MemberVerifiedIE` (waits for the provider's confirmation); `REJECTED` → `MemberVerificationRejectedIE` with the reason |
| `KycRecordMemberResult` | `TeamMember.recordKycResult` | `REVERIFIED` → `TeamMemberReverifiedIE`; `RECHECK_FAILED` → `MemberRecheckFailedIE` |
| `KycRecordMemberLapse` | `lapse` (a missed re-verification or a serious flag) | `TeamMemberLapsedIE` |
| `KycRecordMemberReverificationRequired` | `requireReverification` | `MemberReverificationRequiredIE` |

**Admin**

| Usecase | Does | → |
|---|---|---|
| `AdminRemoveTeamMember` | with a reason: Homa removes a person it learns is not allowed to work, and records what happened | `TeamMemberRemovedIE` |
| `AdminRequestBusinessRecheck`, `AdminRequestMemberRecheck` | with a reason: asks the KYC provider for an early re-check (to be passed within 14 days) | none |
| `AdminViewProviderAccount`, `AdminViewCustomerAccount` (Q) | KYB result, confirmations with time and terms version, verification status, face check failures come from reservation | — |

Handlers: none. Out ports: `CustomerAccountRepository`, `ProviderAccountRepository`, `MemberVerificationRepository`, `TeamMemberRepository`, `PhoneVerificationRepository`, `SmsSender`, `IdentityVerificationService`, `PayoutAccountOnboarding`, `PhotoStorage`, `AccountIEPublisher`, `AdminAuditLog`.

### 7.4 `offering` (publishes `OfferingIE`)

A provider's listing is created on first use (`ListingRepository.findOrDraft`), because setup steps 4 and 5 happen "while KYB is being checked", before any account event.

| Usecase | Does | → |
|---|---|---|
| `ProviderSetProfile`, `ProviderOfferJobs`, `ProviderBringTools`, `ProviderSetPricing`, `ProviderSetHours`, `ProviderSetHelpers`, `ProviderSetSupervisorRule`, `ProviderClearSupervisorRule`, `ProviderCoverCities`, `ProviderChooseCancellationTier`, `ProviderChooseAcceptancePolicy` | the matching `ProviderListing` method with the projections it needs (active jobs and tools, the hours limits, the cities, the roster) | `ListingChangedIE` when the listing is live; nothing for a draft |
| `ProviderDeclareInsurance`, `ProviderClearInsurance` | `declareInsurance(now)`, `clearInsurance` | `ProviderInsuranceDeclaredIE`, `ProviderInsuranceClearedIE` (and `ListingChangedIE` if live) |
| `ProviderGoLive` | `goLive(readiness.isReady())`; lists everything missing when refused | `ListingWentLiveIE` |
| `ProviderViewListing` (Q) | the listing, its missing requirements, its insurance badge | — |

Handlers (17), each tested for delivery twice:

| Handler | Does | → |
|---|---|---|
| `HandleJobAddedIE`, `HandleJobRetiredIE`, `HandleToolAddedIE`, `HandleToolRetiredIE` | keep the active lists; a retirement calls `withdrawJob` or `withdrawTool` on **every** listing that has it (R11) | per listing: `ListingChangedIE` (still live), or `ListingWentOfflineIE` (left with no job), plus `JobWithdrawnFromListingIE`, `ListingWentOfflineForRetiredJobIE` or `ToolWithdrawnFromListingIE` |
| `HandleHomaPolicyChangedIE` | keeps the limits; `fitHours` on every listing | `ListingHoursNarrowedIE`, `ListingWentOfflineForHoursIE`, with `ListingChangedIE` or `ListingWentOfflineIE` |
| `HandleServiceAreaPublishedIE` | keeps the cities | none |
| `HandleProviderVerifiedIE`, `HandleProviderVerificationRejectedIE`, `HandlePayoutAccountReadyIE` | update `ProviderReadiness` | none |
| `HandleProviderReverifiedIE` | readiness; `resume` a paused listing | `ListingResumedIE` |
| `HandleProviderVerificationLapsedIE`, `HandleProviderFlaggedIE` | readiness; `pause` a live listing (R3: accepted reservations are untouched) | `ListingPausedIE` |
| `HandleTeamMemberAddedIE`, `HandleTeamMemberReverifiedIE`, `HandleTeamMemberLapsedIE`, `HandleTeamMemberRemovedIE`, `HandleTeamMemberRoleChangedIE` | rebuild the roster from members that `isTeamMember()` (R4); `updateRoster` | `ListingChangedIE`, or `ListingWentOfflineIE` when no helper, or no supervisor for a rule, is left |

Out ports: `ProviderListingRepository`, `CatalogueProjection`, `PolicyLimitsProjection`, `ServiceAreaProjection`, `ProviderReadinessProjection`, `TeamRosterProjection`, `OfferingIEPublisher`.

### 7.5 `availability` (publishes `AvailabilityIE`, new)

| Usecase | Does | → |
|---|---|---|
| `ProviderSetAvailability` | `setWindows` | `ProviderFreeTimeChangedIE` |
| `ProviderSetBufferBetweenJobs` | `setBufferBetweenJobs`; slots already held do not move | `ProviderFreeTimeChangedIE` |
| `ProviderConnectCalendar` | starts the Google or Apple sign-in through `CalendarService` | none |
| `CalendarRecordBusyBlocks` | a sync result: `replaceBusyBlocks`; nothing if the blocks are identical | `ProviderFreeTimeChangedIE` |
| `ProviderViewCalendar` (Q) | windows, busy blocks, holds, buffer | — |

Handlers: `HandleReservationAcceptedIE` (`hold(reservationId, slot)`; on `SlotNotAvailableException` the booking stands and `HoldRefusedIE` is published, D1), `HandleReservationCancelledIE` (`release`), `HandleReservationCompletedIE` (`release`; a missing hold is tolerated), `HandleHomaPolicyChangedIE` (keeps the booking horizon). Each hold or release also publishes `ProviderFreeTimeChangedIE`. Scheduled: `HomaSyncCalendars`, `HomaRepublishFreeTime` (section 9). Out ports: `ProviderCalendarRepository`, `CalendarService`, `PolicyHorizonProjection`, `AvailabilityIEPublisher`.

### 7.6 `discovery` (publishes `DiscoveryIE`)

| Usecase | Does | → |
|---|---|---|
| `GuestSearchProviders` | a guest picks a city or a pin (the pin becomes a city through `Geocoder`); builds `SearchCriteria.of` with the search limits; builds a `ProviderCandidate` from the listing, free-time and verified-until projections for each live, unpaused provider covering the city; `MatchClassifier.classify`; answers exact and close matches with the reason and the suggested start | `SearchPerformedIE` (jobs, start, hours, helpers, city, exact and close counts; nothing about the person, R13) |

Handlers (12): the five listing events (went live, changed, went offline, paused, resumed), the two hours events (`ListingHoursNarrowedIE`, `ListingWentOfflineForHoursIE`), `HandleHomaPolicyChangedIE`, `HandleProviderVerifiedIE`, `HandleProviderReverifiedIE`, `HandleBusinessReverificationRequiredIE` and `HandleProviderFreeTimeChangedIE`. Out ports: `ListingProjection`, `FreeTimeProjection`, `VerifiedUntilProjection`, `SearchLimitsProjection`, `Geocoder`, `DiscoveryIEPublisher`. Result order within each group: question Q3.

### 7.7 `reservation` (publishes `ReservationIE`)

The largest context. Four sub-groups, built in slices 5, 9, 10 and 11.

**Before requesting (slice 5)**

| Usecase | Does | → |
|---|---|---|
| `CustomerCreateEnquiry` | `Enquiry.create` with the account's current address | `EnquiryCreatedIE` |
| `CustomerBookAgainFromReservation`, `CustomerBookAgainFromEnquiry` | `Enquiry.bookAgain` with the past details, the current address and the new start | `EnquiryCreatedIE` |
| `CustomerPreviewReservation` (Q) | the review page: builds a request that is **not saved** to give the price breakdown, cancellation terms, supervisor inclusion and any refusal | — |
| `CustomerRequestReservation` | the offering, policy, verified-until date and free time from projections; `Reservation.request`; copies the account's access notes; if from an enquiry `markRequested` | `ReservationRequestedIE`, plus `EnquiryRequestedIE` from an enquiry, or `DirectRequestMadeIE` without one |
| `CustomerEditRequest` | `edit` (the address cannot change); checks free time again | `ReservationEditedIE` |
| `CustomerCancelReservation` | a request: `cancelByCustomer` only, free at any time before the provider answers; a reserved booking: `cancelByCustomer` returns the settlement | request → `RequestCancelledIE`; booking → `ReservationCancelledIE`, `SettlementDecidedIE` |
| `CustomerViewCancellationTerms` (Q) | `cancellationTerms()` | — |
| `CustomerChangeAccessNotes` | `changeAccessNotes` until the job is completed | `AccessNotesChangedIE` |
| `CustomerViewReservation`, `CustomerListReservations`, `CustomerViewEnquiry` (Q) | including who is coming, check-in time and each person's face check result | — |

**The provider answers (slice 5)**

| Usecase | Does | → |
|---|---|---|
| `ProviderViewRequests`, `ProviderViewReservation`, `ProviderListReservations` (Q) | requests only once their payment is secured (R14); the customer's name, address and notes from acceptance | — |
| `ProviderAcceptRequest` | refused while the listing is paused (R2); checks free time and the verified-until date; `accept`; subtracts the booking from the free-time projection; `declineForTakenTime` on each other open request that no longer fits (R15) | `ReservationAcceptedIE`; `RequestDeclinedForTakenTimeIE` per declined request |
| `ProviderDeclineRequest` | `decline` (refused for an Instant book request) | `ReservationDeclinedIE` |
| `ProviderCancelReservation` | `cancelByProvider` returns the settlement and any fine | `ReservationCancelledIE`, `SettlementDecidedIE`, `FineChargedIE` (if fined) |

**Job day (slice 10)**

| Usecase | Does | → |
|---|---|---|
| `ProviderNameTeam` | `nameTeam(team, roster, now)` with the roster from the team directory | `TeamNamedIE` |
| `ProviderCheckIn` | the device location only at this moment: the distance to the booked address from `DistanceCalculator`; `checkIn(distance, now)`; the location is not stored, only the result | `ProviderCheckedInIE` |
| `ProviderStartFaceChecks` | opens the KYC provider's face check for each named person | none |
| `KycRecordFaceCheck` | one final result per person: `recordFaceCheck`; the first pass completes check-in | `FaceCheckRecordedIE`; `CheckInCompletedIE` on the first pass |
| `KycReportFaceCheckUnavailable` | only after the KYC provider's own status says the service is down (R8, never the provider app's word); `recordFaceCheckUnavailable` | `CheckInCompletedIE` (marked unavailable) |
| `ProviderReportCustomerNoShow` | `reportCustomerNoShow`; the settlement is **not** sent (F3) | `ReservationCancelledIE` (ending customer no-show) |
| `CustomerChallengeNoShow` | `challengeNoShow` within the report window | `NoShowChallengedIE` |
| `ProviderSubmitCompletion` | evidence from `EvidenceStorage`; `submitCompletion` (also while disputed over the job) | `CompletionSubmittedIE` |

**Reports, decisions and flags (slice 11)**

| Usecase | Does | → |
|---|---|---|
| `CustomerFileReport`, `ProviderFileReport` | `fileReport(party, type, reason, evidence)`; a reason is required, evidence is optional | `ReportFiledIE` |
| `AdminCancelFlaggedReservation` | with a reason: `cancelForProviderFlag`, a full refund, no fine | `ReservationCancelledIE`, `SettlementDecidedIE` |
| `AdminRestoreFlaggedReservation` | with a reason; refused while the business is still flagged or from the booked start: `restoreAfterFlagCleared` | `ReservationRestoredIE` |
| `AdminViewFlaggedReservations` (Q) | the queue's flagged section | — |

**Handlers (24)**

| Handler | Does | → |
|---|---|---|
| `HandleListingWentLiveIE`, `HandleListingChangedIE`, `HandleListingWentOfflineIE`, `HandleListingPausedIE`, `HandleListingResumedIE` | keep each live listing's offering and whether it is paused | none |
| `HandleHomaPolicyChangedIE` | keeps the policy in force | none |
| `HandleProviderVerifiedIE`, `HandleProviderReverifiedIE`, `HandleBusinessReverificationRequiredIE` | keep the verified-until date | none |
| `HandleProviderFreeTimeChangedIE` | replaces the free-time projection, keeping the bookings reservation holds locally (D1) | none |
| `HandleCustomerRegisteredIE`, `HandleCustomerRenamedIE`, `HandleCustomerAddressChangedIE`, `HandleCustomerAccessNotesChangedIE` | keep the customer's name, address and notes | none |
| `HandleTeamMemberAddedIE`, `HandleTeamMemberRoleChangedIE`, `HandleTeamMemberReverifiedIE` | keep the team directory | none |
| `HandleTeamMemberLapsedIE`, `HandleTeamMemberRemovedIE` | `dropFromTeam` on every reserved or under-way reservation that names the person (R5) | `TeamMemberDroppedIE` or `TeamMemberStoppedIE` |
| `HandlePaymentSecuredIE` | `confirmPayment(amount)`; for an Instant book request: check free time, `acceptInstantly`, subtract from the projection, decline clashing requests (R16) | `ReservationAcceptedIE`, or `RequestDeclinedForTakenTimeIE` |
| `HandlePaymentFailedIE` | `paymentFailed`; nothing more is published (R12) | none |
| `HandleCaseOpenedIE` | keeps the due time for both parties' views | none |
| `HandleCaseDecidedIE` | `resolve(decision, now)` | `ReservationCompletedIE` or `ReservationCancelledIE`, `SettlementDecidedIE` |
| `HandleProviderFlaggedIE` | `disputeForProviderFlag` on every accepted reservation whose completion is not submitted, including a job under way and a job dispute (R10); runs at once | `ReservationFlaggedIE` |

Out ports: `EnquiryRepository`, `ReservationRepository`, `OfferingProjection`, `PolicyProjection`, `VerifiedUntilProjection`, `ProviderFreeTimeProjection`, `CustomerProfileProjection`, `TeamDirectoryProjection`, `CaseDueProjection`, `IdentityVerificationService` (face checks), `DistanceCalculator`, `EvidenceStorage`, `ReservationIEPublisher`, `AdminAuditLog`.

### 7.8 `payment` (publishes `PaymentIE`)

| Usecase | Does | → |
|---|---|---|
| `CustomerStartPayment` | gives the app the Stripe payment session for its request (client secret): a card is authorised and charged only on acceptance, FPX is paid upfront. The session is kept in `PaymentSessionStore`; no `Payment` exists until Stripe confirms | none |
| `StripeRecordCardAuthorised`, `StripeRecordFpxPaid` | webhooks: `Payment.authoriseCard` or `payUpfrontByFpx` | `PaymentSecuredIE` |
| `StripeRecordPaymentFailed` | a declined card, or an FPX payment that failed or was abandoned; no `Payment` is created | `PaymentFailedIE` |
| `StripeRecordRefund` | records the bank's refund status for the customer's view | `PaymentRefundedIE` |
| `StripeRecordPayoutPaid`, `StripeRecordPayoutFailed` | `Payout.markPaidOut`, `markFailed` (F6) | `PayoutPaidOutIE`, `PayoutFailedIE` |
| `HomaPayOutDuePayouts` | scheduled: sends each **payable** payout to Stripe once, retrying failed ones | none |
| `AdminWaiveFine` | with a reason, once: `Fine.waive` | `FineWaivedIE` |
| `AdminReleasePayout`, `AdminWithholdPayout` | with a reason: `releaseHold`, `withhold` | `PayoutHoldReleasedIE`, `PayoutWithheldIE` |
| `CustomerViewPayments` (Q) | payments and refunds with status and expected timing; receipts (Q8) | — |
| `ProviderViewIncome` (Q) | earnings, payouts, fines being deducted; statements | — |
| `AdminViewPayments`, `AdminViewFines` (Q) | including failed payments, refunds and payouts | — |

Handlers (13)

| Handler | Does | → |
|---|---|---|
| `HandleReservationRequestedIE` | creates the Stripe payment session so it exists when the app asks; keeps the reservation's parties | none (the result comes by webhook) |
| `HandleReservationEditedIE` | card: a new authorisation replaces the old, which is released; FPX: collect the increase or refund the decrease (`adjustTotal`); if the new payment fails, first end the held one (R12) | `PaymentSecuredIE`, `PaymentRefundedIE` |
| `HandleReservationAcceptedIE` | `capture` | `PaymentCapturedIE` |
| `HandleReservationDeclinedIE`, `HandleReservationExpiredIE`, `HandleRequestCancelledIE`, `HandleRequestDeclinedForTakenTimeIE` | `cancelBeforeAcceptance`: release the authorisation, or refund FPX in full | `PaymentRefundedIE` (FPX only) |
| `HandleSettlementDecidedIE` | `settle`: refunds now, then a `Payout` through `FineDeductionPolicy.apply` for the provider's earning | `PaymentRefundedIE`, `PayoutCreatedIE` |
| `HandleFineChargedIE` | `Fine.charge` | none |
| `HandleChargebackDecidedIE` | the provider covers the amount: a `CHARGEBACK` fine, recovered like any fine | none |
| `HandleProviderFlaggedIE` | `Payout.hold` on every payable payout, now and later (R10) | `PayoutHeldIE` |
| `HandleProviderReverifiedIE` | new payouts are no longer held; held ones wait for the admins | none |
| `HandlePayoutAccountReadyIE` | keeps the payout destination | none |

Out ports: `PaymentRepository`, `PayoutRepository`, `FineRepository`, `PaymentGateway` (Stripe), `PaymentSessionStore`, `ReservationPartiesProjection`, `PayoutDestinationProjection`, `PaymentInstructionLog` (each instruction executed once, with retries), `PaymentIEPublisher`, `AdminAuditLog`.

### 7.9 `resolution` (publishes `ResolutionIE`)

| Usecase | Does | → |
|---|---|---|
| `AdminViewCaseQueue` (Q) | `Case.QUEUE_ORDER`, with the time left to each `decisionDueAt` | — |
| `AdminViewCase` (Q) | the case, its evidence; the reservation and chat are read from their own contexts (D2) | — |
| `AdminDecideCase` | with a reason: `decide`; a chargeback case gives `ChargebackDecidedIE`, any other `CaseDecidedIE` | `CaseDecidedIE` or `ChargebackDecidedIE` |
| `AdminOpenChargebackCase` | the card dispute: the amount the bank took back is at stake | `CaseOpenedIE` |

Handlers: `HandleReportFiledIE` (opens a case, or adds the other party's evidence to the open one; a safety report `markSafety`), `HandleNoShowChallengedIE`, `HandleCompletionDeadlineMissedIE` (each `Case.open`), `HandleReservationFlaggedIE` (`closeForProviderFlag` on an open case, none otherwise). Publishes `CaseOpenedIE` when a case opens, and `CaseClosedForProviderFlagIE` when a flag closes one. Out ports: `CaseRepository`, `ResolutionIEPublisher`, `AdminAuditLog`.

### 7.10 `conversation` (publishes none; it only listens)

| Usecase | Does |
|---|---|
| `CustomerStartChat` | the first message of an enquiry's chat: `Conversation.startByCustomer` (Q5) |
| `CustomerPostMessage`, `ProviderPostMessage` | `post`; refused in an archived chat |
| `CustomerViewChat`, `ProviderViewChat`, `CustomerViewInbox`, `ProviderViewInbox` (Q) | chats grouped by provider for the customer; the reminder that payments and messages outside Homa are not covered |
| `AdminViewCaseChat` (Q) | opened only for a case |

Handlers: `HandleEnquiryCreatedIE` (keeps who the enquiry is between), `HandleEnquiryRequestedIE` (`linkReservation`; if no one has written yet, `openForReservation`), `HandleDirectRequestMadeIE` (`openForReservation`, no message, both can write), `HandleEnquiryClosedIE` (`archive`, if a chat exists), `HandleCustomerRegisteredIE`, `HandleCustomerRenamedIE`. Out ports: `ConversationRepository`, `EnquiryPartiesProjection`, `CustomerNamesProjection`.

### 7.11 `feedback` (publishes `FeedbackIE`)

| Usecase | Does | → |
|---|---|---|
| `CustomerSubmitRating` | `Rating.submit` with the reservation's `RateableReservation` (marked rated from storage) and the limits; updates the provider's and each helper's `RatingSummary` | `RatingSubmittedIE` |
| `CustomerViewRateableReservation` (Q) | the team that came, for the rating form | — |
| `AdminHideRating` | with a reason: `Rating.hide`; the summaries `withdraw` it | `RatingHiddenIE` |
| `GuestViewProviderRatings`, `GuestViewTeamRatings` (Q) | the averages once they pass the minimum ("New" with a count below it); written feedback newest first, hidden ones left out | — |
| `AdminViewRatings` (Q) | every rating, hidden or not | — |

Handlers: `HandleReservationRequestedIE` (parties), `HandleTeamNamedIE` (named team), `HandleTeamMemberDroppedIE` (a person who came off the team cannot be rated), `HandleReservationCompletedIE` (marks the reservation rateable from this moment), `HandleHomaPolicyChangedIE` (rating window, minimum ratings). Out ports: `RatingRepository`, `RatingSummaryRepository`, `RateableReservationProjection`, `FeedbackLimitsProjection`, `FeedbackIEPublisher`, `AdminAuditLog`.

### 7.12 `notification` (new; publishes none) — D3

**Domain slice first** (before its usecases): `Recipient` (customer or provider id, language Bahasa Melayu or English, push devices), `NotificationKind`, `Notification` (recipient, kind, parameters, sent), and `NotificationPolicy` (who gets which kind, once per reservation and kind, in the recipient's language, falling back to English). The recipient's language is kept here, not in `account`, because it matters only to messages.

Usecases: `CustomerRegisterDevice`, `ProviderRegisterDevice`, `CustomerChooseLanguage`, `ProviderChooseLanguage`. One handler per event in the matrix, each calling `NotificationPolicy` and `PushSender`. The matrix is the contract; each row is a handler and a test, and every kind is sent once per recipient and reservation (R25).

| Event | Tells | What |
|---|---|---|
| `PaymentSecuredIE` | provider | a new request to answer by the response deadline (not before it is paid, R14) |
| `ReservationAcceptedIE`, `ReservationDeclinedIE`, `ReservationExpiredIE`, `RequestDeclinedForTakenTimeIE`, `PaymentFailedIE` | customer | booked / declined / expired / declined because the time was taken / payment failed, nothing charged |
| `RequestCancelledIE`, `ReservationEditedIE`, `AccessNotesChangedIE` | provider | request cancelled / edited / notes changed |
| `ReservationCancelledIE` | the other party | who cancelled, the fee, the refund |
| `TeamNamedIE`, `TeamMemberDroppedIE`, `TeamMemberStoppedIE` | customer | who is coming; a change; a person who cannot work |
| `ProviderCheckedInIE`, `FaceCheckRecordedIE`, `CheckInCompletedIE` | customer | checked in at (time); each person's face check result; "face check unavailable" |
| `CompletionSubmittedIE` | customer | the job is marked completed; the dispute window ends at (time) |
| `ReportFiledIE`, `CompletionDeadlineMissedIE`, `NoShowChallengedIE`, `CaseOpenedIE` | both parties | a report is open and the decision is due by (time) |
| `ReservationFlaggedIE`, `ReservationRestoredIE` | both parties | the job is on hold while Homa checks / restored |
| `CaseDecidedIE`, `ChargebackDecidedIE` | both parties (chargeback: provider) | the outcome |
| `FineChargedIE`, `FineWaivedIE` | provider | a fine; a waiver |
| `PaymentRefundedIE`, `PaymentCapturedIE` | customer | refund status and expected timing; charged |
| `PayoutCreatedIE`, `PayoutPaidOutIE`, `PayoutFailedIE`, `PayoutHeldIE`, `PayoutHoldReleasedIE`, `PayoutWithheldIE` | provider | the payout and its state |
| `ReservationCompletedIE` | customer | rate this job (within the rating window) |
| `RatingSubmittedIE` | provider | a new rating |
| `MemberVerificationStartedIE`, `MemberVerifiedIE`, `MemberVerificationRejectedIE` | provider | waiting for verification / waiting for your confirmation / which check failed |
| `ProviderVerificationRejectedIE`, `BusinessRecheckFailedIE`, `MemberRecheckFailedIE` | provider | redo it |
| `BusinessReverificationRequiredIE`, `MemberReverificationRequiredIE`, `ProviderVerificationLapsedIE`, `TeamMemberLapsedIE`, `ProviderFlaggedIE` | provider | re-verify by (date) / you lapsed / the business is flagged |
| `ListingPausedIE`, `ListingResumedIE`, `ListingWentOfflineIE`, `ListingWentOfflineForRetiredJobIE`, `ListingWentOfflineForHoursIE`, `ListingHoursNarrowedIE`, `JobWithdrawnFromListingIE`, `ToolWithdrawnFromListingIE` | provider | what happened to the listing and what to do |
| `HoldRefusedIE` | provider | this booking clashes with your calendar |

Reminders: the team reminder (6 hours before the start while the team is not named) and the completion reminder (30 minutes after the end) arrive as `TeamReminderDueIE` and `CompletionReminderDueIE` if D8 is accepted; re-verification reminders come from the KYC provider. A chat-message push is outside the spec (Q4).

### 7.13 `tracking` (new; publishes none) — D3

**Domain slice first:** the **measures** of the Goals table, as plain definitions over facts (`SearchFact`, `ReservationFact`, `CaseFact`, `PayoutFact`): the share of searches with an exact match by city, job and time; acceptance rate and median time to answer, leaving out the automatic taken-time declines; completed with no report; late cancellation and no-show rates; book-again within 60 days; reports per 100 jobs and cases decided within 72 hours; active live providers (a completed job in the last 30 days, paid more than RM0) and median monthly payout. Each has a worked example and the boundary on either side.

Usecases (all queries, actor `Admin`): `AdminViewOverview` (the measures, reservations, booking value, service fee earned, Instant book share), `AdminSearchReservations`, `AdminViewReservationHistory` (status changes, payment states, check-in time and location result, evidence), `AdminViewProviderTracking` (cancellation count, late cancellations and no-shows, fines, upcoming and lapsed re-verifications, face check failures), `AdminViewCustomerTracking`, `AdminViewPaymentTracking`.

Handlers: one per event it reads. Built **view by view**, not all at once: Overview needs about 25, the reservation history about 30, and so on, about 60 in all. Each view is its own build step so none is a 60-handler wall. The case file and a provider's team are not copied here; the console composes them from each context (D2). `provider cancellation count` is the number of `FineChargedIE` with reason late cancellation or no-show (a waiver does not remove it).

### 7.14 `privacy` (new; publishes `PrivacyIE`) — D3

**Domain slice first** (and a product decision: Q2): `DataRequest` (subject, type `ACCESS`, `CORRECTION`, `COPY` or `ERASURE`, received, status, recorded by, the records kept by law and why), and the rule for closing an account. Usecases: `CustomerRequestAccountClosure`, `ProviderRequestAccountClosure`, `AdminRecordDataRequest`, `AdminCompleteDataRequest`, `AdminViewDataRequests`. Publishes `DataErasureRequestedIE`; `account`, `reservation`, `conversation`, `feedback` and `notification` each add a handler that deletes or anonymises their own personal data, except records kept by law (payment records). Evidence is purged after the retention period by a scheduled step in `reservation`, not here (section 9).

---

## 8. Required rules

Each rule needs a usecase test. R1 to R14 are the required rules the domain plan refers to ("R1 to R5", "R6", "R10"); their wording is in git history up to `170c533`. I re-checked each against the product description and the current domain and kept it, correcting R5 to use `dropFromTeam`, which the domain gained after the rule was first written. R15 onward are new.

| # | Rule | Where |
|---|---|---|
| R1 | One event per verification outcome; nothing for `UNCHANGED` | 7.3 |
| R2 | A paused provider cannot accept; such a request stays Requested and expires normally; if the provider re-verifies first they can accept it | `ProviderAcceptRequest` |
| R3 | Going offline or paused never touches an accepted reservation | offering and reservation handlers |
| R4 | The roster holds team members only, by role; a business is never a helper itself | offering |
| R5 | A lapsed or removed person named on a reservation comes off it at once and the customer is told; before the start the team counts as not named until the provider names someone; during the job they stop working | `HandleTeamMemberLapsedIE`, `HandleTeamMemberRemovedIE` |
| R6 | Verified for the whole job: request, edit and acceptance pass the business's verified-until date as it is now; search puts it on each candidate | reservation, discovery |
| R7 | A verified person joins only once the provider confirms the named person | account |
| R8 | Only verified people work: the face check opens for every named person; the unavailable path is taken only from the KYC provider's own status, never the provider app's word | reservation |
| R9 | The KYC provider is behind one port; webhooks are signature-checked and repeats skipped; a timed-out session is a rejection; a safety net asks about checks waiting too long | account, reservation |
| R10 | A seriously flagged provider: every accepted reservation without a submitted completion is disputed at once; the admins cancel (full refund, no fine, no fee) or restore; undecided ones are cancelled at the booked start; payouts are held; the listing pauses | reservation, payment, offering |
| R11 | A retired job or tool comes off every listing; a live listing left with no job goes offline; a request already made keeps it | offering |
| R12 | A request ends with one event and never waits unpaid: a customer cancel gives `RequestCancelledIE`; a failed payment gives `PaymentFailedIE` and reservation publishes nothing more; after a failed edit payment the held payment is ended first | reservation, payment |
| R13 | A search is recorded with no customer data | discovery |
| R14 | A request is paid before the provider answers; the provider sees it only then; an edit that raises the total waits for payment again | reservation, payment |
| **R15** | **Free time is checked at request, at edit and at acceptance, and the provider's other open requests that no longer fit are declined for taken time.** The decline does not count against the provider's response rate | reservation (D1) |
| **R16** | **Instant book:** on `PaymentSecuredIE` for an Instant book request the same handler records the payment, checks free time, accepts, and declines clashing requests, in one transaction. If the time is not free, the request ends with `declineForTakenTime` and costs nothing | `HandlePaymentSecuredIE` |
| **R17** | **The customer no-show fee is sent to payment only when the report window ends unchallenged** (F3). A challenge makes Homa's decision the only settlement | reservation |
| **R18** | **Every settlement is decided once.** A repeated delivery of a decision, or a second path to a settlement, is refused by the domain; the handler records it and changes nothing | reservation, payment |
| **R19** | **A chargeback** opens a case by an admin; its decision publishes `ChargebackDecidedIE` and not `CaseDecidedIE`, so reservation does not settle again; the provider's covered amount is recovered like a fine | resolution, payment |
| **R20** | **Chat start:** an enquiry's chat begins with the customer's first message; requesting from an enquiry whose chat has no message opens the reservation's chat with none (Q5) | conversation |
| **R21** | **The address is copied at request and cannot change on it;** a new address is for new searches and requests only | reservation |
| **R22** | **Location is used only at the moment of check-in.** The usecase keeps the distance result and the time, never the coordinates of the device | reservation |
| **R23** | **Admin audit:** each admin change is recorded with who, what, the reason and when, in the same transaction | all `Admin*` |
| **R24** | **Handlers are idempotent,** each with a "delivered twice" test | all handlers |
| **R25** | **A pending reminder is sent once** per recipient, kind and reservation | notification |

---

## 9. Scheduled work

Triggered by a scheduler (EventBridge or equivalent); the trigger lands in `infra/transport/messaging` and calls an ordinary usecase port (Discipline §10). Actor `Homa`. Every step selects what is due from a repository query, calls the domain method (which refuses early), and publishes; running twice is safe.

| Usecase | Context | Domain call | When due | → |
|---|---|---|---|---|
| `HomaExpireRequests` | reservation | `expire` | `requestExpiresAt ≤ now` | `ReservationExpiredIE` |
| `HomaMarkProviderNoShows` | reservation | `markProviderNoShow` (also a team still incomplete) | after the check-in period's last minute | `ReservationCancelledIE`, `SettlementDecidedIE`, `FineChargedIE` |
| `HomaReleaseCustomerNoShowFees` | reservation | `closeNoShowReportWindow` (F3) | the report window has ended unchallenged | `SettlementDecidedIE` |
| `HomaCloseDisputeWindows` | reservation | `closeDisputeWindow` | the window has ended with no report | `ReservationCompletedIE`, `SettlementDecidedIE` |
| `HomaRemindProviders` | reservation | `isTeamReminderDue`, `isCompletionReminderDue` (D8); a `ReminderLog` makes each fire once | 6 hours before the start while the team is not named; 30 minutes after the booked end | `TeamReminderDueIE`, `CompletionReminderDueIE` |
| `HomaMissCompletionDeadlines` | reservation | `missCompletionDeadline` | 24 hours after the booked end | `CompletionDeadlineMissedIE` |
| `HomaCancelUndecidedFlaggedReservations` | reservation | `cancelForProviderFlag` where `isFlagCancellationDue` | the booked start | `ReservationCancelledIE`, `SettlementDecidedIE` |
| `HomaCloseEnquiries` | reservation | `Enquiry.close` (F10) | the enquiry's date has passed | `EnquiryClosedIE` |
| `HomaPurgeExpiredEvidence` | reservation | none (storage) | 120 days after resolved (the policy value) | none; what a purged reference shows: Q7 |
| `HomaSyncCalendars` | availability | `replaceBusyBlocks` | the provider's sync interval | `ProviderFreeTimeChangedIE` |
| `HomaRepublishFreeTime` | availability | `freeTime` | daily, so the horizon slides | `ProviderFreeTimeChangedIE` |
| `HomaPayOutDuePayouts` | payment | `Payout.markPaidOut` / `markFailed` through Stripe | payable payouts | `PayoutPaidOutIE`, `PayoutFailedIE` |
| `HomaAskKycAboutStaleChecks` | account | `recordKybResult` / `recordKycResult` from the KYC provider's status | checks waiting too long (R9) | per outcome |

Every step is idempotent, selects only what is due, and publishes through the context's one publisher.

---

## 10. The admin console

The console is a client of the `Admin*` usecases above; it has no server of its own. What the spec's "Homa Admin" needs, and where each part lives:

| Console area | Served by |
|---|---|
| Reports: the queue, safety first, time left to the 72-hour target, the decision with a reason, both parties told | `resolution` (7.9), with `reservation` and `conversation` read at the case's request (D2) |
| Seriously flagged businesses, listed with the queue; cancel or restore each reservation; release or withhold each payout | `reservation` (7.7), `payment` (7.8) |
| Fine waivers | `payment` |
| Tracking: overview, reservations, payments, providers, customers, fines, ratings | `tracking` (7.13), and `feedback` for ratings |
| Settings: job list, tool list, showcase, Homa-set values, each with a reason where the spec asks | `catalogue`, `policy` |
| Data requests | `privacy` (7.14) |
| Every change recorded with who and when | each context's `AdminAuditLog` (R23) |

An admin's id is set by `infra/security` from the signed-in admin and never read from a request (rule I2). Whether admins have different permissions is not in the spec; today every admin can do everything.

---

## 11. Ports out and rules for the infra phase

### 11.1 Out ports by kind

- **Repositories** (one per aggregate), each refusing a save made on an out-of-date copy (rule I1).
- **Projections** (section 5), each written by one handler and read by usecases.
- **The context's one IE publisher.**
- **Outbound clients, behind ports:** `SmsSender`, `IdentityVerificationService` (KYC provider: KYB, member checks, re-verification, face checks, early re-checks), `PaymentGateway` and `PayoutAccountOnboarding` (Stripe Connect), `CalendarService` (Google, Apple), `Geocoder` and `DistanceCalculator` (open-source geoservice), `PushSender` (to be chosen), `PhotoStorage` and `EvidenceStorage` (object store), `AdminAuditLog`.
- **Time:** the `Clock`.

### 11.2 Rules infra must satisfy, each with an infra test

| # | Rule |
|---|---|
| I1 | **A save never overwrites a change made meanwhile:** optimistic locking. The Homa-set values are stricter: a save is refused unless its version is one more than the version in force. |
| I2 | **Only authentication says who is acting.** `adminId`, the customer's id and the provider's id come from `infra/security`; only admins reach `Admin*` ports, only a customer reaches their own reservations, only a provider their own listing and team. |
| I3 | **A change is all or nothing:** the save, the outbox rows and the audit entry commit together (D4, D5). |
| I4 | **Webhooks** (KYC provider, Stripe, calendar) check the signature, skip a repeat by the sender's event id, and answer quickly; slow work is queued. |
| I5 | **Stripe:** card authorise-then-capture; FPX paid upfront; refunds to the original method; fees paid by Homa out of the service fee; amounts always in whole sen. Never store card or bank details. |
| I6 | **Personal data:** the device location is not stored; the selfie is never stored (only the result and time); evidence is kept for the retention period; chats and evidence are opened by admins only for a case (the access is logged). |
| I7 | **Startup and statelessness** (Discipline §10): lazy initialisation, configuration from the environment, no in-memory sessions; the startup-budget gate applies to `app/`. |

---

## 12. Build order and definition of done

**Definition of done for a slice** (Discipline §9): every usecase and handler in it has intent-named tests against in-memory fakes; CRAP below 6; mutation at least 80% for the `usecase` layer and ratcheted, and 90% for any domain or `kernel` code the slice touched; the ArchUnit test passes; every event it publishes or consumes is in the contract test; the docs (this plan's tables, the domain plan, the audit) are updated in the same commit series.

| Slice | Contents | Needs |
|---|---|---|
| 0 | Step 0 (2.4): gates re-run, `TimeRange`, domain and `ie/` changes | — |
| 1 | Foundations (ArchUnit usecase test, test-support fakes, `AdminAuditLog`, PIT targets) and **`catalogue`**, **`policy`** | 0 |
| 2 | **`account`**: customer first, then provider, KYC reports and the team | 1 |
| 3 | **`offering`** with its 17 handlers | 0.8, 2 |
| 4 | **`availability`**, publishing free time | 0.2, 0.3 |
| 5 | **`reservation`** I: enquiry, preview, request, edit, accept, decline, expire, taken time, and all projection handlers | 0.4, 0.5, 3, 4 |
| 6 | **`payment`** I: take payment, secure, fail, capture, cancel before acceptance; **Instant book end to end** | 5 |
| 7 | **`discovery`** | 3, 4 |
| 8 | **`conversation`** | 5 |
| 9 | **`reservation`** II and **`payment`** II: cancellations, settlements, fines, payouts, Stripe payouts | 0.7, 6 |
| 10 | **`reservation`** III: team naming, check-in, face checks, no-shows, completion, dispute window | 9 |
| 11 | **`resolution`** and **`reservation`** IV: reports, cases, decisions, chargebacks, flagged providers, payout holds | 0.6, 10 |
| 12 | **`feedback`** | 10 |
| 13 | **`notification`**: domain slice, then the matrix | the events it listens to |
| 14 | **`tracking`**: domain slice (measures), then view by view | Q1 |
| 15 | **`privacy`**: domain slice, then usecases and the erasure handlers | Q2 |
| 16 | Closing: flow tests, the closing report, the audit | all |

**Order notes.** Slices 6 and 5 are built together in practice, because a request is not useful without its payment (R14); the plan keeps them apart so each passes its gates alone. Slice 7 can start any time after 4. The `app/` composition (D2) and infra start after slice 11, in the infra phase, so the first end-to-end run is a customer who books, a team that turns up and a payout. A **flow test** package, `app.flow`, wires several contexts' usecases and fakes to an in-memory event bus and replays the journeys of the stories (request and Instant book, cancellation, no-show, completion and payout, a flagged provider). It is the only test code allowed to import more than one context, and it is not a quality gate.

---

## 13. Testing approach and quality gates

### 13.1 Tests

- **Framework-free,** against in-memory fakes of the out ports in test-support files. A usecase test proves orchestration: what was loaded, passed to the domain, saved and published (the exact event record, built with the contract test's builder). It does not re-test the domain's rules.
- **Names in the ubiquitous language,** per the domain plan's convention: one class per usecase (`CustomerRequestReservationTest`), `@Nested` classes for situations (`WhenTheProviderUsesInstantBook`), methods as outcome sentences (`theRequestIsRefusedWhenTheProviderIsNotFree`). No `test`, `should`, `null`, `flag`, `dto`.
- **Each usecase test has:** the success path; each refusal the usecase can raise itself; a refused change saves, publishes and records nothing; and for handlers, delivery twice.
- **Time is passed, not decided:** where a usecase reads the clock, one test shows the domain received exactly `now` (a fixed `Clock`), so a boundary such as 10:15 or 10:16 is the domain's to judge, not the usecase's.
- **Shared builders:** `aListing().live()`, `aReservation().requested()`, `aFreeTime().forSaturday()`, `anIE()` helpers in test-support.

### 13.2 Gates

| Gate | Threshold | Enforced by |
|---|---|---|
| Unit tests | robust, intent-focused, mandatory first | JUnit 5 |
| CRAP | below 6 per method | `tools/quality_gates.py` from JaCoCo |
| Mutation, `usecase` | at least 80%, with the ratchet; report strength too | PIT, gated per layer by the script, `quality/ratchet.json` |
| Mutation, `kernel` and domain | at least 90% (unchanged), ratchet at 100% | as today |
| Dependency rule | usecase imports only its own domain and usecase, `kernel`, `ie` and `java..`; no Spring; **one in port per implementation**; **one handler per event per context**; **one publisher per context**; `ie` imports only `kernel` | `<Context>UsecaseArchitectureTest` (ArchUnit) and `IntegrationEventArchitectureTest` |
| Event coverage | every event has a publisher and a consumer (6.1) | a test over the ledger table |
| Startup budget | within budget | applies at `app/`, later |

A planted violation (a usecase importing another context's domain, a Spring annotation in a usecase) must fail each ArchUnit test once, as the domain phase did.

---

## 14. Open questions for the product owner

None of these blocks Step 0 or slices 1 to 5. Each says what I would do if there is no answer.

| # | Question | Needed by | Default |
|---|---|---|---|
| Q1 | **Newcomers measure.** "First-time searchers who make a first reservation within 7 days" needs searches linked to a person, but the Privacy policy keeps searches with nothing that identifies the customer. Options: a pseudonymous search id (a hash of the device and day) added to `SearchPerformedIE` and the policy; or measure first-time **customers** instead (registered, then first reservation within 7 days of sign-up); or drop the measure. | Slice 14 | Measure first-time customers, change nothing in `SearchPerformedIE` |
| Q2 | **Closing an account.** What happens to open reservations, unpaid fines and payouts when a customer or a provider closes their account? The spec says data is deleted or anonymised after closing, except records the law requires. | Slice 15 | A customer with an open reservation cannot close; a provider with an open reservation, an unpaid fine or a payable payout cannot close |
| Q3 | **Order of search results** within exact matches and within close matches. | Slice 7 | Exact by price then nearest start; close by the fewest differences then nearest start |
| Q4 | **Push on a new chat message.** The spec lists reminders and alerts but not chat messages. A chat nobody is told about is unlikely to work. | Slice 13 | Add `MessagePostedIE` (no text) and notify the other party |
| Q5 | **Does tapping Chat need a first message?** The domain starts a chat with one; "this creates an enquiry with its own chat" reads as if the chat exists first. | Slice 8 | The chat starts at the first message; the enquiry exists before it |
| Q6 | **Is the 72-hour target a Homa-set value?** It is stated as an aim in the Dispute Policy, not in the Homa-set table. | Slice 11 | A constant of `resolution` (D6) |
| Q7 | **After the evidence retention period,** what does a purged evidence reference show to an admin? Is a chat deleted too? | Slice 11 | The reference stays and reads "removed after 120 days"; chats are kept while the account is active |
| Q8 | **Receipts and statements** wait on the accountant's service tax and e-invoicing answer ("before launch"). What interim format is acceptable? | Slice 9 | A plain receipt per payment and refund, a plain statement per payout, marked draft |

Questions that are not mine to answer and that the spec already marks "before launch": the lawyer's answers on the minimum age, foreign team members, the lawful-employment terms, the held payouts of a flagged business, and the PDPA wording; the accountant's on the service tax; the choice of KYC provider, push service and SMS provider. None of them changes a usecase's shape; each only changes an adapter.

---

## 15. Product coverage check

Every section of the product description is served by a context and a slice below, or is stated as outside the backend usecases. A cell with a dash means no usecase is needed.

| Product section | Served by | Slice |
|---|---|---|
| What Homa Is; 7-Star Experience; Principles | the journeys below; Instant book, the response deadline, access notes, Book again are R15, R16, 7.7 | 5 to 6 |
| Goals and Measures | `tracking` 7.13 | 14 |
| Where Homa Operates (three cities, ringgit, two languages) | `catalogue` service area; `notification` language | 1, 13 |
| How Homa Works: search and browse | `discovery` 7.6 | 7 |
| Choose; Review; Reserve | `offering` data composed at the edge (D2); `reservation` 7.7 preview and request; `payment` 7.8 | 5, 6 |
| Time; Jobs and tools; Teams, helpers, supervisors | `reservation` request rules; `catalogue`; `offering` roster rules | 3, 5 |
| Pricing | `reservation` price at request; `policy` service fee | 1, 5 |
| Reservation lifecycle; payment states | `reservation`, `payment` | 5 to 11 |
| Payment Protection | `payment`, `reservation` settlements | 6, 9 |
| Request, Acceptance, Check-in, Completion, Dispute, Conduct policies | `reservation`, `resolution` | 5, 10, 11 |
| Rating Policy | `feedback` 7.11 | 12 |
| Verification Policy | `account` 7.3; `reservation` R5, R8, R10; `offering` R3, R4, R11 | 2, 3, 10, 11 |
| Privacy Policy | `privacy` 7.14; R22; I6; consent screens are the KYC provider's | 15 |
| Scope, Damage, Cancellation, Provider Cancellation Fines | `reservation`, `payment`, `offering` (Insured) | 3, 9 |
| Homa-Set Values | `policy` 7.2; every context's policy projection | 1 |
| Key Experiences: Account, Homepage, Provider page, Ratings, Availability, Chat | `account`, `catalogue`, composed provider page (D2) with `offering`, `account`, `feedback`, `reservation`, `payment`; `availability`; `conversation` | 1 to 12 |
| Provider App: onboarding, teams, after going live | `account`, `offering`, `availability`, `reservation`, `payment` | 2 to 10 |
| Homa Admin: reports, tracking, settings | section 10 | 1, 11, 14, 15 |
| Not in the MVP | not built; the plan adds no usecase for any of it | — |
| External services | the outbound clients of 11.1 | infra |

---

## Appendix: counts

Counts are approximate where marked; the ones for the existing code are from `src/main/java`.

| Measure | Number |
|---|---|
| Bounded contexts | 11 existing, 3 proposed |
| Events in `ie/` today | 82, with 9 event interfaces |
| Events after Step 0 | 91 (82 + 11 new − 2 replaced), with `AvailabilityIE` as a 10th interface |
| Events if D8 is accepted | 93 |
| Events if Q4 and slice 15 add theirs | 95, with `PrivacyIE` as an 11th interface |
| Usecases, commands and queries | about 150 |
| Event handlers | about 85 in the 11 existing contexts, about 40 in `notification`, about 60 in `tracking`, a handful in `privacy` |
| Scheduled steps | 13 |
| Domain changes in Step 0 | 13 new or changed methods and values across 8 contexts: `kernel`, `availability`, `discovery`, `reservation`, `account`, `resolution`, `payment`, `offering` |
