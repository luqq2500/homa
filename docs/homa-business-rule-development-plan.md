# Homa Business Rule Development Plan

> Ingested from Notion on 2026-11-21 (page edited 08:35 UTC): https://app.notion.com/p/41e8436ade40813b9820f4e604494d7c
> The Notion page is the source of truth; refresh this copy when it changes.

Development plan for the [Homa Business Rule](homa-business-rule.md) page, as of 21 Nov 2026.

## Summary and key decisions

Homa splits into **8 bounded contexts**. **Reservation** is the core domain and owns every fee and fine formula. **Payment** moves the money; it never decides an amount. Contexts talk only through `ie/` events, so Reservation keeps local read models of provider, team and customer state. Everything is one Maven project (a single pom.xml, Java + Spring Boot) with a single src/main/java, one package root per bounded context, and ArchUnit tests in CI enforcing the boundaries (see Source layout).

The decisions that shape everything else:

1. **Money rules live in the Reservation domain.** Fees, fines, refunds and compensation (RES-6, §6, §7, RPT-9, ROS-8, SUS-3) are worked out by `SettlementPolicy` objects. The result is a `Settlement` value object that carries the CAN-2 cap and the PAY-8 rounding rule: each line is rounded once, and the refund is the amount paid minus the rounded fee. Payment executes the `Settlement` it receives in an event and never re-derives it.
2. **Terms are snapshotted.** A `ReservationTerms` value object is frozen when the reservation is Requested (RES-8). Policies read only the snapshot, never the live listing. The customer's address is snapshotted the same way, so a later profile change never moves a booked job. Reservations are never edited, so a snapshot never changes; a different booking is a new reservation with a new snapshot (P2, REQ-1). The booking discount survives a smaller roster (ROS-8).
3. **Calendar exclusivity is its own aggregate.** `ProviderSchedule` (RES-9, with buffer) and `CustomerSchedule` (RES-10, without buffer) hold the active blocks. Optimistic locking prevents double booking. A database exclusion constraint is only a safety net; the rule stays in the domain.
4. **Phases come from time, not status.** Stored statuses follow the rules exactly: Requested, Reserved, Completed, Withdrawn, Declined, Expired, Cancelled. Withdrawn is a status of its own, not a `CancellationCause`, because a withdrawal isn't a cancellation: no fee and no reliability count (REQ-2, RTG-10). Completed is not final: a Provider No Show confirmed inside the protection window turns it into Cancelled (NSH-6). There is no checkout hold: the first confirmed payment for a time wins (RES-11). `ReservationPhase` (BeforeCheckIn, CheckIn, InJob, PostJob, Closed) is computed from `JobTime` and a `Clock`. Every time-based rule asks the phase.
5. **Cancellation reason is a value, not a status.** `Cancelled` carries a `CancellationCause` (CustomerCancelled, ProviderCancelled, NoRoster, ProviderNoShow, CustomerNoShow, CustomerSuspended, ProviderSuspended, LastMinute…). Reliability counts (RTG-10) and settlement read the cause.
6. **Reports live in Support; effects live where the state is.** Support owns a report's lifecycle and Support's confirmation. Reservation applies the money effect when it receives `ProviderNoShowConfirmed` or a similar event. Roster Member Left goes the other way: Reservation records the fact first (it changes the roster and the money), and Support creates the report automatically so it can be disputed (ROS-10, RPT-10).
7. **Time-driven rules enter through schedulers.** `infra/transport/messaging` triggers call ordinary usecase ports: expire requests, lock the roster at check-in, remind about completion, release payouts, close rating windows. All times are Asia/Kuala_Lumpur through an injected `Clock`.
8. **The rule cases are the test suite.** Each rule ID gets one intent-named test built from the §0 fixture (Aisyah, Kemas Home, the RM200 reservation). The §10 table becomes one parameterised settlement test.

### Rule principles (P1–P3)

The rules page now opens with three principles, and every new question is answered from them first. Each one maps to a single place in the design.

| Principle | Where the design enforces it |
| --- | --- |
| P1 Paid is committed | Schedules are blocked only when a payment confirms (RES-11). Payment refunds any losing, duplicate or unusable payment at once (RES-11, RES-12). A withdrawal and an acceptance change the same Reservation version, so whichever commits first wins (REQ-5) |
| P2 A change is a new booking | `Reservation` has no `edit()`. A Requested reservation can only be withdrawn (`withdraw()`) and a Reserved one cancelled (`cancelByCustomer()`); the new booking goes through `request()` under the terms and values in force then |
| P3 A reservation keeps what it was booked with | `ReservationTerms` and `ServiceAddress` snapshots; Homa's reservation values are read with `asOf(requestedAt)` in their owning context, while account values apply at once (see Homa-set values) |

### Delivery, idempotency and consistency

- **Cross-context events go through a transactional outbox.** Delivery is at-least-once and ordered per aggregate. Handling an event in the publisher's transaction is allowed only inside one context, so a Payment failure can never roll back a Reservation.
- **Every IE handler is idempotent.** A `ProcessedMessageStore` port records (eventId, handler) in the handler's own transaction, and a repeat is a no-op. Gateway webhooks are de-duplicated by the gateway's event id. Every refund, compensation and payout call carries an idempotency key equal to its settlement line id, so a replayed `ReservationCancelledIE` can't refund twice.
- **Accepted exception: checkout writes three aggregates in one transaction.** `Reservation`, `ProviderSchedule` and `CustomerSchedule` are saved together when a payment is confirmed, so the first confirmed payment wins and double booking is impossible (RES-9, RES-10, RES-11). Everywhere else, one transaction changes one aggregate. This is deliberate; don't "fix" it.
- **Calendar rules use a business calendar.** Instants are stored. Rules counted in days or months (MON-1, HLP-3, ROS-4, RTG-2) use `LocalDate` in Asia/Kuala_Lumpur through the kernel's `BusinessCalendar`, so a 90-day or 6-month rule never drifts at a day edge.

## Bounded contexts and context map

Every rule ID has exactly one owning context. Where a rule touches two contexts, the owner is the one whose aggregate enforces the invariant, and the other context reacts to an event.

| Context | Subdomain | Owns rules | Aggregates (roots) | Package root |
| --- | --- | --- | --- | --- |
| Reservation | Core | RES-1–12, REQ-1–5, ACC-1–3, ROS-1–10, JOB-1–6, CAN-1–3, CCX-1–6, PCX-1–2, NRS-1–3, LMC-1–3, SUS-1–3, NSH-3, NSH-5, NSH-6, CUS-5 (effect) | Reservation, ProviderSchedule, CustomerSchedule | `com.homa.reservation` |
| Payment | Supporting | PAY-1–11 | PaymentHold, Payout, ProviderFineAccount, Ledger | `com.homa.payment` |
| Provider | Supporting | PRV-1–6, HLP-1–8, MON-1–4 | Provider, TeamMember | `com.homa.provider` |
| Listing | Supporting | OFR-1–11 | ProviderListing, AvailabilitySchedule, Preset | `com.homa.listing` |
| Customer | Supporting | CUS-1–4, CUS-6–9 | Customer | `com.homa.customer` |
| Support | Supporting | RPT-1–11, NSH-1, NSH-2, NSH-4 | Report | `com.homa.support` |
| Rating | Supporting | RTG-1–11 | RatingSession, ReliabilityRecord | `com.homa.rating` |
| Conversation | Supporting | CHT-1–5 | Conversation | `com.homa.conversation` |
| Notification | Generic | Notify on events (ROS-1, JOB-4, NRS-1, reminders) | none (routing only) | `com.homa.notification` |
| Identity | Generic | Sign-in through an identity provider such as Google or Apple; one identity-provider account per Homa customer account (CUS-7), roles (customer, provider staff, Support) | buy: Cognito or similar | `infra.security` in each context + `com.homa.app` |

Why these boundaries:

- **Provider and Listing are split.** Provider is compliance: identity, permits, re-verification, suspension. Listing is commerce: prices, settings, availability. They change for different reasons and at different rates. One aggregate for both would lock pricing edits behind KYC state.
- **Scheduling stays inside Reservation.** RES-9 and RES-10 must hold in the same transaction that creates the reservation. Across contexts that would need a saga with a race window, so `ProviderSchedule` lives beside `Reservation`.
- **Notification and Identity have no Homa rules.** They are generic subdomains, so the plan buys or thinly wraps them and keeps them out of the core.

```mermaid
flowchart LR
  Customer -- standing --> Reservation
  Provider -- status changes --> Reservation
  Listing -- terms, availability --> Reservation
  Reservation -- settlements --> Payment
  Payment -- PaymentHeld --> Reservation
  Reservation -- member left --> Support
  Support -- no-show confirmed --> Reservation
  Reservation -- completed, causes --> Rating
  Reservation -- requested, status --> Conversation
  Reservation -- lifecycle events --> Notification
```

Only two event streams flow back into Reservation: `PaymentHeldIE` and the no-show confirmations. Provider also listens to Reservation events to count open reservations for PRV-5.

## Source layout

Homa is one Maven project (one pom.xml) with a single `src/main/java`. Each bounded context is a package root with the skill's `domain`, `usecase` and `infra` layers inside it. The compiler won't stop a wrong import, so ArchUnit tests do: they are written in slice 0, before any domain code, and a failing rule blocks the merge.

Build plugins in the `pom.xml`: `spring-boot-maven-plugin` (the one deployable), `maven-surefire-plugin` (unit and ArchUnit tests), `maven-failsafe-plugin` (Testcontainers integration tests, `*IT.java`), `jacoco-maven-plugin` (coverage, which feeds the CRAP check), and `pitest-maven` (mutation gates, scoped per package: kernel and domain at least 90%, use case at least 80%). Test dependencies: `archunit-junit5`, `testcontainers`, `wiremock`.

```txt
src/main/java/com/homa/
├── kernel/                  Money, Percentage, TimeInterval
├── ie/
│   ├── port/                ReservationIE, PaymentIE, ...
│   └── events/              ReservationRequestedIE, ...
├── reservation/
│   ├── domain/              Reservation, JobTime, settlement/, schedule/, replica/, exception/
│   ├── usecase/             port/in, port/out, dto/, exception/, ie/, *.java
│   └── infra/               persistent/, transport/{controller,messaging,client}/, security/, event/
├── payment/                 same three layers
├── provider/  listing/  customer/  support/  rating/  conversation/  notification/
└── app/                     HomaApplication, wiring of infra to ports, transaction decorators

src/test/java/com/homa/
├── architecture/            ArchitectureTest (the rules below)
├── fixtures/                HomaFixtures: Aisyah, Kemas Home, the RM200 reservation
└── <context>/...            tests mirror the main packages
```

The rules that replace module boundaries:

