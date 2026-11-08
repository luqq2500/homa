# Homa Domain Development Plan

This plan covers the **domain phase only** (Backend Module Discipline §8, step 1): the `domain` layer of each bounded context, with its objects, methods, exceptions and tests. Usecases and infra come later.

**Sources:** `homa-product-description.md` (the product spec) and `backend_module_discipline.md`. When a rule here and the spec disagree, the spec wins; fix this plan.

## Contents

- Conventions for the domain phase
- Bounded contexts
- Context map
- catalogue
- policy
- account
- offering
- availability
- discovery
- reservation
- payment
- resolution
- conversation
- Not in the domain phase
- Integration events
- Build order
- Testing approach
- Open questions

## Conventions for the domain phase

- **Pure Java, no framework.** Domain classes import nothing from `usecase`, `infra` or any framework, and from shared modules only the shared kernel `kernel/` (Discipline §5, rule 1).
- **Time is passed in.** Every time-dependent method takes `Instant now` (or the times it needs) as a parameter. No clock inside the domain, so tests are deterministic.
- **Ids** use the type-safe generator: `new ReservationId().generate()` returns a `String`.
- **Shared kernel.** `kernel/` holds `Money` (Malaysian ringgit as whole sen, `long`, never `double`) and `Percentage`. Every context's domain uses these instead of defining its own. Percentages round half-up to the nearest sen (see Open questions #5).
- **Business rule violations** throw an exception from that context's `domain/exception/`, named for the rule, for example `CancellationLockedDuringCheckInPeriodException`.
- **Outcomes are values.** Methods that settle money return a value object (such as `Settlement`) rather than moving money. The `payment` context executes it.
- **Cross-context data arrives as snapshots.** A context never calls another context's domain. The usecase layer passes in what is needed (for example an `OfferingSnapshot` into `Reservation.request`). The domain never imports `ie/`: it returns values (such as `Settlement`), and usecases map them into integration events later. The events are built as the last step of this phase (see Integration events).
- **Homa-set values are snapshotted.** A reservation stores the `PolicySnapshot` that applied when it was requested, so a later change only affects new reservations.

## Bounded contexts

| Context | Owns | Main aggregates |
|---|---|---|
| `catalogue` | Homa's job list, tool list, launch cities and homepage shelves | `JobList`, `ToolList`, `ServiceArea`, `Shelf` |
| `policy` | Homa-set values and their versions | `HomaPolicy` |
| `account` | Customer and provider accounts, helpers, phone and eKYC verification status, payout account status | `CustomerAccount`, `ProviderAccount`, `Helper`, `PhoneVerification` |
| `offering` | What a provider sells: profile, jobs, tools, pricing, hours, helpers range, cities, cancellation tier, going live | `ProviderListing` |
| `availability` | When a provider can work: availability windows, synced busy blocks, accepted reservations | `ProviderCalendar` |
| `discovery` | Matching a search to providers: exact and close matches | `SearchCriteria`, `MatchClassifier` |
| `reservation` | Enquiries and the reservation lifecycle: pricing, request, accept, cancel, team, check-in, no-shows, completion, reports, dispute window, settlements | `Enquiry`, `Reservation` |
| `payment` | Money movement: authorise, hold, release, refund, payouts, fines and fine deductions | `Payment`, `Payout`, `Fine` |
| `resolution` | Cases that need the founders' decision and their outcomes | `Case` |
| `conversation` | Chats: one per enquiry or reservation, who may write, archiving | `Conversation` |

All ten depend on the shared kernel `kernel/` (`Money`, `Percentage`), which is built first.

**Why `reservation` owns the money rules and `payment` doesn't:** the cancellation tiers, no-show fees and fines all depend on *when* something happened in the lifecycle, which only `reservation` knows. So `reservation` decides **who gets how much** (`Settlement`, `FineCharge`), and `payment` decides **how the money moves** (states, holds, payouts, the 50% fine deduction cap).

## Context map

```text
catalogue ──(job/tool list, cities)──► offering ──(OfferingSnapshot)──► reservation
policy ─────(PolicySnapshot)─────────────────────────────────────────► reservation
account ────(verified, payout ready)─► offering (go-live readiness)
offering + availability ──(read models)──► discovery
availability ◄──(ReservationAccepted / Cancelled)── reservation
reservation ──(SettlementDecided, FineCharged)──► payment
reservation ──(ReportFiled, CompletionDeadlineMissed, NoShowChallenged)──► resolution
resolution ──(CaseDecided)──► reservation ──(SettlementDecided)──► payment
reservation ──(EnquiryCreated, ReservationRequested)──► conversation
```

---

## catalogue

**Purpose:** the reference lists Homa sets, which providers pick from and search matches on.

| Object | Kind | Responsibility |
|---|---|---|
| `Job` | Entity | Code, name, group (such as Kitchen), active flag |
| `JobGroup` | Value object | Display group for scanning |
| `JobList` | Aggregate | Holds jobs; codes are unique; jobs are retired, never deleted |
| `Tool` / `ToolList` | Entity / Aggregate | Same pattern as jobs |
| `ServiceArea` | Aggregate | Launch cities: Salak Tinggi, Cyberjaya, Putrajaya |
| `Shelf` | Aggregate | A curated homepage shelf: title, order, preset search (jobs, duration, helpers) |

```java
JobList.add(JobCode code, String name, JobGroup group)
JobList.rename(JobCode code, String name)
JobList.retire(JobCode code)
JobList.isOffered(JobCode code) : boolean
ServiceArea.covers(City city) : boolean
Shelf.create(String title, PresetSearch preset, int order)
```

```gherkin
Feature: Job list
  Scenario: Job codes are unique
    Given the job list has "KITCHEN_GREASE"
    When Homa adds another job with code "KITCHEN_GREASE"
    Then it is rejected with DuplicateJobCodeException

  Scenario: A retired job stays in the list but is no longer offered
    Given the job list has "WARDROBE_ORGANISE"
    When Homa retires "WARDROBE_ORGANISE"
    Then the job list still contains "WARDROBE_ORGANISE"
    And "WARDROBE_ORGANISE" is not offered

Feature: Service area
  Scenario Outline: Only launch cities are covered
    Then the service area covers "<city>" is <covered>
    Examples:
      | city         | covered |
      | Salak Tinggi | true    |
      | Cyberjaya    | true    |
      | Putrajaya    | true    |
      | Kajang       | false   |

Feature: Shelves
  Scenario: A shelf needs a preset search with at least one job
    When Homa creates a shelf "Weekend quick refresh" with no jobs
    Then it is rejected with EmptyPresetSearchException
```


---

## policy

**Purpose:** the Homa-set values table, versioned, so each reservation uses the values in force when it was requested.

| Object | Kind | Responsibility |
|---|---|---|
| `HomaPolicy` | Aggregate | Current version of all Homa-set values; changing a value creates a new version |
| `PolicySnapshot` | Value object | Immutable copy of one version, handed to `reservation` |
| `CancellationTier` | Enum | `FLEXIBLE`, `STANDARD`, `FIRM` with their percentages |

Values held: service fee 15%; duration 1–6 h; helpers per search 1–10; minimum notice 2 h; booking horizon 30 days; team reminder 6 h before the booked start; earliest check-in 15 min before the booked start; check-in period 15 min; check-in radius 300 m; customer no-show wait 15 min; customer no-show fee travel + 30%; no-show report window 1 h; request expiry 24 h or booked start; completion reminder 30 min; completion deadline 24 h; evidence retention 7 days; dispute window extension 1 h; fee window 24 h; tiers 0% / 10% / 20%; provider late cancellation fine 20%; provider no-show fine 30%; fine deduction cap 50%.

```java
HomaPolicy.initial() : HomaPolicy               // the values above, version 1
HomaPolicy.change(PolicyValue value, Instant now) : HomaPolicy  // new version
HomaPolicy.snapshot() : PolicySnapshot
```

```gherkin
Feature: Homa-set values
  Scenario: The first version holds the launch values
    When Homa creates the initial policy
    Then the service fee is 15%
    And the check-in radius is 300 metres
    And the fine deduction cap is 50%

  Scenario: Changing a value creates a new version and keeps the old one
    Given policy version 1 with a check-in period of 15 minutes
    When Homa changes the check-in period to 20 minutes
    Then policy version 2 has a check-in period of 20 minutes
    And a snapshot taken from version 1 still has 15 minutes

  Scenario: A cancellation fee can never exceed the customer no-show fee
    When Homa changes the Firm tier to 40% of the hours fee
    Then it is rejected with CancellationFeeAboveNoShowCeilingException
```


---

## account

**Purpose:** who someone is and whether they are verified. Customers verify by phone; providers and helpers by eKYC (Kenal); providers also need a ready Stripe payout account.

| Object | Kind | Responsibility |
|---|---|---|
| `CustomerAccount` | Aggregate | Verified phone, address with city |
| `PhoneVerification` | Aggregate | SMS code, expiry, attempts |
| `ProviderAccount` | Aggregate | Individual provider: eKYC status, payout account status, whether they include themselves as a helper |
| `Helper` | Aggregate | Belongs to one provider: name, photo, eKYC status |
| `VerificationStatus` | Enum | `PENDING`, `VERIFIED`, `REJECTED` |
| `Address` | Value object | Street text, city, coordinates (from the geoservice) |

```java
PhoneVerification.start(PhoneNumber phone, String code, Instant now)
PhoneVerification.confirm(String code, Instant now) : boolean
CustomerAccount.register(PhoneVerification verified, Address address)
CustomerAccount.changeAddress(Address address)

ProviderAccount.register(PhoneVerification verified, String legalName)
ProviderAccount.recordEkycResult(VerificationStatus result)
ProviderAccount.recordPayoutAccountReady()
ProviderAccount.includeSelfAsHelper(boolean include)
ProviderAccount.isReadyForGoLive() : boolean    // own eKYC verified and payout account ready

Helper.add(ProviderId provider, String name, Photo photo)
Helper.recordEkycResult(VerificationStatus result)
```

```gherkin
Feature: Customer registration
  Scenario: A customer can only register with a confirmed phone number
    Given a phone verification for "+60123456789" that is not confirmed
    When the customer registers
    Then it is rejected with PhoneNotVerifiedException

  Scenario: A wrong code does not confirm the phone
    Given a phone verification started with code "482913"
    When the customer enters "000000"
    Then the phone is not confirmed

  Scenario: An expired code is rejected
    Given a phone verification started at 10:00 with a 5-minute expiry
    When the customer enters the right code at 10:06
    Then it is rejected with VerificationCodeExpiredException

Feature: Provider readiness
  Scenario Outline: A provider is ready for go-live only when verified and payout-ready
    Given a provider whose eKYC is <ekyc> and payout account ready is <payout>
    Then the provider is ready for go-live is <ready>
    Examples:
      | ekyc     | payout | ready |
      | VERIFIED | true   | true  |
      | VERIFIED | false  | false |
      | PENDING  | true   | false |
      | REJECTED | true   | false |

Feature: Helpers
  Scenario: A new helper starts unverified
    When the provider adds helper "Aina" with a photo
    Then Aina's eKYC status is PENDING

  Scenario: A helper needs a name and a photo
    When the provider adds a helper with no photo
    Then it is rejected with HelperPhotoRequiredException
```


---

## offering

**Purpose:** a provider's listing, and the rules for going live.

| Object | Kind | Responsibility |
|---|---|---|
| `ProviderListing` | Aggregate | Profile, offered jobs and tools, pricing, ranges, cities, tier, live status |
| `Profile` | Value object | Display name and picture |
| `HourlyRate` | Value object | Rate per helper per hour, above zero |
| `HoursRange` | Value object | Min and max hours, whole hours, within Homa's 1–6 |
| `HelpersRange` | Value object | Min and max helpers, at least 1, at most the verified team size and at most 10 |
| `TravelFee` | Value object | `separate(amount)` or `included()` |
| `TeamRoster` | Value object | Verified helper ids, used for the helper range and for naming who's coming |
| `OfferingSnapshot` | Value object | What `reservation` copies at request time: rate, ranges, travel fee, jobs, cities, tier, roster |

```java
ProviderListing.draft(ProviderId provider)
ProviderListing.setProfile(Profile profile)
ProviderListing.offerJobs(Set<JobCode> jobs, Set<JobCode> activeCatalogueJobs)
ProviderListing.bringTools(Set<ToolCode> tools, Set<ToolCode> activeCatalogueTools)
ProviderListing.setPricing(HourlyRate rate, TravelFee travelFee)
ProviderListing.setHours(HoursRange hours, PolicySnapshot policy)
ProviderListing.setHelpers(HelpersRange helpers)
ProviderListing.coverCities(Set<City> cities, ServiceAreaView area)
ProviderListing.chooseCancellationTier(CancellationTier tier)
ProviderListing.updateRoster(TeamRoster roster)    // shrinks max helpers if the team shrinks
ProviderListing.goLive(boolean providerReady, boolean allHelpersVerified)
ProviderListing.missingForGoLive() : Set<GoLiveRequirement>
ProviderListing.snapshot() : OfferingSnapshot
```

```gherkin
Feature: Offering setup
  Scenario: A provider can only offer jobs from Homa's job list
    When the provider offers "DECLUTTER_GARAGE" which is not an active catalogue job
    Then it is rejected with JobNotInCatalogueException

  Scenario Outline: Hours must sit within Homa's range
    When the provider sets hours from <min> to <max>
    Then the result is <result>
    Examples:
      | min | max | result                        |
      | 1   | 4   | accepted                      |
      | 2   | 6   | accepted                      |
      | 0   | 4   | HoursOutOfRangeException      |
      | 2   | 7   | HoursOutOfRangeException      |
      | 4   | 2   | InvalidHoursRangeException    |

  Scenario: Max helpers cannot exceed the verified team size
    Given the provider has 3 verified helpers
    When the provider sets helpers from 1 to 4
    Then it is rejected with HelpersExceedTeamSizeException

  Scenario: Max helpers cannot exceed 10
    Given the provider has 12 verified helpers
    When the provider sets helpers from 1 to 11
    Then it is rejected with HelpersOutOfRangeException

  Scenario: The helper range shrinks when the team shrinks
    Given a live provider with helpers from 1 to 4 and 4 verified helpers
    When one helper is removed from the roster
    Then the helper range is 1 to 3

  Scenario: Cities must be launch cities
    When the provider covers "Kajang"
    Then it is rejected with CityNotInServiceAreaException

Feature: Going live
  Scenario: A complete, verified provider goes live
    Given a provider listing with a profile, at least one job, pricing, hours, helpers, cities and a tier
    And the provider is verified with a ready payout account
    And all helpers are verified
    When the provider goes live
    Then the listing is live

  Scenario: Missing requirements block go-live and are listed
    Given a provider listing with no profile picture and no cities
    When the provider goes live
    Then it is rejected with ListingNotReadyException
    And the missing requirements are "profile picture" and "cities"

  Scenario: An unverified helper blocks go-live
    Given a complete listing and a verified provider
    And one helper is still PENDING
    When the provider goes live
    Then it is rejected with ListingNotReadyException
```