| # | Rule | Skill pillar it protects |
| --- | --- | --- |
| 1 | `kernel` depends only on `java..` | kernel is JDK-only |
| 2 | `ie` depends only on `kernel` and `java..` | events carry primitives and kernel types |
| 3 | `*.domain` depends only on its own context's domain, `kernel` and `java..` | domain imports nothing else, no framework |
| 4 | `*.usecase` depends only on its own context's usecase and domain, `ie`, `kernel` and `java..`, so never on `*.infra`, Spring, Jakarta or a cloud SDK | usecase reaches the outside only through `port/out` |
| 5 | Contexts don't depend on each other; only `kernel`, `ie` and `app` are shared, and only `app` may see every context | contexts talk only through `ie` events |
| 6 | `infra.transport.controller` doesn't depend on `domain` or on use case implementations, only on `usecase.port.in` and `usecase.dto` | domain objects never cross transport |
| 7 | Implementations of `*IEHandler` live in `usecase.ie`, and only `infra.event` calls them | one handler per event, listener injects the interface |

```java
@AnalyzeClasses(packages = "com.homa", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

  @ArchTest static final ArchRule kernelIsJdkOnly = classes()
      .that().resideInAPackage("com.homa.kernel..")
      .should().onlyDependOnClassesThat().resideInAnyPackage("com.homa.kernel..", "java..");

  @ArchTest static final ArchRule ieIsContractOnly = classes()
      .that().resideInAPackage("com.homa.ie..")
      .should().onlyDependOnClassesThat().resideInAnyPackage("com.homa.ie..", "com.homa.kernel..", "java..");

  // allowlist: domain sees only domain, kernel and the JDK (rule 5 then pins it to its own context)
  @ArchTest static final ArchRule domainIsPure = classes()
      .that().resideInAPackage("com.homa.*.domain..")
      .should().onlyDependOnClassesThat().resideInAnyPackage(
          "com.homa.*.domain..", "com.homa.kernel..", "java..");

  // allowlist: usecase sees only inner layers, ie, kernel and the JDK
  @ArchTest static final ArchRule usecaseUsesInnerLayersOnly = classes()
      .that().resideInAPackage("com.homa.*.usecase..")
      .should().onlyDependOnClassesThat().resideInAnyPackage(
          "com.homa.*.usecase..", "com.homa.*.domain..", "com.homa.ie..", "com.homa.kernel..", "java..");

  @ArchTest static final ArchRule contextsAreIsolated = slices()
      .matching("com.homa.(*)..").namingSlices("$1")
      .should().notDependOnEachOther()
      .ignoreDependency(resideInAPackage("com.homa.app.."), alwaysTrue())
      .ignoreDependency(alwaysTrue(), resideInAnyPackage("com.homa.kernel..", "com.homa.ie.."));

  @ArchTest static final ArchRule controllersUsePortsOnly = noClasses()
      .that().resideInAPackage("com.homa.*.infra.transport.controller..")
      .should().dependOnClassesThat().resideInAPackage("com.homa.*.domain..")
      .orShould().dependOnClassesThat(resideInAPackage("com.homa.*.usecase..")
          .and(not(resideInAnyPackage("com.homa.*.usecase.port.in..", "com.homa.*.usecase.dto.."))));
}
```

Rule 3 as written stops a domain from reaching out of the domain layer. Rule 5 stops it reaching another context's domain, so together they give "own context, kernel and JDK only". Rule 7 is a naming check added alongside the handler interfaces in slice 1.

**When to split into Maven modules.** Move a context into its own module when a second developer joins, or when it must deploy on its own (Payment first, for PCI scope). Because the packages already follow the module layout, the move is mechanical: add a parent pom with a `<module>` per context, move the package into it, and turn its ArchUnit rules into declared `<dependency>` entries.

## Kernel and integration events

`kernel/` gets only four types. Each one is used by two or more contexts, has no Homa-specific rule, and needs only the JDK.

| Type | Shape | Used by | Notes |
| --- | --- | --- | --- |
| `Money` | `BigDecimal` plus currency (MYR) | Reservation, Payment, Listing, Support | Arithmetic stays exact. `roundedToSen()` rounds HALF_UP to 2 dp, and only when a settlement line is produced (PAY-8: 39.375 becomes RM39.38). |
| `Percentage` | `BigDecimal` from 0 to 100 | Reservation, Payment, Listing | `of(Money)` and `complement()` (1 − p, for the discount) |
| `TimeInterval` | `Instant start`, `Instant end` | Reservation, Listing, Rating, Support | `overlaps`, `contains`, `duration`, `extendEnd(Duration)` |

The fourth, `BusinessCalendar` (the fixed Asia/Kuala_Lumpur zone with `dateOf(Instant)`, `plusDays` and `plusMonths`), serves the calendar rules in Reservation, Provider, Rating and Customer (CUS-4 suspension periods). Not admitted to the kernel: quarter-hour rounding (only Reservation needs it), city codes (Homa presets carry business meaning), and IDs (each context wraps its own, and events carry Strings).

### Integration event catalog

The `ie/port` interfaces are `CustomerIE`, `ProviderIE`, `ListingIE`, `ReservationIE`, `PaymentIE`, `SupportIE` and `RatingIE`. Each context has one publisher. Events carry Strings, `Money`, `Percentage`, `Instant` and enums as Strings, never domain objects.