---

## availability

**Purpose:** answers "is this provider free for this slot?" from the provider's own windows, synced calendar busy blocks and accepted reservations.

| Object | Kind | Responsibility |
|---|---|---|
| `ProviderCalendar` | Aggregate | Availability windows, busy blocks, reservation holds |
| `TimeSlot` | Value object | Start and end; start on a 15-minute step |
| `AvailabilityWindow` | Value object | When the provider says they can work |
| `BusyBlock` | Value object | From Google or Apple Calendar sync |
| `ReservationHold` | Value object | An accepted reservation's slot |

```java
ProviderCalendar.setWindows(List<AvailabilityWindow> windows)
ProviderCalendar.replaceBusyBlocks(List<BusyBlock> blocks)    // each sync replaces the last
ProviderCalendar.hold(ReservationId id, TimeSlot slot)
ProviderCalendar.release(ReservationId id)
ProviderCalendar.isAvailable(TimeSlot slot) : boolean
ProviderCalendar.nearestAvailableStart(TimeSlot wanted, Duration within) : Optional<Instant>
```

```gherkin
Feature: Provider availability
  Scenario: A slot inside a window with nothing else is available
    Given the provider is available Saturday 08:00 to 18:00
    Then Saturday 10:00 to 13:00 is available

  Scenario: A synced busy block makes the slot unavailable
    Given the provider is available Saturday 08:00 to 18:00
    And a synced busy block Saturday 12:00 to 13:00
    Then Saturday 10:00 to 13:00 is not available

  Scenario: An accepted reservation holds the slot
    Given reservation R1 holds Saturday 10:00 to 13:00
    Then Saturday 11:00 to 12:00 is not available

  Scenario: Releasing a cancelled reservation frees the slot
    Given reservation R1 holds Saturday 10:00 to 13:00
    When R1 is released
    Then Saturday 10:00 to 13:00 is available

  Scenario: Holding an overlapping slot is rejected
    Given reservation R1 holds Saturday 10:00 to 13:00
    When reservation R2 tries to hold Saturday 12:00 to 14:00
    Then it is rejected with SlotNotAvailableException

  Scenario: A slot partly outside the window is not available
    Given the provider is available Saturday 08:00 to 12:00
    Then Saturday 10:00 to 13:00 is not available
```


---

## discovery

**Purpose:** classify each provider for a search as an exact match, a close match or no match.

| Object | Kind | Responsibility |
|---|---|---|
| `SearchCriteria` | Value object | Jobs, start, duration, helpers, customer city; validated against Homa's ranges |
| `ProviderCandidate` | Value object | Read model: jobs, hours range, helpers range, cities, availability answer |
| `MatchClassifier` | Domain service | Returns `EXACT`, `CLOSE` or `NONE` with the reason |

**Exact match:** covers the city, offers every chosen job, the helpers and duration are within its ranges, and it's free at the chosen time.
**Close match:** covers the city and fits helpers and duration, but offers only some of the jobs, or is free near the chosen time (see Open questions #7).
**No match:** doesn't cover the city, helpers or duration outside its ranges, offers none of the jobs, or not free near the time.

```java
SearchCriteria.of(Set<JobCode> jobs, Instant start, int hours, int helpers, City city, Instant now, PolicySnapshot policy)
MatchClassifier.classify(SearchCriteria criteria, ProviderCandidate candidate) : MatchResult
```

```gherkin
Feature: Search criteria
  Scenario Outline: Search values must be within Homa's ranges
    When the customer searches for <hours> hours with <helpers> helpers
    Then the result is <result>
    Examples:
      | hours | helpers | result                      |
      | 1     | 1       | accepted                    |
      | 6     | 10      | accepted                    |
      | 7     | 1       | DurationOutOfRangeException |
      | 2     | 11      | HelpersOutOfRangeException  |

  Scenario: The start must respect the minimum notice
    Given it is Saturday 09:00
    When the customer searches for a start at Saturday 10:30
    Then it is rejected with StartTooSoonException

Feature: Matching
  Background:
    Given the customer searches for KITCHEN and LAUNDRY_FOLD, Saturday 10:00, 3 hours, 2 helpers, in Cyberjaya

  Scenario: Exact match
    Given a provider in Cyberjaya offering KITCHEN and LAUNDRY_FOLD, hours 2 to 4, helpers 1 to 3, free Saturday 10:00 to 13:00
    Then the provider is an EXACT match

  Scenario: Some jobs only is a close match
    Given a provider in Cyberjaya offering only KITCHEN, otherwise fitting and free
    Then the provider is a CLOSE match because of missing jobs

  Scenario: Free near the time is a close match
    Given a provider offering both jobs, fitting, free Saturday 11:00 to 14:00 but not 10:00
    Then the provider is a CLOSE match because of a nearby time

  Scenario: Not covering the city is no match
    Given a provider in Putrajaya only, offering both jobs and free
    Then the provider is NO match

  Scenario: Helpers outside the range is no match
    Given a provider in Cyberjaya with helpers 1 to 1
    Then the provider is NO match
```


---

## reservation

**Purpose:** the core of Homa: enquiries, the reservation lifecycle, every time-based rule, and the money outcome of each ending. This is the biggest context; build it in the slices listed under **Build order**.

### Domain objects

| Object | Kind | Responsibility |
|---|---|---|
| `Enquiry` | Aggregate | Chosen details before requesting; closes when its date passes; one reservation per enquiry |
| `Reservation` | Aggregate | The lifecycle below and all its rules |
| `ReservationDetails` | Value object | Jobs, start, hours, helpers, address, payment method |
| `PriceBreakdown` | Value object | Hours fee, travel fee, provider price, service fee, total |
| `ReservationStatus` | Enum | `REQUESTED`, `RESERVED`, `DECLINED`, `EXPIRED`, `CANCELLED`, `DISPUTED`, `COMPLETED` |
| `TeamAssignment` | Value object | Lead and helpers named for this reservation |
| `CheckIn` | Value object | Time checked in, distance from the customer |
| `Completion` | Value object | Time submitted and evidence references |
| `Report` | Entity | Filed by the customer or the provider: type, reason, evidence |
| `Ending` | Enum | Why it ended: `CUSTOMER_CANCELLED_FREE`, `CUSTOMER_CANCELLED_IN_FEE_WINDOW`, `CUSTOMER_NO_SHOW`, `PROVIDER_CANCELLED`, `PROVIDER_NO_SHOW`, `COMPLETED`, `DECIDED_BY_HOMA`, and so on |
| `Settlement` | Value object | Who gets what: customer refund, provider earning, Homa service fee, when it's released |
| `FineCharge` | Value object | Provider fine amount and reason |
| `CaseDecision` | Value object | The founders' decision, received from `resolution` |

### Lifecycle

```text
REQUESTED ──accept──► RESERVED ──dispute window closes, no report──► COMPLETED
    │                    │  │
    ├─decline─► DECLINED │  ├─report filed / completion deadline missed──► DISPUTED ──decision──► COMPLETED
    ├─expire──► EXPIRED  │  │
    └─cancel──► CANCELLED◄─┘ (customer or provider cancel, provider no-show, customer no-show)
                   │
                   └─customer challenges a no-show──► DISPUTED ──decision──► CANCELLED
```

### Methods

```java
// Enquiry
Enquiry.create(CustomerId c, ProviderId p, ReservationDetails details, Instant now)
Enquiry.bookAgainFrom(Enquiry past, Instant newStart, Instant now)
Enquiry.isClosed(Instant now) : boolean
Enquiry.markRequested(ReservationId id)

// Request
Reservation.request(CustomerId c, ReservationDetails d, OfferingSnapshot o, PolicySnapshot p,
                    Optional<EnquiryId> enquiry, Instant now)
Reservation.price() : PriceBreakdown
Reservation.requestExpiresAt() : Instant
Reservation.edit(ReservationDetails d, Instant now) : PriceChange
Reservation.accept(Instant now)
Reservation.decline(Instant now)
Reservation.expire(Instant now)

// Cancelling
Reservation.cancellationTerms() : CancellationTerms   // free until, fee from then, locked period, after check-in
Reservation.cancelByCustomer(Instant now) : Settlement
Reservation.cancelByProvider(Instant now) : CancellationResult   // Settlement + FineCharge

// Job day
Reservation.nameTeam(TeamAssignment team, TeamRoster roster, Instant now)
Reservation.checkIn(int distanceMetres, Instant now)
Reservation.markProviderNoShow(Instant now) : CancellationResult
Reservation.reportCustomerNoShow(Instant now) : Settlement
Reservation.challengeNoShow(Instant now)
Reservation.submitCompletion(List<EvidenceRef> evidence, Instant now)

// After the job
Reservation.disputeWindowEndsAt() : Optional<Instant>
Reservation.fileReport(Party party, ReportType type, String reason, List<EvidenceRef> evidence, Instant now)
Reservation.missCompletionDeadline(Instant now)
Reservation.closeDisputeWindow(Instant now) : Settlement
Reservation.resolve(CaseDecision decision) : Settlement   // ends COMPLETED, or CANCELLED for a challenged no-show

// Reminders (queries for the scheduler)
Reservation.isTeamReminderDue(Instant now) : boolean
Reservation.isCompletionReminderDue(Instant now) : boolean
```

### Worked numbers used in the features

- RM30 per helper per hour, 2 helpers, 3 hours → **hours fee RM180**
- Separate travel fee RM20 → **provider price RM200**
- Service fee 15% → **RM30**; **total RM230**
- Travel included instead: RM35 per helper per hour, 2 helpers, 3 hours → **provider price RM210**, service fee RM31.50, total RM241.50
- Booked Saturday 10:00 to 13:00

### Features

```gherkin
Feature: Pricing
  Scenario: Price with a separate travel fee
    Given a provider charging RM30 per helper per hour with a separate travel fee of RM20
    When the customer requests 2 helpers for 3 hours
    Then the hours fee is RM180.00
    And the provider price is RM200.00
    And the service fee is RM30.00
    And the total is RM230.00

  Scenario: Price with the travel fee included
    Given a provider charging RM35 per helper per hour with travel included
    When the customer requests 2 helpers for 3 hours
    Then the provider price is RM210.00
    And the service fee is RM31.50
    And the total is RM241.50

  Scenario: Choosing fewer jobs does not change the price
    Given a request for KITCHEN and LAUNDRY_FOLD at a total of RM230.00
    When the customer edits the request to KITCHEN only
    Then the total is still RM230.00

Feature: Requesting a reservation
  Background:
    Given it is Thursday 09:00
    And a live provider in Cyberjaya with hours 2 to 4 and helpers 1 to 3

  Scenario Outline: Request details must fit Homa's and the provider's rules
    When the customer requests <hours> hours with <helpers> helpers starting <start>
    Then the result is <result>
    Examples:
      | hours | helpers | start            | result                        |
      | 3     | 2       | Saturday 10:00   | REQUESTED                     |
      | 1     | 2       | Saturday 10:00   | HoursOutsideProviderRangeException |
      | 3     | 4       | Saturday 10:00   | HelpersOutsideProviderRangeException |
      | 3     | 2       | Saturday 10:10   | StartNotOnQuarterHourException |
      | 3     | 2       | Thursday 10:30   | StartTooSoonException         |
      | 3     | 2       | 31 days ahead    | StartBeyondHorizonException   |

  Scenario: Jobs must be offered by the provider
    When the customer requests a job the provider does not offer
    Then it is rejected with JobNotOfferedException

  Scenario: The customer's city must be covered
    Given the customer's address is in Putrajaya
    When the customer requests the Cyberjaya-only provider
    Then it is rejected with CityNotCoveredException

  Scenario: The reservation keeps the policy in force when requested
    Given policy version 1 is in force
    When the customer requests a reservation
    And Homa later publishes policy version 2
    Then the reservation still uses policy version 1

Feature: Request expiry
  Scenario Outline: A request expires after 24 hours or at the booked start, whichever is sooner
    Given a request made at <requested> for a start at <start>
    Then the request expires at <expires>
    Examples:
      | requested      | start          | expires        |
      | Thursday 09:00 | Saturday 10:00 | Friday 09:00   |
      | Saturday 06:00 | Saturday 10:00 | Saturday 10:00 |

  Scenario: The provider cannot accept an expired request
    Given a request that expires Friday 09:00
    When the provider accepts at Friday 09:01
    Then it is rejected with RequestExpiredException

  Scenario: An unanswered request expires
    Given a request that expires Friday 09:00
    When Homa expires it at Friday 09:00
    Then the reservation is EXPIRED

Feature: Accepting and declining
  Scenario: The provider accepts a request
    Given a REQUESTED reservation
    When the provider accepts before it expires
    Then the reservation is RESERVED

  Scenario: The provider declines a request
    Given a REQUESTED reservation
    When the provider declines
    Then the reservation is DECLINED

  Scenario: Only a requested reservation can be accepted
    Given a DECLINED reservation
    When the provider accepts
    Then it is rejected with InvalidStatusTransitionException

Feature: Editing a request
  Scenario: The customer edits a request before it is accepted
    Given a REQUESTED reservation for 3 hours at a total of RM230.00
    When the customer changes it to 4 hours
    Then the total is RM299.00
    And the price change is +RM69.00

  Scenario: A reserved booking cannot be edited
    Given a RESERVED reservation
    When the customer edits it
    Then it is rejected with ReservationNotEditableException

Feature: Customer cancellation
  Background:
    Given a reservation for Saturday 10:00 to 13:00 with an hours fee of RM180, travel RM20, total RM230

  Scenario: Cancelling a request is free on every tier
    Given the reservation is REQUESTED and the provider's tier is FIRM
    When the customer cancels
    Then the customer is refunded RM230.00
    And the provider earns RM0.00

  Scenario Outline: Cancelling a reserved booking before the start
    Given the reservation is RESERVED and the provider's tier is <tier>
    When the customer cancels at <time>
    Then the provider earns <earning>
    And Homa's service fee is <fee>
    And the customer is refunded <refund>
    Examples:
      | tier     | time            | earning | fee   | refund  |
      | FLEXIBLE | Saturday 09:00  | 0.00    | 0.00  | 230.00  |
      | STANDARD | Friday 09:59    | 0.00    | 0.00  | 230.00  |
      | STANDARD | Friday 10:00    | 18.00   | 2.70  | 209.30  |
      | FIRM     | Saturday 09:59  | 36.00   | 5.40  | 188.60  |

  Scenario: With travel included, the tier applies to the full provider price
    Given a RESERVED reservation with travel included, provider price RM210, total RM241.50, tier STANDARD
    When the customer cancels at Saturday 09:00
    Then the provider earns RM21.00
    And the customer is refunded RM217.35

  Scenario: The customer cannot cancel during the check-in period before check-in
    Given the reservation is RESERVED and the provider has not checked in
    When the customer cancels at Saturday 10:05
    Then it is rejected with CancellationLockedDuringCheckInPeriodException

  Scenario: Cancelling after check-in counts as a customer no-show
    Given the provider checked in at Saturday 09:58
    When the customer cancels at Saturday 10:20
    Then the provider earns RM74.00
    And the customer is refunded RM144.90

  Scenario: A cancellation fee is released straight away
    Given the reservation is RESERVED and the provider's tier is STANDARD
    When the customer cancels at Saturday 09:00
    Then the settlement is released immediately

Feature: Provider cancellation and fines
  Background:
    Given a RESERVED reservation for Saturday 10:00 with an hours fee of RM180, total RM230

  Scenario Outline: Provider fines depend on timing only
    When the provider cancels at <time>
    Then the customer is refunded RM230.00
    And the provider is fined <fine>
    Examples:
      | time           | fine   |
      | Friday 09:59   | 0.00   |
      | Friday 10:00   | 36.00  |
      | Saturday 09:59 | 36.00  |

  Scenario: Cancelling after the booked start but before check-in carries the no-show fine
    Given the provider has not checked in
    When the provider cancels at Saturday 10:05
    Then the customer is refunded RM230.00
    And the provider is fined RM54.00

  Scenario: Cancelling after check-in carries the no-show fine
    Given the provider checked in at Saturday 09:58
    When the provider cancels at Saturday 10:30
    Then the customer is refunded RM230.00
    And the provider is fined RM54.00

  Scenario: The fine does not depend on the provider's tier
    Given the provider's tier is FLEXIBLE
    When the provider cancels at Saturday 09:00
    Then the provider is fined RM36.00

  Scenario: With travel included, the fine applies to the full provider price
    Given a RESERVED reservation with travel included and provider price RM210
    When the provider cancels at Saturday 09:00
    Then the provider is fined RM42.00

Feature: Naming who is coming
  Background:
    Given a RESERVED reservation for 2 helpers starting Saturday 10:00
    And the provider's verified roster is Aina, Ben and Chan

  Scenario: The provider names the lead and helpers
    When the provider names Aina as lead with helpers Aina and Ben
    Then the team is named

  Scenario: The team must match the number of helpers booked
    When the provider names only Aina
    Then it is rejected with TeamSizeMismatchException

  Scenario: Helpers must be on the verified roster
    When the provider names Aina and Dina
    Then it is rejected with HelperNotOnRosterException

  Scenario: The lead must be one of the named helpers
    When the provider names Chan as lead with helpers Aina and Ben
    Then it is rejected with LeadNotInTeamException

  Scenario: The team cannot change from the booked start
    Given the team is named
    When the provider changes the team at Saturday 10:00
    Then it is rejected with TeamLockedException

Feature: Check-in
  Background:
    Given a RESERVED reservation starting Saturday 10:00 with the team named

  Scenario Outline: Check-in needs the device near the customer within the check-in period
    When the lead checks in at <time> from <distance> metres
    Then the result is <result>
    Examples:
      | time           | distance | result                          |
      | Saturday 09:45 | 120      | checked in                      |
      | Saturday 09:44 | 120      | CheckInTooEarlyException        |
      | Saturday 10:05 | 120      | checked in                      |
      | Saturday 10:15 | 300      | checked in                      |
      | Saturday 10:05 | 301      | TooFarToCheckInException        |
      | Saturday 10:16 | 50       | CheckInPeriodOverException      |

  Scenario: Check-in is locked until the team is named
    Given a RESERVED reservation with no team named
    When the lead checks in at Saturday 10:05 from 50 metres
    Then it is rejected with TeamNotNamedException

  Scenario: No check-in by the end of the check-in period (10:15) is a provider no-show
    Given the provider has not checked in
    When Homa marks a provider no-show at Saturday 10:16
    Then the reservation is CANCELLED
    And the customer is refunded RM230.00
    And the provider is fined RM54.00

  Scenario: A provider no-show cannot be marked early
    When Homa marks a provider no-show at Saturday 10:15
    Then it is rejected with CheckInPeriodNotOverException

Feature: Customer no-show
  Background:
    Given a RESERVED reservation starting Saturday 10:00 with total RM230, hours fee RM180, travel RM20
    And the lead checked in at Saturday 10:00

  Scenario: The provider reports a customer no-show after the wait
    When the provider reports a customer no-show at Saturday 10:15
    Then the reservation is CANCELLED
    And the provider earns RM74.00
    And Homa's service fee is RM11.10
    And the customer is refunded RM144.90
    And the earning is released at Saturday 11:15 unless challenged

  Scenario: A customer no-show cannot be reported before the wait ends
    When the provider reports a customer no-show at Saturday 10:14
    Then it is rejected with NoShowWaitNotOverException

  Scenario: A customer no-show needs a check-in
    Given a RESERVED reservation where the provider has not checked in
    When the provider reports a customer no-show
    Then it is rejected with NotCheckedInException

  Scenario: The customer challenges a no-show within the report window
    Given the provider reported a customer no-show at Saturday 10:15
    When the customer challenges it at Saturday 11:00
    Then the reservation is DISPUTED

  Scenario: Homa upholds the no-show and the reservation ends cancelled
    Given a customer no-show challenged by the customer
    When Homa decides to proceed with the cancellation
    Then the reservation is CANCELLED
    And the provider earns RM74.00
    And the customer is refunded RM144.90

  Scenario: Homa finds the no-show wrongful and refunds in full
    Given a customer no-show challenged by the customer
    When Homa decides on a full refund
    Then the reservation is CANCELLED
    And the customer is refunded RM230.00
    And the provider earns RM0.00

  Scenario: A challenge after the report window is rejected
    Given the provider reported a customer no-show at Saturday 10:15
    When the customer challenges it at Saturday 11:16
    Then it is rejected with ReportWindowClosedException

  Scenario: The no-show fee never exceeds the ceiling
    Then for every tier, the customer cancellation fee is at most the customer no-show fee

Feature: Completion
  Background:
    Given a RESERVED reservation for Saturday 10:00 to 13:00 and the lead checked in at 10:00

  Scenario: Completion needs photo or video evidence
    When the provider submits completion with no evidence
    Then it is rejected with EvidenceRequiredException

  Scenario: Completion needs a check-in first
    Given a RESERVED reservation with no check-in
    When the provider submits completion with a photo
    Then it is rejected with NotCheckedInException

  Scenario Outline: The completion reminder is due 30 minutes after the booked end
    Then the completion reminder is due at <time> is <due>
    Examples:
      | time           | due   |
      | Saturday 13:29 | false |
      | Saturday 13:30 | true  |

  Scenario: Missing the completion deadline sends it to Homa
    Given no completion has been submitted
    When Homa checks at Sunday 13:00
    Then the reservation is DISPUTED

  Scenario: The completion deadline cannot be missed early
    When Homa checks at Sunday 12:59
    Then it is rejected with CompletionDeadlineNotReachedException

Feature: Dispute window and payout
  Background:
    Given a RESERVED reservation for Saturday 10:00 to 13:00 with provider price RM200 and total RM230

  Scenario Outline: The window ends 1 hour after the later of the booked end and completion
    Given completion was submitted at <submitted>
    Then the dispute window ends at <ends>
    Examples:
      | submitted      | ends           |
      | Saturday 12:45 | Saturday 14:00 |
      | Saturday 13:40 | Saturday 14:40 |

  Scenario: The window has no end until completion is submitted
    Given no completion has been submitted
    Then the dispute window has no end yet

  Scenario: The window closes with no report and the payment is released
    Given completion was submitted at Saturday 12:45
    When Homa closes the dispute window at Saturday 14:00
    Then the reservation is COMPLETED
    And the provider earns RM200.00
    And Homa's service fee is RM30.00

  Scenario: The window cannot close early
    Given completion was submitted at Saturday 12:45
    When Homa closes the dispute window at Saturday 13:59
    Then it is rejected with DisputeWindowStillOpenException

Feature: Reports
  Background:
    Given a RESERVED reservation for Saturday 10:00 to 13:00 and completion submitted at 12:45

  Scenario: The customer files a report inside the window
    When the customer files a FINISHED_WORK report at Saturday 13:30 with a photo
    Then the reservation is DISPUTED

  Scenario: The provider can file a report too
    When the provider files a DURING_JOB report at Saturday 11:00 with a reason
    Then the reservation is DISPUTED

  Scenario: Each party files at most one report
    Given the customer filed a report
    When the customer files another report
    Then it is rejected with ReportAlreadyFiledException

  Scenario: Reports before the booked start are rejected
    When the customer files a report at Saturday 09:59
    Then it is rejected with ReportWindowClosedException

  Scenario: Reports after the window are rejected
    When the customer files a report at Saturday 14:01
    Then it is rejected with ReportWindowClosedException

Feature: Homa's decision
  Background:
    Given a DISPUTED reservation with provider price RM200 and total RM230

  Scenario: Release
    When Homa decides to release
    Then the reservation is COMPLETED
    And the provider earns RM200.00
    And the customer is refunded RM0.00

  Scenario: Partial refund
    When Homa decides the provider receives RM100.00
    Then the provider earns RM100.00
    And Homa's service fee is RM15.00
    And the customer is refunded RM115.00

  Scenario: Full refund
    When Homa decides on a full refund
    Then the customer is refunded RM230.00
    And the provider earns RM0.00

  Scenario: A partial refund cannot give the provider more than the provider price
    When Homa decides the provider receives RM201.00
    Then it is rejected with InvalidDecisionAmountException

Feature: Enquiries
  Scenario: An enquiry closes when its date passes without a request
    Given an enquiry for Saturday 10:00
    Then the enquiry is closed at Saturday 10:00
    And it is open at Saturday 09:59

  Scenario: An enquiry turns into one reservation only
    Given an enquiry that has been requested
    When the customer requests it again
    Then it is rejected with EnquiryAlreadyRequestedException

  Scenario: Book again copies the details with a new date
    Given a past enquiry for KITCHEN, 3 hours, 2 helpers
    When the customer books again for next Saturday 10:00
    Then a new enquiry has KITCHEN, 3 hours, 2 helpers, next Saturday 10:00
```


---

## payment

**Purpose:** carry out what `reservation` decided. Track each payment's states, release and refund the right amounts, pay out per reservation, and deduct fines.

| Object | Kind | Responsibility |
|---|---|---|
| `Payment` | Aggregate | One per reservation: method, amount, states `AUTHORISED` (card only), `HELD`, `RELEASED`, `REFUNDED`, with a record of each part |
| `PaymentMethod` | Enum | `CARD`, `FPX` |
| `Payout` | Aggregate | One per reservation that pays the provider: gross earning, fine deductions, net |
| `Fine` | Aggregate | Amount, reason, deducted so far, waived |
| `FineDeductionPolicy` | Domain service | Applies outstanding fines oldest first, at most 50% of a payout |

```java
Payment.authoriseCard(ReservationId r, Money total, Instant now)
Payment.payUpfrontByFpx(ReservationId r, Money total, Instant now)
Payment.capture(Instant now)                         // card, on accept
Payment.adjustTotal(Money newTotal) : Money           // edit while requested; returns the difference
Payment.cancelBeforeAcceptance() : PaymentInstruction // card: release authorisation; FPX: refund
Payment.settle(Settlement s) : List<PaymentInstruction>  // release and refund parts
Payment.isFullyAccounted() : boolean                 // released + refunded == charged

Fine.charge(ProviderId p, FineCharge charge, Instant now)
Fine.waive(String reason, Instant now)
Fine.outstanding() : Money

Payout.create(ProviderId p, ReservationId r, Money earning)
FineDeductionPolicy.apply(Payout payout, List<Fine> outstanding, Percentage cap) : Payout
```

```gherkin
Feature: Taking payment
  Scenario: A card is authorised, not charged, at request
    When the customer requests with a card for RM230.00
    Then the payment is AUTHORISED for RM230.00

  Scenario: FPX is paid upfront
    When the customer requests with FPX for RM230.00
    Then the payment is HELD for RM230.00

  Scenario: A card is charged when the provider accepts
    Given an AUTHORISED card payment of RM230.00
    When the payment is captured
    Then the payment is HELD for RM230.00

  Scenario: A declined card request releases the authorisation and refunds nothing
    Given an AUTHORISED card payment of RM230.00
    When the request ends before acceptance
    Then the instruction is to release the authorisation
    And no refund is made

  Scenario: A declined FPX request is refunded in full
    Given a HELD FPX payment of RM230.00 not yet accepted
    When the request ends before acceptance
    Then the instruction is to refund RM230.00

  Scenario: Editing an FPX request upward collects the difference
    Given a HELD FPX payment of RM230.00
    When the total changes to RM299.00
    Then the difference to collect is RM69.00

Feature: Settling
  Scenario: A settlement is fully accounted for
    Given a HELD payment of RM230.00
    When it is settled with provider earning RM18.00, service fee RM2.70 and refund RM209.30
    Then RM20.70 is released
    And RM209.30 is refunded
    And the payment is fully accounted for

  Scenario: A settlement that doesn't add up is rejected
    Given a HELD payment of RM230.00
    When it is settled with amounts adding up to RM229.00
    Then it is rejected with SettlementMismatchException

  Scenario: A payment cannot be settled twice
    Given a payment that was already settled
    When it is settled again
    Then it is rejected with PaymentAlreadySettledException

Feature: Fines and payouts
  Scenario: A fine is deducted at most 50% per payout
    Given the provider has an outstanding fine of RM54.00
    When a payout of RM80.00 is made
    Then RM40.00 is deducted
    And the net payout is RM40.00
    And RM14.00 of the fine is still outstanding

  Scenario: The rest of the fine comes off the next payout
    Given the provider has an outstanding fine of RM14.00
    When a payout of RM100.00 is made
    Then RM14.00 is deducted
    And the fine is cleared

  Scenario: Older fines are deducted first
    Given fines of RM10.00 from Monday and RM20.00 from Tuesday
    When a payout of RM30.00 is made
    Then RM10.00 comes off Monday's fine and RM5.00 off Tuesday's fine

  Scenario: A waived fine is not deducted
    Given an outstanding fine of RM36.00 that Homa waived
    When a payout of RM100.00 is made
    Then nothing is deducted

  Scenario: A partly deducted fine can be waived
    Given a fine of RM54.00 with RM40.00 already deducted
    When Homa waives it
    Then RM0.00 is outstanding
```


---

## resolution

**Purpose:** the founders' report queue. Each case collects the evidence and records one decision with a reason.

| Object | Kind | Responsibility |
|---|---|---|
| `Case` | Aggregate | Source, reservation, evidence, status `OPEN` or `DECIDED`, decision |
| `CaseSource` | Enum | `CUSTOMER_REPORT`, `PROVIDER_REPORT`, `NO_SHOW_CHALLENGE`, `MISSED_COMPLETION` |
| `Decision` | Value object | `release()`, `partialRefund(providerReceives)`, `fullRefund()`, plus a required reason |

```java
Case.open(ReservationId r, CaseSource source, List<EvidenceRef> evidence, Money providerPrice, Instant now)
Case.decide(Decision decision, String reason, Instant now)
Case.isOpen() : boolean
```

```gherkin
Feature: Deciding a case
  Scenario: A decision needs a reason
    Given an OPEN case
    When the founder decides to release with no reason
    Then it is rejected with DecisionReasonRequiredException

  Scenario: A case is decided once
    Given a DECIDED case
    When the founder decides again
    Then it is rejected with CaseAlreadyDecidedException

  Scenario: A partial refund must be within the provider price
    Given an OPEN case for a reservation with provider price RM200.00
    When the founder decides the provider receives RM250.00
    Then it is rejected with InvalidDecisionAmountException

  Scenario Outline: Every source can be decided with any outcome
    Given an OPEN case from <source>
    When the founder decides <decision> with reason "Checked the photos"
    Then the case is DECIDED
    Examples:
      | source            | decision        |
      | CUSTOMER_REPORT   | release         |
      | PROVIDER_REPORT   | full refund     |
      | NO_SHOW_CHALLENGE | full refund     |
      | MISSED_COMPLETION | partial refund  |
```


---

## conversation

**Purpose:** the chat rules. One chat per enquiry or reservation, started only by the customer.

| Object | Kind | Responsibility |
|---|---|---|
| `Conversation` | Aggregate | Belongs to one enquiry or reservation, the customer and the provider; messages; archived flag |
| `Message` | Entity | Sender, text, time |

```java
Conversation.startByCustomer(EnquiryId or ReservationId subject, CustomerId c, ProviderId p, Message first)
Conversation.post(Party sender, String text, Instant now)
Conversation.linkReservation(ReservationId r)   // when the enquiry is requested; same chat carries on
Conversation.archive(Instant now)               // enquiry closed without a request
```

```gherkin
Feature: Chat
  Scenario: Only the customer can start a chat
    When the provider tries to start a chat
    Then it is rejected with OnlyCustomerCanStartChatException

  Scenario: The provider can reply once the customer has started
    Given a chat the customer started
    When the provider posts "Yes, I can bring a steam mop"
    Then the chat has 2 messages

  Scenario: Requesting from an enquiry keeps the same chat
    Given a chat for enquiry E1
    When E1 becomes reservation R1
    Then the chat belongs to R1 and keeps its messages

  Scenario: An archived chat is readable but closed
    Given a chat for an enquiry that closed without a request
    When the chat is archived
    And the customer posts a message
    Then it is rejected with ConversationArchivedException

  Scenario: A reservation's chat stays open after the job
    Given a chat for a COMPLETED reservation
    When the customer posts "I think I left my keys in the kitchen"
    Then the message is added
```


---

## Not in the domain phase

These have no business rules of their own, or they belong to usecase or infra later:

- **Notifications:** what to send and when follows from the IEs above; there's no domain object.
- **Admin console tracking:** read models over the contexts above.
- **Privacy data requests:** a workflow for the usecase layer, no domain rules beyond recording each request.
- **Evidence storage and retention:** the 7-day retention is a scheduled infra job driven by `ReservationCompletedIE` and `CaseDecidedIE`.
- **Distance calculation:** the geoservice computes the distance; the domain only checks it against the radius.

## Integration events

The shared `ie/` module holds the contracts that bounded contexts use to talk to each other. It is built as **the last step of the domain phase**, after every domain context passes its gates, so the events are written from domain outputs that have stopped changing. Publishers, handlers and listeners come in the usecase and infra phases.

**Rules**

- `ie/` holds only event interfaces (`ie/port/`) and immutable event records (`ie/events/`). No behaviour.
- Events carry primitives, Strings, kernel types and JDK value types (`Instant`, `Duration`, immutable `List`/`Set`); never domain objects; no `Optional` fields. No context leaks its internals. When a value is not always there, split the event so every field is always present (preferred), or use a nullable field with an accessor that returns `Optional<T>`.
- Collections are copied defensively in the record's compact constructor (`List.copyOf`, `Set.copyOf`), so events stay immutable and reject nulls.
- `ie/` depends only on `kernel/`. No `domain` imports `ie/`.
- One event interface per publishing context, for example `ReservationIE`; each event implements it, for example `ReservationAcceptedIE implements ReservationIE`.
- One event per outcome, never one generic event with a status field: for example `ProviderVerifiedIE` and `ProviderVerificationRejectedIE`.
- Each event's fields come from the result of one domain method, listed below. If an event has no domain method behind it, a method is missing from the domain.

| Event | Published by | Listened to by | Carries the result of |
|---|---|---|---|
| `JobAddedIE` | catalogue | offering | `JobList.add` |
| `JobRetiredIE` | catalogue | offering | `JobList.retire` |
| `ToolAddedIE` | catalogue | offering | `ToolList.add` |
| `ToolRetiredIE` | catalogue | offering | `ToolList.retire` |
| `HomaPolicyChangedIE` | policy | reservation, discovery | `HomaPolicy.change` |
| `CustomerRegisteredIE` | account | conversation | `CustomerAccount.register` |
| `ProviderVerifiedIE` | account | offering | `ProviderAccount.recordEkycResult` with `VERIFIED` |
| `ProviderVerificationRejectedIE` | account | offering, (notifications) | `ProviderAccount.recordEkycResult` with `REJECTED` |
| `PayoutAccountReadyIE` | account | offering, payment | `ProviderAccount.recordPayoutAccountReady` |
| `HelperVerifiedIE` | account | offering | `Helper.recordEkycResult` with `VERIFIED` |
| `HelperVerificationRejectedIE` | account | offering, (notifications) | `Helper.recordEkycResult` with `REJECTED` |
| `HelperRemovedIE` | account | offering | removing a helper |
| `ListingWentLiveIE` | offering | discovery | `ProviderListing.goLive` |
| `ListingChangedIE` | offering | discovery | any `ProviderListing` setter on a live listing |
| `ListingWentOfflineIE` | offering | discovery | `ProviderListing.updateRoster` leaving a live listing with no verified team |
| `EnquiryCreatedIE` | reservation | conversation | `Enquiry.create`, `Enquiry.bookAgainFrom` |
| `EnquiryRequestedIE` | reservation | conversation | `Enquiry.markRequested` |
| `EnquiryClosedIE` | reservation | conversation | `Enquiry.isClosed` turning true |
| `ReservationRequestedIE` | reservation | payment | `Reservation.request` |
| `ReservationEditedIE` | reservation | payment | `Reservation.edit` (with the price change) |
| `ReservationAcceptedIE` | reservation | payment, availability | `Reservation.accept` |
| `ReservationDeclinedIE` | reservation | payment | `Reservation.decline` |
| `ReservationExpiredIE` | reservation | payment | `Reservation.expire` |
| `ReservationCancelledIE` | reservation | availability | any cancel or no-show |
| `TeamNamedIE` | reservation | (notifications) | `Reservation.nameTeam` |
| `ProviderCheckedInIE` | reservation | (notifications) | `Reservation.checkIn` |
| `CompletionSubmittedIE` | reservation | (notifications) | `Reservation.submitCompletion` |
| `ReportFiledIE` | reservation | resolution | `Reservation.fileReport` |
| `NoShowChallengedIE` | reservation | resolution | `Reservation.challengeNoShow` |
| `CompletionDeadlineMissedIE` | reservation | resolution | `Reservation.missCompletionDeadline` |
| `ReservationCompletedIE` | reservation | availability | `Reservation.closeDisputeWindow`, `Reservation.resolve` |
| `SettlementDecidedIE` | reservation | payment | every method returning a `Settlement` |
| `FineChargedIE` | reservation | payment | every method returning a `FineCharge` |
| `CaseDecidedIE` | resolution | reservation | `Case.decide`, with `Case.providerReceives` |
| `PaymentCapturedIE` | payment | (notifications) | `Payment.capture` |
| `PaymentRefundedIE` | payment | (notifications) | refund instructions from `Payment.settle`, `Payment.cancelBeforeAcceptance` |
| `PayoutCreatedIE` | payment | (notifications) | `FineDeductionPolicy.apply` |
| `FineWaivedIE` | payment | (notifications) | `Fine.waive` |

**Testing:** event records have no logic, so CRAP and mutation gates don't apply. One test per event checks it can only be built with all required fields and is immutable. The dependency-rule gate checks that `ie/` imports only `kernel/`.

## Build order

Build in this order so each context only needs what already exists. Within `reservation`, build slice by slice and pass all gates before moving on.

0. `kernel` (shared kernel: `Money`, `Percentage`)
1. `catalogue`
2. `policy`
3. `account`
4. `offering`
5. `availability`
6. `reservation`
   1. Pricing (`PriceBreakdown`)
   2. Request, edit, expiry, accept, decline (`Enquiry` too)
   3. Customer cancellation and `Settlement`
   4. Provider cancellation and `FineCharge`
   5. Team naming and check-in
   6. No-shows
   7. Completion, dispute window, reports and decisions
7. `payment`
8. `resolution`
9. `conversation`
10. `discovery`
11. `ie/` integration event contracts, once every context above passes its gates

## Testing approach

- **Gherkin is the spec, JUnit is the test.** Each feature above becomes a test class; each scenario a test method named after it; each Examples row a parameterised case. Cucumber is optional: if used, it runs with no Spring, matching the framework-free domain rule.
### Test naming convention

Tests are named in the **ubiquitous language** of the bounded context: they describe business behaviour the way a founder or provider would say it, never the method or class being called. A test name should still make sense if the code inside were rewritten.

**Test classes:** one per feature, named `<Aggregate or domain service><Behaviour>Test`, in the same package as the domain object.

| Feature | Test class |
|---|---|
| Customer cancellation | `ReservationCustomerCancellationTest` |
| Provider cancellation and fines | `ReservationProviderCancellationTest` |
| Check-in | `ReservationCheckInTest` |
| Fines and payouts | `FineDeductionPolicyTest` |
| Going live | `ProviderListingGoLiveTest` |

**Context groups:** a Gherkin `Background` or shared `Given` becomes a JUnit `@Nested` class named for the situation, such as `WhenReserved`, `WhenCheckedIn` or `OnTheStandardTier`.

**Test methods:** one per scenario, written as a camelCase sentence that states the business outcome:

- Pattern: `<whoOrWhat><DoesOrIs><Outcome>[When<Condition>]`
- Success: `customerPaysTenPercentOfHoursFeeWhenCancellingInsideFeeWindow`
- Rule violation: `customerCannotCancelDuringCheckInPeriodBeforeCheckIn`
- Boundary: `providerNoShowIsMarkedOnlyAfterCheckInPeriodEnds`
- Parameterised (Scenario Outline): the method states the rule, and each row is named with `@ParameterizedTest(name = ...)`, for example `"{0} cancelling at {1} pays {2}"`.

**Don't:**

- name tests after code: `testCancelByCustomer`, `cancelByCustomer_throws`, `shouldReturnSettlement`
- use technical words the business doesn't use: `null`, `flag`, `dto`, `state machine`
- start with `test` or `should`; the outcome comes first

**The same language everywhere:** Gherkin step wording, test method names, exception names and builder names use identical terms. For example, the rule "cancellation locked during check-in period" appears as the scenario title, the method `customerCannotCancelDuringCheckInPeriodBeforeCheckIn`, and `CancellationLockedDuringCheckInPeriodException`. Builders read as domain sentences: `aReservation().reservedFor(saturdayAt(10, 0)).onTier(STANDARD)`.

**Example:**

```java
class ReservationCustomerCancellationTest {

    @Nested
    class WhenReserved {

        @Test
        void customerPaysTenPercentOfHoursFeeWhenCancellingInsideFeeWindow() { ... }

        @Test
        void customerIsRefundedInFullWhenCancellingBeforeFeeWindowOpens() { ... }

        @Test
        void customerCannotCancelDuringCheckInPeriodBeforeCheckIn() { ... }
    }

    @Nested
    class WhenRequested {

        @Test
        void customerCancelsForFreeOnEveryTier() { ... }
    }
}
```

### Other testing rules

- **Boundaries are the point.** Every time and amount rule has a case on each side of its boundary (for example 10:15 vs 10:16, 300 m vs 301 m). This is what keeps the domain's mutation score at 90% or more.
- **One fixed clock per test.** Tests pass explicit `Instant`s; no sleeping, no system time.
- **Shared builders.** `aReservation().reserved().startingAt(...)`, `anOffering()...`, `aPolicy()...` in test-support files (Discipline §9).
- **Money invariants as tests.** For every ending, provider earning + service fee + customer refund = total; no cancellation fee above the no-show fee.
- **Gates per context (domain phase):** unit tests, CRAP below 6, mutation 90% or more for `kernel/` and `domain` (with the ratchet), dependency rule. The startup-budget gate applies later, at `app/`.

## Open questions

### Decided

- **Provider cancels after the booked start but before checking in:** 30% of the hours fee, the same as a no-show.
- **Challenged customer no-show:** the reservation moves to Disputed. After Homa reviews it, it ends **Cancelled**: either Homa proceeds with the cancellation (the no-show fee stands) or finds it wrongful (full refund).
- **Earliest check-in:** 15 minutes before the booked start.
- **Shared kernel:** domains may depend on `kernel/` (`Money`, `Percentage`). The backend module discipline is updated.
- **SMS code rules:** 6 digits, valid 5 minutes, a new code after 5 wrong attempts.
- **Who checks in:** not recorded or checked. Helpers have no accounts, so the domain cannot know who held the phone, and the provider is accountable either way. Check-in already requires the team to be named.
- **Availability at request and acceptance:** checked by the request and accept usecases, not by `Reservation`.
- **Report evidence:** optional. A reason is required; evidence is encouraged, since some problems (a helper not turning up) have nothing to photograph, and Homa weighs missing evidence when deciding.
- **Cancellation terms:** `Reservation.cancellationTerms()` gives what the review page shows, using the same base as the cancel rule.

### Still open

These came up while turning the spec into rules. Until answered, the plan uses the assumption shown.

1. **Where the customer no-show wait starts** (slice 6). *Assumption:* from the later of the booked start and the check-in time.
2. **Rounding** (slice 1). Percentages of RM amounts can produce half sen. *Assumption:* round half-up to the sen, and compute the customer refund last so the parts always add up to the total.
3. **Does editing a request reset its expiry?** (slice 2). *Assumption:* no, the expiry stays from the original request.
4. **What "near the chosen time" means for close matches** (discovery). *Assumption:* the provider is free for the same duration starting within 2 hours before or after.
5. **SMS code rules** (account). *Decided:* 6 digits, valid 5 minutes, 5 wrong attempts then a new code is needed.
6. **Is the lead one of the helpers?** (slice 5). *Assumption:* yes, the lead is one of the named helpers.
7. **Fine deductions: which payouts count?** The spec says "next successful reservations". *Assumption:* only payouts from Completed reservations, not cancellation or no-show fee payouts.
8. **Changes to an offering while a request is open.** *Assumption:* the request keeps the offering snapshot it was made with; the provider's change affects new requests only.