| Event (`ie/events`) | Publisher | Consumers | Key payload |
| --- | --- | --- | --- |
| `CustomerRegisteredIE`, `CustomerAddressChangedIE` | Customer | Reservation, Conversation | customerId, city, status |
| `CustomerSuspendedIE`, `CustomerReactivatedIE` | Customer | Reservation (CUS-5), Conversation | customerId, at, endsAt (empty when until lifted, CUS-4, CUS-6) |
| `CustomerDeactivatedIE` | Customer | Reservation, Conversation (CUS-8) | customerId; reactivation on sign-in reuses `CustomerReactivatedIE` |
| `CustomerDataErasedIE` | Customer | Reservation, Conversation, Support, Rating (each anonymises its own copies, CUS-9) | customerId, erasedAt |
| `ProviderWentLiveIE` | Provider | Listing, Reservation, Payment | providerId, businessRegistrationNo |
| `ProviderClosedIE` | Provider | Listing, Reservation, Payment (fine becomes debt, PAY-9) | providerId, businessRegistrationNo |
| `ProviderSuspendedIE`, `ProviderReactivatedIE` | Provider | Listing, Reservation (SUS-1–3) | providerId, cause = MISSED_REVERIFICATION or MONITORING_FLAG, at |
| `ProviderBookabilityChangedIE` | Provider | Listing, Reservation, Notification (PRV-6) | providerId, bookable, reason = NO_ACTIVE_HELPERS or CLOSING |
| `InsuranceCoverageChangedIE` | Provider | Listing, Support (RPT-7) | providerId, covered, validUntil |
| `TeamMemberActivatedIE`, `TeamMemberDeactivatedIE`, `TeamMemberPermitUpdatedIE` | Provider | Reservation, Listing (default max helpers) | memberId, providerId, role, citizen/foreign, rosterableUntil (permit expiry minus Provider's 90-day buffer, HLP-3, ROS-4) |
| `TeamMemberSuspendedIE`, `TeamMemberReactivatedIE` | Provider | Reservation (ROS-5, ROS-9, ROS-10) | memberId, cause, at |
| `ListingTermsChangedIE` | Listing | Reservation, Notification (OFR-6 notice raised) | full snapshot: cities, jobs, rate, travel fee, discount, bounds, notice, buffer, policy, tier; noticeRaised |
| `AvailabilityChangedIE`, `ExternalBusyTimeSyncedIE` | Listing | Reservation | providerId, weekly hours, busy intervals |
| `ReservationCheckoutStartedIE` | Reservation | Payment | checkoutId, customerId, total (no time is held, RES-11) |
| `ReservationCheckoutRejectedIE` | Reservation | Payment (full refund at once), Notification | checkoutId, reason = TIME_TAKEN or RULE_FAILED (RES-11) |
| `ReservationRequestedIE`, `ReservationReservedIE` | Reservation | Payment, Conversation, Notification, Provider (PRV-5 open count), Customer (CUS-8 activity) | reservationId, parties, jobTime, total, protectionWindowEnd, requestedAt (P3) |
| `ReservationWithdrawnIE` | Reservation | Payment (full refund at once, PAY-7), Conversation (read-only, CHT-5), Notification, Provider (PRV-5 open count), Customer (CUS-8). Rating doesn't consume it, so withdrawals never count (RTG-10) | reservationId, withdrawnAt, requestedAt |
| `ReservationDeclinedIE`, `ReservationExpiredIE` | Reservation | Payment (full refund), Conversation (read-only, CHT-5), Notification, Provider, Customer (CUS-8) | reservationId |
| `RosterChangedIE` | Reservation | Notification (ROS-1), Support | reservationId, member ids |
| `ReservationCancelledIE` | Reservation | Payment, Rating (RTG-10; voids and removes ratings for NSH-6), Conversation (CHT-5), Notification, Provider, Customer (CUS-8) | cause, settlement lines, protectionWindowEnd |
| `RosterMemberLeftIE` | Reservation | Support (auto-report), Rating, Payment, Notification | memberId, leftAt, reason, refund |
| `SettlementAdjustedIE` | Reservation | Payment | reservationId, adjustment lines (ROS-8, RPT-10 dispute) |
| `ReservationCompletedIE` | Reservation | Payment, Rating (opens the window, RTG-2), Support (completion photos as evidence, JOB-6, NSH-6), Notification (JOB-4), Customer (CUS-8 window end) | payout gross, protectionWindowEnd, completedAt, completion photo refs |
| `PaymentHeldIE`, `PaymentFailedIE` | Payment | Reservation | checkoutId, amount, paidAt |
| `RefundPaidIE`, `PayoutReleasedIE` | Payment | Notification | amounts |
| `ProviderDebtChangedIE` | Payment | Provider (PAY-9) | businessRegistrationNo, outstanding debt, outstanding fine, pending payout count (PAY-9, PAY-11) |
| `ReportOpenedIE`, `ReportResolvedIE` | Support | Payment (RPT-6 hold), Customer (CUS-8), Reservation (a refund Support decided, RPT-5, PAY-7) | reportId, reservationId, decidedRefund (optional) |
| `ProviderNoShowConfirmedIE`, `CustomerNoShowConfirmedIE` | Support | Reservation (NSH-3, NSH-5, NSH-6) | reservationId, confirmedAt |
| `RosterMemberLeftDisputeResolvedIE` | Support | Reservation | reservationId, memberId, corrected leftAt |
| `ProviderRatingRevealedIE`, `ReliabilityChangedIE` | Rating | Listing (provider page) | averages, counts |

## Homa-set values

The rules page lists every value Homa sets in one table (§11). The plan gives each value exactly one owner: a named constant in one context's domain. Other contexts never copy the number; they receive the computed result in an event, such as a `protectionWindowEnd`, a `rosterableUntil` or a bounds snapshot. Changing a value is then a one-line change in one place, reviewed like a contract change.

Each owning context keeps its values in one final class in its domain package, for example `ReservationValues` or `ProviderValues`. `HomaSetValuesTest` checks every row of §11 against its constant, so the rules page and the code can't drift apart silently. The rules split the values in two (P3). Reservation values (minimum rate and discount, notice, duration, partial-time charging, Check-in Period, acceptance deadline, Grace Period, tiers, fees, fines, compensation, cap, Protection Window, completion reminders, platform fee, fine deduction cap, rounding and Rating Window) are fixed when a reservation is Requested. A change never edits the number in place: it adds a new dated version to the owner's values class, and every owner reads the version in force at the reservation's `requestedAt` through `asOf(requestedAt)`. Payment does this for the platform fee and deduction cap, Rating for the Rating Window, and Reservation for its own; Listing's values are already frozen into the terms snapshot. Account values (helper age, permit buffer, re-verification, suspension lengths, default hours and helpers, buffer, rating stars and categories, reliability period) have one current version and apply at once. `HomaSetValuesTest` also checks that a reservation Requested before a change keeps the old version.

| Value (§11) | Owner | Domain type | Other contexts get it as |
| --- | --- | --- | --- |
| Minimum helper age 18 | Provider | `AgeRequirement` | — |
| Work permit buffer 90 days | Provider | `PermitRequirement` | `rosterableUntil` on team-member events (ROS-4) |
| Re-verification every 6 months, reminders 60 days before; 14 days when flagged | Provider | `VerificationCycle` | — |
| Customer suspension lengths | Customer | `Suspension` | `endsAt` on `CustomerSuspendedIE` |
| Minimum rate RM20; maximum discount 99% | Listing | `MinimumRate`, `DiscountRule` | already applied in the terms snapshot |
| Default hours and helpers; duration minimum 1 h in 15-min steps | Listing | `BookingBounds` | bounds in `ListingTermsChangedIE` |
| Minimum notice: default and floors | Listing | `MinimumNotice` | notice in `ListingTermsChangedIE` |
| Buffer between jobs | Listing | `JobBuffer` | buffer in `ListingTermsChangedIE` |
| Check-in Period 15 min; Protection Window end + 24 h | Reservation | `JobTime` | `checkInStart`, `protectionWindowEnd` on reservation events |
| Acceptance deadline | Reservation | `AcceptanceDeadline` | — |
| Grace Period; tier cut-offs | Reservation | `GracePeriod`, `CancellationTier` | — |
| Customer fees, provider fines, compensation, 100% cap | Reservation | settlement policies, `Settlement` | settlement lines on events |
| Partial-time charging in 15-min steps | Reservation | `BillableTime` | refund lines |
| Completion reminders | Reservation | `CompletionReminders` | — |
| Platform fee 15.5%; fine deduction cap 20% | Payment | `PlatformFeeRate`, `FineDeductionCap` (reservation values, read with `asOf(requestedAt)`, P3) | — |
| Money rounding | kernel | `Money` | — |
| Rating Window 7 days; stars and categories; reliability over 12 months | Rating | `RatingWindow`, `Rating`, `ReliabilityRecord` | — |

Two places used to compute a value twice and now take it from its owner. Reservation compared jobs against permit expiry minus 90 days itself; it now receives `rosterableUntil` from Provider. The 1-hour, 15-minute duration floor now lives only in Listing's `BookingBounds`, and Reservation's `JobDuration` checks against the bounds in the terms snapshot.

## Domain model per context

The domain layer holds every rule that would still be true if the app's screens and endpoints were rewritten. Each object below names the rule IDs it enforces on itself. Domain imports only `kernel/`.

### Reservation (core)

```txt
reservation/domain/
├── Reservation.java              AR
├── ReservationId.java
├── ReservationStatus.java        REQUESTED, RESERVED, COMPLETED, WITHDRAWN, DECLINED, EXPIRED, CANCELLED
├── CancellationCause.java
├── ReservationPhase.java         derived from JobTime + now
├── JobTime.java                  start + JobDuration
├── JobDuration.java      RES-5
├── BillableTime.java             worked time rounded up to 15-min steps
├── ServiceAddress.java           address, city, arrival note, as at request (RES-8)
├── JobSelection.java             non-empty set of job codes
├── terms/
│   ├── ReservationTerms.java     frozen snapshot (RES-8)
│   ├── DiscountRule.java
│   ├── AcceptancePolicy.java     INSTANT, REQUEST
│   ├── AcceptanceDeadline.java   ACC-3
│   └── CancellationTier.java     FLEXIBLE 4h, STANDARD 12h, FIRM 24h
├── pricing/
│   ├── Quote.java                hoursFee, travelFee, total, baseCancellationFee
│   └── ReservationPricing.java   domain service, RES-6
├── roster/
│   ├── Roster.java               entity inside Reservation
│   ├── RosterMember.java         memberId, HELPER or SUPERVISOR
│   ├── RosterProposal.java       ROS-8 smaller roster
│   └── MemberDeparture.java      memberId, leftAt, reason
├── settlement/
│   ├── Settlement.java           lines + CAN-2 cap
│   ├── SettlementPolicy.java     interface
│   └── *Policy.java              one per money rule (table below)
├── schedule/
│   ├── ProviderSchedule.java     AR, RES-9
│   └── CustomerSchedule.java     AR, RES-10
├── replica/
│   ├── BookableProvider.java     Listing + Provider state as Reservation sees it
│   ├── RosterableMember.java
│   └── CustomerStanding.java
└── exception/
```

**`JobTime`** answers every time question, so no usecase ever does date arithmetic:

- `checkInPeriod()` is [start − 15 min, start).
- `protectionWindow()` is [check-in start, end + 24 h).
- `noticeFrom(now)` is check-in start − now (RES-4).
- `calendarBlock(buffer)` is [check-in start, end + buffer) (RES-9).
- `phaseAt(now)` returns BeforeCheckIn, CheckIn, InJob, PostJob or Closed.

**`Reservation` behaviour and the rules it enforces:**

| Method | Allowed when | Rules | Result |
| --- | --- | --- | --- |
| `request(...)` (factory, on payment held) | payment confirmed, and at that moment the time is still free and every booking rule still passes; `ProviderSchedule.block` and `CustomerSchedule.block` run in the same transaction, so the first confirmed payment wins | RES-1–5, RES-7, RES-9–11, ACC-1, ACC-3 | REQUESTED with terms snapshot and quote, RESERVED at once when INSTANT; otherwise rejected with a full refund |
| `withdraw(now)` | REQUESTED and not yet accepted. It changes the same Reservation version as `accept()`, so whichever commits first wins; if acceptance won, the customer is told the reservation is Reserved and can cancel under §6.2 (REQ-5) | REQ-1, REQ-2, REQ-5, P2 | WITHDRAWN, full refund, both schedule blocks released in the same transaction; no fee and no reliability count (RTG-10) |
| `accept(now)` | REQUESTED, now before the deadline, and no withdrawal committed first (REQ-5) | ACC-2, REQ-5 | RESERVED, `reservedAt` set |
| `decline()`, `expire(now)` | REQUESTED; expire only after the deadline | REQ-3, REQ-4 | DECLINED or EXPIRED, full refund |
| `cancelByCustomer(now)` | RESERVED before job end (a Requested reservation is withdrawn instead, REQ-1, REQ-2) | REQ-1, CCX-1–6, LMC-1, LMC-2 | CANCELLED + `Settlement` |
| `cancelByProvider(now)` | RESERVED, before job end | PCX-1, PCX-2, LMC-3, RPT-11 | CANCELLED + `Settlement` |
| `assignRoster(members, now)` | BeforeCheckIn; members rosterable; helpers = booked count | ROS-1–4, ROS-6 | roster change, customer notified |
| `proposeSmallerRoster`, `acceptProposal` | BeforeCheckIn | ROS-8 | re-quoted keeping the booking discount; difference refunded |
| `lockRosterAtCheckIn(now)` | phase reaches CheckIn | ROS-1, ROS-5, ROS-7, NRS-1–3 | locked, or CANCELLED (NO_ROSTER) + `Settlement` |
| `recordMemberLeft(member, at, reason)` | CheckIn or InJob; at least one helper stays | RPT-8, RPT-9, RPT-11, ROS-10 | departure + partial refund `Settlement` |
| `markCompleted(photos, now)` | RESERVED, now at or after job end, at least 1 photo | JOB-3 | COMPLETED |
| `applyProviderNoShow()` | RESERVED, or COMPLETED inside the protection window; report confirmed | NSH-3, NSH-6 | CANCELLED + `Settlement` |
| `applyCustomerNoShow()` | RESERVED, report confirmed | NSH-5 | CANCELLED + `Settlement` |
| `cancelForCustomerSuspension(now)` | phase is BeforeCheckIn | CUS-5 | CANCELLED, full refund, no fine |
| `cancelForProviderSuspension(now, cause)` | BeforeCheckIn: MISSED_REVERIFICATION is a provider cancellation by timing; MONITORING_FLAG is a full refund, no fine. In job: MISSED_REVERIFICATION defers, MONITORING_FLAG stops | SUS-1–3 | CANCELLED or job stopped + `Settlement` |

**Settlement policies.** Each policy is a small strategy that returns a `Settlement`. `Settlement` is a value object with these lines: customerFee, customerRefund, compensation, providerFine, providerPayoutGross. Its constructor applies CAN-2: the fee and the fine are each capped at the Total Reservation Fee, and refund + fee equals the amount held. Every line is computed exactly and rounded once to the sen, half up, and the refund is derived as paid minus the rounded fee (PAY-8). A refund that Support decides when resolving a report becomes a `SupportRefundPolicy` settlement, capped at what is still held (RPT-5, PAY-7).

| Policy | Rules | Formula on the standard reservation |
| --- | --- | --- |
| `FreeCancellationPolicy` | REQ-2–4, CCX-1, CCX-3–5, CUS-5, SUS-1 (monitoring flag) | refund 200 |
| `LateCustomerCancellationPolicy` | CCX-6 | fee = base 45, refund 155 |
| `ProviderCancellationPolicy` | PCX-1, PCX-2, SUS-1 (missed re-verification) | fine 45 or 45 + 10%; compensation 0 or 3% |
| `NoRosterPolicy` | NRS-2, NRS-3 | fine 45 + 12% = 69; compensation 4% = 8 |
| `CustomerLastMinutePolicy` | LMC-1, LMC-2 | base + `BillableTime` after hour 1 × discounted all-helper rate + travel |
| `ProviderLastMinutePolicy` | LMC-3 | fine 45 + 20% = 85; compensation 7% = 14 |
| `ProviderNoShowPolicy` | NSH-3, NSH-6 | fine 45 + 30% = 105; compensation 10% = 20 |
| `CustomerNoShowPolicy` | NSH-5 | base + 30% × Hours Fee after hour 1 + travel = 105.50 |
| `MidJobStopPolicy` | SUS-3 | refund = unbilled time × discounted all-helper rate; no fine |
| `MemberLeftPolicy` | RPT-9, ROS-10 | refund = that helper's unbilled time × discounted per-helper rate; 0 for a supervisor |
| `SmallerRosterPolicy` | ROS-8 | re-quote keeping the booking discount: 25 × 4 × 1 × 0.9 + 20 = 110, refund 90 |

**Schedules.** `ProviderSchedule.block(reservationId, interval)` rejects any overlap with an active block (RES-9). `CustomerSchedule.block(...)` checks job time only (RES-10). Blocks are released on Withdrawn, Declined, Expired, Cancelled and Completed. There is no checkout hold. Blocks are placed only when a payment is confirmed: the first confirmed payment for a time wins, and any later confirmed payment for that time is rejected and refunded in full at once (RES-11). Order is decided by which transaction commits first; optimistic locking on `ProviderSchedule` makes the loser retry and see the block.

**Replicas.** `BookableProvider.admits(request, now)` checks bookability (PRV-6), city coverage, hour and helper bounds (max helpers is the smaller of the setting and the active helper count), notice, job time inside working hours (check-in and buffer may fall outside, RES-3) and external busy time. `RosterableMember.canWorkOn(jobEnd)` checks active status and the permit rule, job end ≤ `rosterableUntil`, which Provider computes from the permit expiry and its 90-day buffer and publishes (ROS-3, ROS-4); Reservation never holds the 90 days itself.

### Payment

| Object | Kind | Rules | Behaviour |
| --- | --- | --- | --- |
| `PaymentHold` | AR per reservation | PAY-1, PAY-4, PAY-7, RES-12 | `hold`, `refund(amount)` (never more than held; every refund line, full or partial, is paid the moment its event arrives, PAY-7), several payment attempts per checkout; `recordConfirmation()` keeps the first confirmed payment and refunds any later one for the same checkout in full at once (RES-12), `refundRejectedCheckout()`, `applySettlement` |
| `Payout` | AR per reservation | PAY-2, PAY-3, PAY-6, RPT-6 | opened when the reservation is Reserved, so reports made before completion already hold it (RPT-6); kind JOB or FEE; its gross drops by every partial refund; `markCompleted`, `reportOpened` and `reportResolved`; `releasableAt(now)`; platform fee 15.5% on every payout, JOB and FEE (PAY-3) |
| `ProviderFineAccount` | AR per business registration number | PAY-4, PAY-5, PAY-9–11 | fines oldest first; `deductFrom(payoutAfterPlatformFee)` takes at most 20%, on every payout kind; a suspension leaves the balance as a fine; `closeAccount()` lifts the 20% cap so pending payouts clear the fine in full; what is left after the last pending payout becomes `Debt`; `settleDebt(directPayment)` and `settleFineToRejoin(directPayment)`, for a closed provider rejoining while payouts are pending (PAY-11), are the only direct charges |
| `Ledger`, `LedgerEntry` | AR, double entry | PAY-4 | accounts: customer held, provider payable, Homa revenue (platform fee + fine minus compensation), Homa compensation expense, fine receivable |
| `PlatformFeeRate` | VO, 15.5% | PAY-3 | business constant, not configuration (§11 Homa-set values; `FineDeductionCap` 20% sits beside it) |

### Provider

| Object | Kind | Rules | Behaviour and invariants |
| --- | --- | --- | --- |
| `Provider` | AR | PRV-1, PRV-2, PRV-4–6, MON-1–4, PAY-9 | `register` refuses while the business registration has debt, or has payouts still pending and an unpaid fine (PAY-9, PAY-11); `goLive(activeHelperCount)` needs at least 1; `bookabilityFor(activeHelperCount)` turns false at 0 (PRV-6); `close(openReservations)` needs 0 and runs in two steps: `startClosing()` first turns bookability off, so no request can slip in while the replica catches up (PRV-5); `suspend(cause)`; `reverify(at)`; `reactivate` is automatic after MON-3 (fines don't block it) and needs Support after MON-4 |
| `TeamMember` | AR (one per person) | HLP-1–8, MON-1–4 | role HELPER or SUPERVISOR; `Eligibility` is `Citizen` or `ForeignWorker(permitExpiry, employmentConfirmed)`; `isActive()` = verified, not suspended and not deactivated; `reactivate` needs valid verification |
| `VerificationCycle` | VO | MON-1, MON-2 | `dueAt` = date passed + 6 months; reminders from due − 60 days; monitoring deadline = flag + 14 days |
| `AgeRequirement`, `PermitRequirement` | VO | HLP-1, HLP-3 | at least 18 years old; permit at least 90 days from verification |
| `InsurancePolicy` | VO | PRV-4 | `coversOn(date)`, confirmed by Support |
| `OpenReservationCount`, `RegistrationDebt` | replicas | PRV-5, PAY-9 | kept up to date from Reservation and Payment events |

### Listing

| Object | Kind | Rules | Behaviour and invariants |
| --- | --- | --- | --- |
| `ProviderListing` | AR | OFR-1–7, OFR-9–11 | jobs, tools and cities only from presets; rate × (1 − discount) never below `MinimumRate` (RM20, a business constant), so any rate or discount change that breaks it is rejected (OFR-11); exactly one policy and one tier; notice never below the policy's floor; `choosePolicy(REQUEST)` raises a notice under 2 h to 2 h and flags it for a notification (OFR-6); any change publishes a full terms snapshot |
| `TravelFeePolicy` | VO | OFR-3 | None, Flat, or ByCity (must price every covered city) |
| `DiscountRule` | VO | OFR-4 | rate up to 99%, min hours, min helpers; none by default |
| `BookingBounds` | VO | OFR-5 | every provider's bounds sit on Homa's floor of 1 h in 15-min steps (RES-5), and this is the only place that floor is defined; hours 1–8 by default; helpers from 1 to the active helper count by default |
| `MinimumNotice` | VO | OFR-6 | 2 h by default; floor 1 h for Instant, 2 h for Request |
| `JobBuffer` | VO | OFR-10 | 30 min by default |
| `AvailabilitySchedule` | AR | OFR-8 | weekly hours + busy intervals synced from external calendars |
| `Preset` | AR (Homa admin) | OFR-1 | job types, tools, cities |

### Customer

`Customer` (AR) is built only through `register(signInId, name, HomeAddress, arrivalNote, termsAcceptance)`; no phone number is needed (CUS-1, CUS-2). One identity-provider account (issuer + subject, for example a Google or Apple account) holds one Homa account (CUS-7). The rule is the domain's, but it spans every Customer, so the use case looks the sign-in up through `CustomerRepository` and `register()` refuses a taken one; a unique index on the sign-in column backs it up when two sign-ups race. A new account starts with no ratings or reliability counts, because Rating keys everything by customerId.

- `Suspension` (VO) is SEVEN_DAYS, THIRTY_DAYS or UNTIL_LIFTED, with `endsAt` from `BusinessCalendar` (CUS-4). `suspend(period, reason, now)` and `isSuspendedAt(now)`: a timed suspension ends by itself when `endsAt` passes, and only Support can lift UNTIL_LIFTED (CUS-6).
- `deactivate(activity, now)` refuses while the customer is suspended, or while `CustomerActivity` shows any Requested or Reserved reservation, open report, or protection window not yet ended, and names each blocker (CUS-8). `reactivateOnSignIn()` reverses it.
- `erasePersonalData(now)` replaces name, address and arrival note with an anonymous marker and keeps only the id (CUS-9). It is allowed only under CUS-8's conditions and closes the account for good. If the customer is suspended, an `ErasedIdentityBlock` keeps a one-way hash of the identity-provider account, and `register()` refuses that account until the suspension would have ended, or until Support lifts an until-lifted one (CUS-9).
- `CustomerActivity` is a replica of open reservations, open reports and the latest protection window end, kept from Reservation and Support events.
- `HomeAddress` carries the city code that Reservation copies for RES-1.

### Support

`Report` (AR) is created by `Report.open(reporterRole, category, description, evidence, ReservationWindow, now)`.

- `ReportCategory.allowedFor(role)` enforces RPT-2.
- The description must not be empty (RPT-3).
- The report must fall inside the protection window (RPT-1).
- A no-show report must come after check-in ends (NSH-2, NSH-4).
- Customer No Show needs `ArrivalEvidence`: a photo and a location (NSH-4).

States are OPEN, then CONFIRMED, REJECTED or RESOLVED. Roster Member Left is auto-resolved when the protection window ends, unless the customer disputes it before then (RPT-10). A Provider No Show report is still accepted after the job was marked Completed (NSH-6). `ReservationWindow` is a replica holding the parties, check-in, job time, protection window and the completion photo refs, filled from `ReservationCompletedIE` (JOB-6, NSH-6).

### Rating

`RatingSession` (AR per reservation) exists only for Completed reservations (RTG-9). It opens when the job is marked Completed and closes 7 days after the protection window ends (RTG-2). `submit(Rating)` checks direction-specific categories (RTG-7, RTG-8) and allows media only from customer to provider (RTG-5). `revealedAt` is the moment both ratings are in or the window closes (RTG-3). `voidForProviderNoShow()` covers NSH-6 and `remove(ratingId)` covers RTG-11. A rating whose author's data was erased stays, shown as from a former customer (CUS-9).

`ReliabilityRecord` (AR per party) stores dated incidents and returns counts over the trailing 12 months, filtered by which causes count (RTG-10).

### Conversation

`Conversation` (AR) is either an `ENQUIRY` or a `RESERVATION` conversation. `startEnquiry` accepts only a customer in good standing (CHT-1, CUS-5). Reservation chat accepts messages from both sides from Requested until the protection window ends (CHT-2). It turns read-only then, or at once when the reservation is Withdrawn, Declined, Expired or cancelled before check-in starts; anything new goes through a new Enquiry (CHT-5). Writability is computed from a `ReservationChatWindow` replica (status + window end) and the clock, so no scheduler is needed. The off-platform warning (CHT-3) is UI copy, not a domain rule.

## Use cases per context

A use case loads aggregates through `port/out`, calls domain behaviour, saves, and publishes one event. It also owns application policy: who may act, and what happens next. It never computes a fee and never compares times itself; it asks `JobTime`, a policy or an aggregate.

Naming follows the skill: the interface is the actor (`ReservationAccepter`), the implementation is the action (`ProviderAcceptReservation`). Each use case has its own `dto/` request and response. Transactions wrap use cases through a decorator wired in `app/`, so `usecase/` carries no Spring annotation. Every port is one interface with one implementation. Event handler interfaces are named `<Event>IEHandler` in `port/in` (`PaymentHeldIEHandler`), each implemented by exactly one `Handle<Event>IE` in `usecase/ie`.

### Reservation

```txt
reservation/usecase/
├── port/in/      *er / *or interfaces + *IEHandler interfaces
├── port/out/     ReservationRepository, ProviderScheduleRepository,
│                 CustomerScheduleRepository, BookableProviderRepository,
│                 RosterableMemberRepository, CustomerStandingRepository,
│                 CompletionPhotoStore, ReservationIEPublisher, TimeSource
├── dto/          one request/response pair per use case
├── exception/    ReservationNotFound, NotReservationParty, TimeAlreadyTaken
├── ie/           Handle*IE implementations
└── *.java        use case implementations
```

| Interface (`port/in`) | Implementation | Trigger | Rules |
| --- | --- | --- | --- |
| `AvailableStartFinder` | `FindAvailableStartTimes` | API query | RES-3, RES-4, RES-5, RES-9, OFR-6 |
| `ReservationQuoter` | `QuoteReservation` | API query | RES-6, OFR-4 |
| `ReservationCheckoutStarter` | `CustomerStartReservationCheckout` | API | RES-1–5, RES-9, RES-10, ACC-3; checks the time is free now but holds nothing (RES-11) |
| `ReservationWithdrawer` | `CustomerWithdrawReservation` | API | REQ-1, REQ-2, REQ-5; on an optimistic-lock conflict it reloads, and if the reservation is now Reserved it returns `ReservationAlreadyAccepted` so the app offers cancellation; publishes `ReservationWithdrawnIE` |
| `ReservationAccepter` | `ProviderAcceptReservation` | API | ACC-2; loses to a withdrawal committed first (REQ-5) |
| `ReservationDecliner` | `ProviderDeclineReservation` | API | REQ-3 |
| `OverdueRequestExpirer` | `ExpireOverdueRequests` | scheduler, every minute | REQ-4 |
| `CustomerCancellationPreviewer` | `PreviewCustomerCancellation` | API query (shows the fee before confirming) | CCX-1–6, LMC-1 |
| `CustomerReservationCanceller` | `CustomerCancelReservation` | API | REQ-1, CCX, LMC-1, LMC-2 |
| `ProviderReservationCanceller` | `ProviderCancelReservation` | API | PCX-1, PCX-2, LMC-3 |
| `RosterAssigner` | `ProviderAssignRoster` | API | ROS-1–6 |
| `SmallerRosterProposer` | `ProviderProposeSmallerRoster` | API | ROS-8 |
| `SmallerRosterResponder` | `CustomerRespondToSmallerRoster` | API | ROS-8 |
| `CheckInRosterLocker` | `LockRostersAtCheckIn` | scheduler | ROS-1, ROS-7, NRS-1–3 |
| `RosterMemberLeftRecorder` | `ProviderRecordRosterMemberLeft` | API | RPT-8, RPT-9, RPT-11 |
| `ReservationCompleter` | `ProviderCompleteReservation` | API | JOB-3; its event opens ratings (RTG-2) |
| `UncompletedJobReminder` | `RemindUncompletedJobs` | scheduler at end and end + 2 h | JOB-5 |
| `ReservationViewer`, `ReservationLister` | `ViewReservation`, `ListMyReservations` | API query | ROS-6 |

Event handlers (`usecase/ie/`, one per event):

| Handler implementation | Reacts to | Effect |
| --- | --- | --- |
| `HandlePaymentHeldIE` | `PaymentHeldIE` | re-checks the time and every booking rule and blocks both schedules in one transaction: the first confirmed payment wins and becomes REQUESTED (RES-7), RESERVED if Instant (ACC-1); a payment that loses publishes `ReservationCheckoutRejectedIE` so Payment refunds it at once (RES-11) |
| `HandlePaymentFailedIE` | `PaymentFailedIE` | nothing to release: no time is blocked until a payment is confirmed, and the customer can retry (RES-11) |
| `HandleListingTermsChangedIE`, `HandleAvailabilityChangedIE`, `HandleExternalBusyTimeSyncedIE` | Listing events | update `BookableProvider` only; existing reservations keep their terms (RES-8) |
| `HandleProviderWentLiveIE`, `HandleProviderClosedIE`, `HandleProviderBookabilityChangedIE` | Provider events | bookable on or off (PRV-6) |
| `HandleProviderSuspendedIE` | `ProviderSuspendedIE` | SUS-1: before check-in, missed re-verification is a provider cancellation and a flag is a free refund. SUS-2 defers; SUS-3 stops the job and publishes `SettlementAdjustedIE` so the unworked time is refunded at once (PAY-7) |
| `HandleTeamMemberSuspendedIE` | `TeamMemberSuspendedIE` | ROS-5 flags rosters to fix; ROS-9 defers; ROS-10 records the member as left |
| `HandleTeamMemberDeactivatedIE`, `...ActivatedIE`, `...PermitUpdatedIE`, `...ReactivatedIE` | Provider events | `RosterableMember` replica; ROS-5 when rostered |
| `HandleCustomerSuspendedIE` | `CustomerSuspendedIE` | CUS-5 cancels before check-in; in-job reservations continue. `HandleCustomerDeactivatedIE` and `HandleCustomerDataErasedIE` update the replica and anonymise address snapshots (CUS-8, CUS-9) |
| `HandleCustomerRegisteredIE`, `HandleCustomerReactivatedIE`, `HandleCustomerAddressChangedIE` | Customer events | `CustomerStanding` replica |
| `HandleProviderNoShowConfirmedIE` | Support | NSH-3 settlement, including from COMPLETED (NSH-6) |
| `HandleCustomerNoShowConfirmedIE` | Support | NSH-5 settlement |
| `HandleRosterMemberLeftDisputeResolvedIE` | Support | recomputes the RPT-9 refund and publishes `SettlementAdjustedIE`; `HandleReportResolvedIE` does the same for a refund Support decided (RPT-5, PAY-7). Both publish `SettlementAdjustedIE` |

### Payment

| Use case (interface → implementation) | Trigger | Rules |
| --- | --- | --- |
| `ReservationPayer` → `CustomerPayForReservation` | API (returns the gateway session) | PAY-1, RES-12 (only the first confirmed payment per checkout is published; a later one is refunded at once) |
| `GatewayPaymentConfirmer` → `ConfirmGatewayPayment` | gateway webhook via `transport/messaging` | publishes `PaymentHeldIE`/`PaymentFailedIE`, with `paidAt` |
| `HandleReservationCheckoutStartedIE` | event | opens a pending hold |
| `HandleReservationCheckoutRejectedIE` | event | full refund at once when another customer's payment took the time first (RES-11) |
| `HandleReservationDeclinedIE`, `HandleReservationExpiredIE` | event | full refund now (PAY-7); `HandleReservationWithdrawnIE` does the same for a withdrawal (REQ-2) |
| `HandleReservationCancelledIE` | event | first voids any unreleased JOB payout (NSH-6 after Completed); refund now; Homa pays compensation and keeps the fine's remainder (PAY-4); fine added to `ProviderFineAccount`; FEE `Payout` held until the window ends (PAY-6) |
| `HandleRosterMemberLeftIE`, `HandleSettlementAdjustedIE` | event | partial refund, paid at once (PAY-7) |
| `HandleReservationCompletedIE` | event | JOB `Payout` marked completed with the window end set; `HandleReservationReservedIE` opened it earlier so reports from before completion count (RPT-6) |
| `HandleReportOpenedIE`, `HandleReportResolvedIE` | event | holds and frees the payout (RPT-6) |
| `HandleProviderClosedIE` | event | closed mode: pending payouts still release when due and clear the fine in full (no 20% cap); once none are left, the remainder becomes debt and it publishes `ProviderDebtChangedIE` (PAY-9). A suspension changes nothing here |
| `DebtSettler` → `ProviderSettleDebt` | API | PAY-10, PAY-11: clears debt, or a closed provider's whole outstanding fine so it can rejoin while payouts are pending (those payouts then release less the platform fee, with nothing taken for the fine); the only direct charges |
| `DuePayoutReleaser` → `ReleaseDuePayouts` | scheduler | PAY-2, PAY-3, PAY-5, PAY-8 |

`port/out`: `PaymentGateway`, `PayoutGateway`, `PaymentHoldRepository`, `PayoutRepository`, `FineAccountRepository`, `LedgerRepository`, `PaymentIEPublisher`, `TimeSource`.

### Provider

| Use case (interface → implementation) | Trigger | Rules |
| --- | --- | --- |
| `ProviderRegistrator` → `ProviderRegisterBusiness` | API | PRV-1, PAY-9, PAY-11 (no debt, and no unpaid fine while payouts are pending) |
| `TeamMemberAdder` → `ProviderAddTeamMember` | API | HLP-1–4 |
| `TeamMemberDeactivator` → `ProviderDeactivateTeamMember`; `TeamMemberReactivator` → `ProviderReactivateTeamMember` | API | HLP-5–8; re-checks bookability and publishes `ProviderBookabilityChangedIE` (PRV-6) |
| `ProviderLauncher` → `ProviderGoLive` | API | PRV-2, PRV-3 |
| `ProviderAccountCloser` → `ProviderCloseAccount` | API | PRV-5 |
| `InsurancePolicySubmitter` → `ProviderSubmitInsurancePolicy`; `InsurancePolicyConfirmer` → `SupportConfirmInsurancePolicy` (Support) | API | PRV-4 |
| `InsuranceExpiryApplier` → `ExpireInsuranceCoverage` | scheduler, daily | PRV-4 |
| `Reverifier` → `SubmitReverification` | API + KYC callback | MON-1, MON-2 |
| `ReverificationReminder` → `RemindDueReverifications` | scheduler, daily | MON-1, MON-2 |
| `LapsedVerificationSuspender` → `SuspendLapsedVerifications` | scheduler | MON-3; also re-checks PRV-6 for the member's provider |
| `MonitoringResultRecorder` → `RecordMonitoringResult` | KYC webhook | MON-2, MON-4 |
| `SuspensionLifter` → `SupportReactivateAfterFlag` | API (Support role) | MON-4 |
| `HandleReservationRequestedIE` and closing events | event | `OpenReservationCount` for PRV-5 |
| `HandleProviderDebtChangedIE` | event | `RegistrationDebt` for PAY-9, PAY-11 |

`port/out`: `VerificationClient` (KYC vendor), `ProviderRepository`, `TeamMemberRepository`, `ProviderIEPublisher`, `TimeSource`.

### Listing

| Use case (interface → implementation) | Trigger | Rules |
| --- | --- | --- |
| `OfferingUpdater` → `ProviderUpdateOfferings` | API | OFR-1 |
| `PricingTermsSetter` → `ProviderSetPricingTerms` (rate, travel fee, discount) | API | OFR-2–4, OFR-11 (rejects a rate or discount that falls below RM20 after discount) |
| `BookingRulesSetter` → `ProviderSetBookingRules` (bounds, notice, buffer, policy, tier) | API | OFR-5, OFR-6 (auto-raise to 2 h on switching to Request, with a notice to the provider), OFR-9, OFR-10 |
| `ProofPhotoAdder` → `ProviderAddProofPhoto` | API | OFR-7 |
| `AvailabilitySetter` → `ProviderSetAvailability`; `ExternalCalendarConnector` → `ProviderConnectExternalCalendar` | API | OFR-8 |
| `ExternalCalendarSyncer` → `SyncExternalCalendars` | scheduler or webhook | OFR-8 |
| `ProviderSearcher` → `SearchProvidersByCity`, `ProviderPageViewer` → `ViewProviderPage` | API query | RES-1, PRV-4 tag, RTG-6, RTG-10 |
| `PresetManager` → `AdminManagePresets` (Homa admin) | API | OFR-1 |
| Handlers for Provider, TeamMember, Insurance and Rating events | event | listing visibility, active helper count, page badges |

`port/out`: `ListingRepository`, `AvailabilityRepository`, `PresetRepository`, `CalendarClient`, `ListingIEPublisher`.

### Customer

| Use case (interface → implementation) | Trigger | Rules |
| --- | --- | --- |
| `CustomerRegistrator` → `CustomerRegisterAccount` | API | CUS-1, CUS-2, CUS-7 |
| `CustomerProfileUpdater` → `CustomerUpdateProfile` | API | CUS-1 |
| `CustomerSuspender` → `SupportSuspendCustomer` | API (Support) | CUS-4; publishes `CustomerSuspendedIE` with `endsAt` |
| `ExpiredSuspensionEnder` → `EndExpiredSuspensions` | scheduler | CUS-6; publishes `CustomerReactivatedIE` |
| `SuspensionLifter` → `SupportLiftSuspension` | API (Support) | CUS-6 (until lifted) |
| `AccountDeactivator` → `CustomerDeactivateAccount` | API | CUS-8; publishes `CustomerDeactivatedIE` |
| `SignInReactivator` → `CustomerReactivateOnSignIn` | sign-in | CUS-8 |
| `PersonalDataEraser` → `SupportEraseCustomerData` | API (Support) | CUS-8 conditions, CUS-9; publishes `CustomerDataErasedIE` |
| Handlers for Reservation and Support events | event | `CustomerActivity` replica (CUS-8) |

Only the Support role may call the Support use cases, an application policy checked in the use case. Every other context handles `CustomerDataErasedIE` by anonymising its own copies: Reservation's address snapshots, Conversation messages, report descriptions not needed as evidence, and rating authors, who then show as a former customer. Payment keeps its records, and outbox rows older than the retention period are purged (CUS-9).

### Support

| Use case (interface → implementation) | Trigger | Rules |
| --- | --- | --- |
| `ReportFiler` → `PartyFileReport` | API | RPT-1–3, NSH-2, NSH-4, NSH-6 |
| `NoShowEvidenceResponder` → `ProviderRespondToNoShow` | API | NSH-2 |
| `NoShowConfirmer` → `SupportConfirmNoShow`; `NoShowRejecter` → `SupportRejectNoShow` | API (Support) | NSH-2, NSH-4, NSH-6, RPT-4 |
| `ReportResolver` → `SupportResolveReport` | API (Support) | RPT-5, RPT-7; a refund Support decides travels on `ReportResolvedIE` to Reservation, which settles it (PAY-7) |
| `MemberLeftDisputer` → `CustomerDisputeMemberLeft` | API | RPT-10 |
| `MemberLeftDisputeResolver` → `SupportResolveMemberLeftDispute` | API (Support) | RPT-10, CHT-4 |
| `UndisputedReportCloser` → `AutoResolveMemberLeftReports` | scheduler at window end | RPT-10 |
| `HandleRosterMemberLeftIE` | event | auto-creates the RPT-8 report |
| `HandleReservationReservedIE` and closing events | event | `ReservationWindow` replica |

### Rating

`HandleReservationCompletedIE` opens a session, and `HandleProviderNoShowConfirmedIE` voids it (NSH-6). `RatingSubmitter` → `PartySubmitRating` covers RTG-1, RTG-4, RTG-5, RTG-7 and RTG-8. `ClosedWindowRevealer` → `RevealClosedRatingWindows` is a scheduler for RTG-3. `RatingRemover` → `SupportRemoveRating` covers RTG-11. `HandleReservationCancelledIE`, `HandleRosterMemberLeftIE` and the no-show handlers feed `ReliabilityRecord` (RTG-10). Rating has no Withdrawn handler, so withdrawals are never counted.

### Conversation

`EnquiryStarter` → `CustomerStartEnquiry` (CHT-1), `MessageSender` → `PartySendMessage` (CHT-2), and `ConversationViewer` → `ViewConversation`, which Support may call for evidence (CHT-4). `HandleReservationRequestedIE` opens the reservation thread, and the Withdrawn, Declined, Expired and Cancelled handlers make it read-only at once, the last only when the cancellation came before check-in (CHT-5). Status and suspension handlers keep the replicas current.

## Key end-to-end flows

These four flows use the §0 example and cross every context boundary. Each one becomes a cross-module acceptance test.

### A. Happy path: request, accept, complete, payout

1. Aisyah calls `FindAvailableStartTimes` and `QuoteReservation`, which returns RM200.
2. `CustomerStartReservationCheckout` asks `BookableProvider.admits(...)`, and checks the time is free now, without holding it (RES-11): nothing blocks `ProviderSchedule` for Sat 9:45–14:30 and `CustomerSchedule` for 10:00–14:00. It publishes `ReservationCheckoutStartedIE`.
3. Payment opens a pending `PaymentHold`. `CustomerPayForReservation` hands off to the gateway. If an attempt fails, Aisyah can retry; the successful attempt's webhook leads to `ConfirmGatewayPayment`, which publishes `PaymentHeldIE`.
4. `HandlePaymentHeldIE` re-checks the time and every booking rule, then calls `Reservation.request(...)`: the reservation is REQUESTED with deadline Tue 20:00, and both schedules are blocked in the same transaction, so another customer's payment for the same time would now be rejected and refunded (RES-11). It publishes `ReservationRequestedIE`: Conversation opens the thread, Provider counts an open reservation, and Notification tells Kemas Home.
5. At Mon 22:00 `ProviderAcceptReservation` makes it RESERVED. The grace period runs until Tue 02:00 (CCX-1).
6. On Friday `ProviderAssignRoster` rosters Siti and Nurul, and Aisyah is notified (ROS-1).
7. At Sat 9:45 `LockRostersAtCheckIn` locks the roster.
8. At Sat 14:00 `RemindUncompletedJobs` reminds Kemas Home if the job isn't marked yet (JOB-5).
9. `ProviderCompleteReservation` uploads a photo and publishes `ReservationCompletedIE`. Payment creates a JOB `Payout` (gross RM200, releasable from Sun 14:00). Rating opens the window until the following Sunday 14:00 (RTG-2).
10. At Sun 14:00 `ReleaseDuePayouts` takes the 15.5% platform fee (RM31). Up to 20% of the remaining RM169 (RM33.80) goes to any outstanding fine (PAY-5), and the rest is sent.

### B. Late customer cancellation (Fri 22:00)

1. `PreviewCustomerCancellation` shows a RM45 fee. The Standard cut-off was Fri 21:45 and there is no grace period.
2. `CustomerCancelReservation` calls `cancelByCustomer`. `LateCustomerCancellationPolicy` returns fee 45 and refund 155. Both schedule blocks are released.
3. `ReservationCancelledIE` (cause CUSTOMER_CANCELLED_WITH_FEE) reaches Payment, which refunds RM155 now (PAY-7) and holds a FEE payout of RM45 until Sun 14:00. On release the 15.5% platform fee (RM6.98) is taken and Kemas Home receives RM38.02 (PAY-3, PAY-6, PAY-8). Rating logs a customer cancellation with a fee (RTG-10). Conversation makes the chat read-only at once, because the cancellation came before check-in (CHT-5).

### C. Provider no show

1. At 10:05 Aisyah's `PartyFileReport` (PROVIDER_NO_SHOW) is accepted because check-in has ended. `ReportOpenedIE` freezes any payout.
2. Kemas Home sends no arrival evidence, so `SupportConfirmNoShow` publishes `ProviderNoShowConfirmedIE`.
3. Reservation calls `applyProviderNoShow()`. `ProviderNoShowPolicy` returns refund 200, compensation 20 and fine 105. It publishes `ReservationCancelledIE` (PROVIDER_NO_SHOW).
4. Payment refunds RM200 from the hold and pays RM20 from Homa's compensation account. It adds RM105 to `ProviderFineAccount`, recovered from future payouts after the platform fee, at most 20% each time. RM20 repays Homa's compensation and Homa keeps RM85 (PAY-4, PAY-5).
5. Rating logs a provider no-show (RTG-10); no ratings open, and any already opened by a false Completed are voided and removed (NSH-6). Support marks the report resolved.

### D. Helper flagged mid-job (11:00)

1. `RecordMonitoringResult` suspends Rina (MONITORING_FLAG) and publishes `TeamMemberSuspendedIE`.
2. `HandleTeamMemberSuspendedIE` finds Rina rostered on a reservation in phase InJob. Because the cause is a flag, it calls `recordMemberLeft(Rina, 11:00, MONITORING)`. Had the cause been MISSED_REVERIFICATION, it would only have scheduled the suspension for after the job (ROS-9).
3. `MemberLeftPolicy` returns a RM67.50 refund (3 h × 25 × 0.9). It publishes `RosterMemberLeftIE`.
4. Payment refunds RM67.50. Support auto-creates the Roster Member Left report. Notification tells Aisyah that a member left, without the reason (RPT-9).

## Build order, slices and testing

Build in vertical slices, and keep the skill's order inside every slice: domain first, then use case, then infra. Slice 1 alone gives a bookable, payable, completable reservation.

| Slice | Contexts touched | Rules delivered | Done when |
| --- | --- | --- | --- |
| 0. Skeleton | `kernel`, `ie`, `app` | PAY-8 (in `Money`) | `Money`, `Percentage` and `TimeInterval` pass at 90%+ mutation score; ArchUnit rules 1–6 exist and gate CI before any domain code is written; the CRAP script and PIT ratchet run in CI; the outbox and `ProcessedMessageStore` are in place; the runtime is decided (Lambda with SnapStart, or ECS/Fargate) |
| 1. Book and get paid | Customer, Provider (minimal), Listing (minimal), Reservation, Payment | CUS-1–3, CUS-7, PRV-1–3, HLP-1–4, ROS-1–3, ROS-6, OFR-1–6, OFR-9–11, RES-1–12, ACC-1–3, JOB-1–6, PAY-1–3 | flow A passes end to end |
| 2. Requests and withdrawals | Reservation, Payment | REQ-1–5 | expiry scheduler works; a withdrawal racing an acceptance resolves to whichever commits first (REQ-5) |
| 3. Cancellations | Reservation, Payment | CAN-1–3, CCX-1–6, PCX-1–2, LMC-1–3, PAY-4–7 | the §10 table passes as one parameterised test; flow B passes |
| 4. Team and roster | Provider, Reservation | PRV-6, HLP-5–8, ROS-4, ROS-5, ROS-7, ROS-8, NRS-1–3 | check-in lock and no roster work |
| 5. Reports and no-shows | Support, Reservation, Payment | RPT-1–11, NSH-1–6, CHT-4 | flow C passes, including a no-show after Completed |
| 6. Compliance, suspensions and debt | Provider, Customer, Reservation, Payment | MON-1–4, PRV-4, PRV-5, CUS-4–6, CUS-8, CUS-9, SUS-1–3, ROS-9, ROS-10, PAY-9–11 | flow D passes; a debtor can't rejoin; paying the fine directly allows rejoining with payouts pending |
| 7. Ratings and reliability | Rating, Listing | RTG-1–11 | ratings only for Completed; double-blind reveal; 12-month counts |
| 8. Chat and calendar sync | Conversation, Listing | CHT-1–3, CHT-5, OFR-7, OFR-8 | enquiries work; external busy time blocks slots |

Inside each context the order is fixed:

1. Domain value objects (`JobTime`, `Quote`, `DiscountRule`), then aggregates, then policies. Tests run in plain JUnit with a fixed `Clock`.
2. `port/in` and `port/out` interfaces and DTOs, then use cases and IE handlers. Tests use in-memory port fakes from a shared `com.homa.fixtures` test package. Maven has no test-fixtures source set; after a module split, publish the fixtures as a test-jar.
3. Infra: `persistent` (JPA or jOOQ, plus Testcontainers Postgres), `event` (Spring `ApplicationEventPublisher` with `@TransactionalEventListener`, plus an outbox table), `transport/controller`, `transport/messaging` (schedulers and webhooks), and `security` (claims mapped to CustomerId, ProviderStaff and SupportAgent).

### Testing strategy

- **One test per rule ID, named in the ubiquitous language and tagged with it.** For example, `customerPaysBaseCancellationFeeAfterStandardCutoff()`. The rule's case column is the scenario. Test classes are named for the domain object or use case they specify (`ReservationCancellationTest`, `CustomerCancelReservationTest`). Methods are camelCase sentences in Homa's own terms, subject then behaviour then condition, with no `test` prefix and no underscores (`reservationExpiresWhenProviderMissesAcceptanceDeadline`, `refundRoundsHalfUpToTheSen`). The rule ID goes in `@Tag("CCX-6")` and the rule's case sentence in `@DisplayName`, so the spec-to-code trace is still a grep.
- **The §0 setup is a shared fixture.** `HomaFixtures.kemasHome()`, `.aisyah()` and `.standardReservation()` live in `src/test/java/com/homa/fixtures` so every context uses the same numbers.
- **The §10 table is one parameterised test** on the settlement policies. It is the money regression gate.
- **Boundary tests at every time edge:** 21:44 against 21:45 for the tier cut-off, the 12:00 against 12:05 rounding step, deadline ±1 minute, a withdrawal recorded two seconds after an acceptance (REQ-5), and permit expiry minus exactly 90 days.
- **Quality gates from the skill:** CRAP index below 6. Mutation score at least 90% for kernel and domain and at least 80% for use cases, enforced with PIT plus a ratchet. ArchUnit checks that domain imports only `kernel` and that no context imports another context's packages.
- **Infra tests use real dependencies:** persistence and outbox through Testcontainers; the gateway and KYC clients through WireMock contract tests.

### Deployment

The plan ships one deployable (`app/`) holding all contexts and an in-process event bus with a transactional outbox. It starts as a single source tree (see Source layout). Splitting out Payment first is the most likely change, for PCI scope and fault isolation; once its package has moved into its own module, that switch touches only `infra/event`, `app/` and `deploy/`. Schedulers run as EventBridge rules calling `transport/messaging` handlers, which call ordinary use case ports. That fits the skill's Lambda target, but Spring Boot's cold start puts the skill's startup gate at risk, so slice 0 decides between Lambda with SnapStart and ECS/Fargate.

## Pre-kickoff review

A final review against the backend-module-discipline skill (21 Nov 2026) found the plan ready to start once the fixes below were made. All of them are now applied in this plan.

| # | Finding | Fix applied | Slice |
| --- | --- | --- | --- |
| 1 | Slice 1 couldn't pass flow A: it needs a live provider and a roster, which sat in slice 4 | Minimal Provider (PRV-1–3, HLP-1–4) and roster (ROS-1–3, ROS-6) moved into slice 1 | 1 |
| 2 | No idempotency, though an outbox and webhooks deliver at least once | `ProcessedMessageStore` per handler; idempotency key = settlement line id on every money call; webhooks de-duplicated | 0 |
| 3 | Event delivery mode was ambiguous | Outbox for every cross-context event; same-transaction handling only inside a context | 0 |
| 4 | ArchUnit rules 3 and 4 were denylists | Rewritten as `onlyDependOnClassesThat()` allowlists | 0 |
| 5 | No CRAP tool for Maven; PIT has no ratchet | CRAP computed from JaCoCo XML; last PIT score committed and enforced in CI | 0 |
| 6 | Spring Boot cold start against the startup gate | Runtime chosen in slice 0: Lambda with SnapStart, or ECS/Fargate | 0 |
| 7 | Day and month rules at risk of drifting at day edges | Kernel `BusinessCalendar` in Asia/Kuala_Lumpur | 1 |
| 8 | Some ports had two implementations; handler interfaces were unnamed | One interface per implementation; `<Event>IEHandler` naming | all |
| 9 | Support couldn't see completion photos (JOB-6, NSH-6) | Photo refs on `ReservationCompletedIE`; Support consumes it | 5 |
| 10 | NSH-6 after Completed left a JOB payout in place | Payment voids any unreleased JOB payout first | 5 |
| 11 | PRV-5 checked a replica, so a request could slip in during closing | Two-step close: `startClosing()` turns bookability off first | 6 |
| 12 | Test naming used snake_case with rule IDs | camelCase ubiquitous-language names; rule ID in `@Tag`, case in `@DisplayName` | all |

One deliberate exception is recorded under Delivery, idempotency and consistency: checkout writes three aggregates in one transaction.

## Rule gaps and how they were resolved

Four rounds of rule updates closed the 32 gaps found during planning. Six later rule changes on 21 Nov are logged as rounds 5 to 10. Round 10 removes edits altogether, which closes Y; nothing is left open. Each table keeps what was proposed next to what the rules decided at that round; later rounds can refine an earlier answer (for example, round 2 narrowed round 1's #6).

### Round 1: the original 16 gaps

| # | Gap | Originally proposed | Rule decided | Outcome |
| --- | --- | --- | --- | --- |
| 1 | State between tapping Pay and payment secured | Internal checkout with a 15-minute schedule hold, never shown as a reservation status | RES-11: a 15-minute checkout hold, not a status | Same |
| 2 | When money is rounded | Exact arithmetic; each settlement line rounded HALF_UP once | PAY-8: exact, each line rounded once half up; refund = paid minus the rounded fee | Same, plus the refund derivation |
| 3 | An edit that raises the price | Keep the original terms; a higher price needs a top-up before the edit applies; a lower price is refunded at once | REQ-1: the same | Same |
| 4 | Who keeps the fine's remainder | Homa revenue | PAY-4: Homa keeps it | Same |
| 5 | Base for the 20% fine deduction | After the platform fee, on every payout kind | PAY-5: the same | Same |
| 6 | Fines with no future payouts | Recorded as debt; collected outside the platform | PAY-9: debt blocks rejoining under the same business registration until paid | **Differs** |
| 7 | No-show after Completed | COMPLETED can become CANCELLED while a no-show report is open inside the window | NSH-6: the same; ratings are voided | Same |
| 8 | When Roster Member Left auto-resolves | At the end of the protection window | RPT-10: the same | Same |
| 9 | Which outcomes get ratings | Open for COMPLETED and CUSTOMER_NO_SHOW; window opens at job end; a late provider no-show voids it | RTG-2, RTG-9: Completed only; the window opens when the job is marked Completed | **Differs** |
| 10 | Job inside working hours | The job time must fit; the 15-minute check-in may start earlier | RES-3: job time must fit; check-in and buffer may fall outside | Same |
| 11 | Lowest minimum notice | 30 minutes; Request still needs 1 hour before check-in | OFR-6: 1 h for Instant, 2 h for Request | **Differs** |
| 12 | Re-verification cycle | 6 months from the pass date | MON-1: the same | Same |
| 13 | Provider with 0 active helpers | Hidden from search; existing reservations go through ROS-5 and ROS-7 | PRV-6: the same, and the provider is notified | Same, plus a notification |
| 14 | Provider suspended before the job | Full refund, no compensation and no fine, whatever the cause | SUS-1: missed re-verification is a provider cancellation (PCX); a monitoring flag is a free refund with no fine | **Differs** |
| 15 | Grace period against the Firm cut-off | Grace wins: free | CCX-1: grace wins | Same |
| 16 | Discount on a smaller roster | Re-check the discount before check-in, so 1 helper loses it (RM120, RM80 back); frozen after check-in | ROS-8: keep the booking discount (RM110, RM90 back) | **Differs** |

### Round 2: the 9 follow-up gaps

| # | Gap | Originally proposed | Rule decided | Outcome |
| --- | --- | --- | --- | --- |
| A | When an unpaid fine becomes debt | At closure or suspension; debt overrides MON-3's automatic reactivation | PAY-9: only at closure. During a suspension it stays a fine, deducted from payouts after reactivation | **Differs** |
| B | How debt is paid | A direct payment, used only for debt | PAY-10: the same | Same |
| C | Payment lands after the hold expired | Re-take the slot if still free; otherwise full refund at once | RES-12: the same, but every booking rule must also still pass, and the customer is told | Same, plus the rule re-check |
| D | Top-up never paid | Extra time held 15 min; then the edit is dropped and the reservation is unchanged | REQ-6: the same, and the customer is told | Same |
| E | Provider acts while a top-up is pending | Accepting accepts the original and drops the edit; expiry drops it too | REQ-6: the provider can't accept or decline while the top-up is pending | **Differs** |
| F | Instant to Request with a 1 h notice | Reject the switch until the notice is at least 2 h | OFR-6: the notice is raised to 2 h automatically and the provider is told | **Differs** |
| G | NSH-6 and ratings already revealed | Void and hide them | NSH-6: voided and removed, including revealed ones | Same |
| H | Completed after the rating window closes | No ratings open | RTG-2: the same | Same |
| I | Messaging after the reservation ends | Read-only once the reservation closes; Support can still read | CHT-2, CHT-5: writable until the protection window ends; Declined or Expired read-only at once; new questions through a new Enquiry | **Differs** |

### Round 3: the 5 follow-up gaps

| # | Gap | Originally proposed | Rule decided | Outcome |
| --- | --- | --- | --- | --- |
| J | Deadline passes during a top-up | Block a price-raising edit in the last 15 min before the deadline | REQ-7: the same; price-lowering edits are always allowed | Same |
| K | Top-up succeeds after its 15 minutes | Apply it if still Requested and every rule passes; otherwise refund at once | REQ-8: the same | Same |
| L | Customer cancels during a pending top-up | Free cancellation; edit dropped; late top-up refunded | REQ-9: the same | Same |
| M | Pending payouts when a provider closes | They still release and the debt is deducted from them in full | PAY-9: they release when due and clear the fine in full with no 20% cap; only what is still owed afterwards becomes debt | Same, but debt is fixed after the pending payouts, not at closure |
| N | Chat after a cancellation | Writable until the booked job's protection window ends, for any cancellation | CHT-5: read-only at once if cancelled before check-in; writable until the window ends if cancelled after | **Differs** |

### Round 4: the 2 follow-up gaps

| # | Gap | Originally proposed | Rule decided | Outcome |
| --- | --- | --- | --- | --- |
| O | A closed provider rejoins while a payout is still pending | Block rejoining until every pending payout settles; treat the uncleared fine as the debt meanwhile | PAY-11: blocked while any payout is pending, unless it pays its whole outstanding fine directly; pending payouts then release in full | Same, plus the pay-now option |
| P | Deadline when a late top-up applies | From when it applies, with REQ-7 re-checked then | REQ-8: the same | Same |

### Round 5: rule changes on 21 Nov

| Rule | Change | Plan impact |
| --- | --- | --- |
| CUS-7 (new) | One phone number holds one customer account; a suspended customer's number can't register again | Customer owns it; `register()` refuses a taken number, backed by a unique index; phone changes re-check it |
| RES-11 | A failed payment no longer ends the hold; the customer can retry until it expires, and leaving checkout releases it | New `CustomerLeaveCheckout` use case and `ReservationCheckoutLeftIE`; `HandlePaymentFailedIE` keeps the hold; `PaymentHold` allows several attempts |
| RPT-6 | Reports made before the job is marked Completed also hold the payout | Payment opens the `Payout` at Reserved instead of at Completed, so earlier reports are counted |

### Round 6: rule changes on 21 Nov (morning)

| Rule | Change | Plan impact |
| --- | --- | --- |
| RES-11, RES-12 | No checkout hold. The first confirmed payment for a time wins; any other is refunded at once. One checkout confirmed twice keeps the first payment and refunds the other | Schedules are blocked only when a payment is confirmed, in one transaction. `CustomerLeaveCheckout` and `ReservationCheckoutLeftIE` are removed. Payment refunds a second confirmation for the same checkout |
| REQ-6 | A changed time is no longer held during a top-up; if another customer's payment takes it first, the edit is dropped and the top-up refunded | `applyTopUp` re-checks the schedule when the top-up lands |
| CUS-1, CUS-7 | No phone number; one verified sign-in per account; a new account starts with no ratings or counts | Customer keyed by `signInId` with a unique index; the phone port is removed |
| CUS-4, CUS-6 | Suspensions last 7 days, 30 days or until lifted; timed ones end on their own | `Suspension` value object; `EndExpiredSuspensions` scheduler |
| CUS-8 (new) | Customers can deactivate when nothing is open, and reactivate by signing in | `deactivate()` checks a `CustomerActivity` replica fed by Reservation and Support |
| CUS-9 (new) | Customers can ask Support to erase their personal data | `SupportEraseCustomerData` publishes `CustomerDataErasedIE`; each context anonymises its copies; Payment keeps its records |
| OFR-4, OFR-11 (new) | Discount up to 99%; the rate after discount can't go below RM20 | `ProviderListing` rejects any rate or discount that breaks `MinimumRate` |

Round 5's questions, answered by round 6:

| # | Question | Proposed | Rule decided | Outcome |
| --- | --- | --- | --- | --- |
| Q | What counts as leaving checkout | Explicit Cancel or Back; otherwise let 15 minutes run out | RES-11: there is no hold to leave | No longer applies |
| R | Two payment attempts both succeed | Keep the first, refund the later one at once | RES-12: the same | Same |
| S | Can a customer close their account | No self-closure; Support handles it | CUS-8 and CUS-9: self-deactivation and data erasure on request | **Differs** |

### Round 7: rule changes on 21 Nov (08:30)

| Rule | Change | Plan impact |
| --- | --- | --- |
| CUS-7 | Customers sign in through an identity provider (Google, Apple); one identity-provider account per Homa account | Customer keyed by issuer + subject; Identity row updated |
| CUS-9 | Erasure only under CUS-8's conditions; a suspended customer's identity-provider account stays blocked after erasure | `erasePersonalData()` checks CUS-8's conditions; `ErasedIdentityBlock` keeps a one-way hash until the suspension ends |
| RES-8 | The reservation also keeps the customer's address from when it was made | The `ServiceAddress` snapshot already in the plan now cites RES-8; an edit can't change it |
| PAY-7 | Every refund, full or partial, is paid at once | Payment pays each refund line on arrival. Two missing paths added: a SUS-3 job stop and a refund Support decides both publish `SettlementAdjustedIE` |
| §11 (new) | One table of every Homa-set value | New Homa-set values section: one owner per value, computed results shared through events, `HomaSetValuesTest`. The permit buffer is now published as `rosterableUntil`, and the duration floor lives only in Listing |

Round 6's questions, answered by round 7:

| # | Question | Proposed | Rule decided | Outcome |
| --- | --- | --- | --- | --- |
| T | Erasure while things are still open | Only under CUS-8's conditions; erasure deactivates | CUS-9: the same conditions as deactivation | Same |
| U | Does erasure free the sign-in | A one-way hash stays blocked until the suspension ends | CUS-9: the identity-provider account stays blocked until the suspension ends | Same |

### Round 8: rule changes on 21 Nov (15:40)

| Rule | Change | Plan impact |
| --- | --- | --- |
| REQ-6 | A price-raising edit applies only once its top-up is confirmed; until then the request stays as it is and the provider can still accept or decline it | `PendingEdit` no longer locks the request or holds time. The 15-minute top-up window and the `DropUnpaidEdits` scheduler are removed |
| REQ-7 (rewritten) | The top-up is refunded at once if, before it confirms, the provider accepts or declines, another customer's payment takes the edited time, or a booking rule fails | `applyTopUp()` re-checks all three; otherwise `ReservationEditDroppedIE` triggers the refund and tells the customer |
| REQ-8 (rewritten), REQ-9 (removed) | Cancelling before the top-up confirms is free, drops the edit and refunds a late top-up (the old REQ-9, renumbered) | References renumbered throughout the plan |
| PAY-3, with PAY-6 and PAY-9 to PAY-11 examples | The 15.5% platform fee applies to every payout, including cancellation, last-minute and no-show fees | FEE payouts now pay the platform fee (RM45 becomes RM38.02). A closed provider's pending payouts release less the fee |
| §11 | Top-up window row removed; platform fee covers every payout | Homa-set values table updated |

### Round 9: rule changes on 21 Nov (16:00)

| Rule | Change | Plan impact |
| --- | --- | --- |
| Principles P1–P3 (new) | Paid is committed; an edit is a new booking check; a reservation keeps what it was booked with | New Rule principles table under the summary maps each principle to the objects that enforce it |
| REQ-1 | Edits can change date, time, hours, helpers, jobs or address; every booking rule is checked again with the original terms; the deadline is recalculated when an edit applies | `edit()` runs the same `admits()` and schedule checks as `request()`; the address snapshot is replaced on edit; the terms snapshot keeps the whole travel-fee table |
| REQ-6 | A lower price applies at once with a refund; a higher price waits for its top-up | Same-or-lower-price edits apply at once and move the schedules in one transaction |
| REQ-7 | A customer cancelling before the top-up confirms joins the refund cases (the old REQ-8 merged in) | References renumbered |
| REQ-8 (new meaning) | No second edit while a top-up is unconfirmed | `edit()` refuses while a top-up is unconfirmed |
| RES-8, P3, §11 | Homa's reservation values are fixed at Requested; account values apply at once | Owners keep dated versions read with `asOf(requestedAt)`; reservation events carry `requestedAt` |
| PAY-7 | The lower-price refund now cites REQ-6 | Reference updated |

Round 8's questions, answered by round 9:

| # | Question | Proposed | Rule decided | Outcome |
| --- | --- | --- | --- | --- |
| V | Can an edit change the address | No; cancel and book again | REQ-1: yes, if the provider covers the new city | **Differs** |
| W | Do Requested reservations keep the old §11 values | Settlement money and time values fixed; compliance values apply at once | P3 and RES-8: reservation values fixed at Requested, account values at once, each listed in full | Same, with the rules' fuller list |
| X | A second edit while a top-up is unconfirmed | The newest edit replaces the earlier one | REQ-8: no second edit until the top-up is confirmed or refunded | **Differs** |

### Round 10: rule changes on 21 Nov (16:30)

| Rule | Change | Plan impact |
| --- | --- | --- |
| P2 | Now "A change is a new booking": reservations can't be edited; withdraw or cancel, then book again under the terms in force | `edit()`, `applyTopUp()`, `dropPendingEdit()`, `PendingEdit`, `ReservationEditor` and `TopUpPayer` removed |
| REQ-1 | No edits, whether Requested or Reserved (absorbs the old REQ-5) | Customer cancellation covers Reserved only |
| REQ-2 | Withdrawing a Requested reservation: free, full refund, time reopens; not a cancellation and not counted | New `WITHDRAWN` status, `withdraw()`, `ReservationWithdrawer` and `ReservationWithdrawnIE` |
| REQ-5 (new meaning) | Withdrawal and acceptance at nearly the same moment: whichever is recorded first wins | Both change the same Reservation version under optimistic locking; the loser reloads and gets a clear outcome |
| REQ-6, REQ-7, REQ-8 | Removed (top-ups and edit refunds) | Five top-up and edit events and their handlers removed; `PaymentHold.topUp` removed |
| RES-8, RES-9, CHT-5, RTG-10, PAY-7, status glossary, §10 | Withdrawn added; the edit exception in RES-8 removed | Schedules release on Withdrawn; chat goes read-only; Rating ignores withdrawals; Payment refunds at once |

Round 9's open question, answered by round 10:

| # | Question | Proposed | Rule decided | Outcome |
| --- | --- | --- | --- | --- |
| Y | An edited address in a city the provider added after Requested | Refuse the edit; book a new reservation | P2 and REQ-1: no edits at all; every change is a new reservation | Same in spirit, applied to every change |

### Still open

None. Every gap raised so far has a rule decision.
