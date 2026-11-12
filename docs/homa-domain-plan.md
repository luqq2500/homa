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
- feedback
- Not in the domain phase
- Integration events
- Build order
- Testing approach
- Decisions made while building
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
| `catalogue` | Homa's job list, tool list, launch cities and the homepage showcase | `JobList`, `ToolList`, `ServiceArea`, `Showcase` |
| `policy` | Homa-set values and their versions | `HomaPolicy` |
| `account` | Customer and business accounts, phone verification, team members (helpers and supervisors) and adding them, the KYC provider's results (KYB, KYC, re-verification, monitoring), payout account status | `CustomerAccount`, `ProviderAccount`, `MemberVerification`, `TeamMember`, `PhoneVerification` |
| `offering` | What a provider sells: profile, jobs, tools, pricing, hours, helpers range, cities, cancellation tier, going live | `ProviderListing` |
| `availability` | When a provider can work: availability windows, synced busy blocks, accepted reservations, buffer between jobs | `ProviderCalendar` |
| `discovery` | Matching a search to providers: exact and close matches | `SearchCriteria`, `MatchClassifier` |
| `reservation` | Enquiries and the reservation lifecycle: pricing, request, accept, cancel, team, check-in, no-shows, completion, reports, dispute window, settlements | `Enquiry`, `Reservation` |
| `payment` | Money movement: authorise, hold, release, refund, payouts, fines and fine deductions | `Payment`, `Payout`, `Fine` |
| `resolution` | Cases that need the founders' decision and their outcomes | `Case` |
| `conversation` | Chats: one per enquiry or reservation, who may write, archiving | `Conversation` |
| `feedback` | Customers' ratings of providers and helpers, and the averages shown | `Rating`, `RatingSummary` |

All eleven depend on the shared kernel `kernel/` (`Money`, `Percentage`), which is built first.

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
reservation ──(ReservationRequested, TeamNamed, ReservationCompleted)──► feedback
policy ─────(rating window, minimum ratings to show)──► feedback
```

---

## catalogue

**Purpose:** the reference lists Homa sets, which providers pick from and search matches on.

| Object | Kind | Responsibility |
|---|---|---|
| `Job` | Entity | Code, name, group (such as Kitchen), active flag; the name and group can change, the code never |
| `JobGroup` | Value object | Display group for scanning |
| `JobList` | Aggregate | Holds jobs; codes are unique; jobs are retired, never deleted |
| `Tool` / `ToolList` | Entity / Aggregate | Same pattern as jobs |
| `ServiceArea` | Aggregate | Launch cities: Salak Tinggi, Cyberjaya, Putrajaya |
| `FeaturedSearch` | Entity | A search Homa features on the homepage: title, preset search (jobs, duration, helpers), and whether it is shown |
| `Showcase` | Aggregate | The featured searches in order: positions from 1, with no gaps and no two in one place |

```java
JobList.add(JobCode code, String name, JobGroup group)
JobList.rename(JobCode code, String name) : boolean    // whether the name changed
JobList.regroup(JobCode code, JobGroup group) : boolean // whether the group changed
JobList.retire(JobCode code)
JobList.isOffered(JobCode code) : boolean
ServiceArea.covers(City city) : boolean
Showcase.feature(String title, PresetSearch preset, int position, Set<JobCode> offeredJobs) : FeaturedSearch
                                       // only jobs providers can offer; from 1 to one past the last; the rest move down
Showcase.rename(String featuredSearchId, String title) : boolean   // whether the title changed
Showcase.move(String featuredSearchId, int position) : boolean     // whether it moved
Showcase.remove(String featuredSearchId)                           // the rest move up
Showcase.withdrawJob(JobCode retired) : boolean   // one left with no job is no longer shown
FeaturedSearch.isShown() : boolean
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

Feature: Featured searches
  Scenario: A featured search needs a preset search with at least one job
    When Homa features "Weekend quick refresh" with no jobs
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

Values held (26): service fee 15%; duration 1–6 h; minimum notice 2 h; booking horizon 30 days; team reminder 6 h before the booked start; earliest check-in 15 min before the booked start; check-in period 15 min; check-in radius 300 m; customer no-show wait 15 min; customer no-show fee travel + 30%; no-show report window 1 h; request expiry 24 h or the response deadline; response deadline 1 h before the booked start; completion reminder 30 min; completion deadline 24 h; evidence retention 120 days; dispute window extension 24 h; fee window 24 h; tiers 0% / 10% / 20%; provider late cancellation fine 20%; provider no-show fine 30%; fine deduction cap 50%; rating window 14 days; minimum ratings to show 3. Homa sets no limit on helpers; each provider's maximum is the helpers on its team. The verification rules (documents, re-verification, face check attempts) are the KYC provider's, not Homa-set values.

```java
HomaPolicy.initial(Instant now) : HomaPolicy    // the values above, version 1, in force from now
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

**Purpose:** who someone is and whether they are verified. Customers verify by phone. Businesses and their team members are verified by a KYC provider, and account records the results (see "business providers only, verification by a KYC provider"). Businesses also need a ready Stripe payout account.

| Object | Kind | Responsibility |
|---|---|---|
| `CustomerAccount` | Aggregate | Verified phone, name (providers see it), address with city |
| `PhoneVerification` | Aggregate | SMS code, expiry, attempts |
| `ProviderAccount` | Aggregate | A business: its verification status, the SSM registration from KYB, the verified-until and last verification dates, and payout account status |
| `MemberVerification` | Aggregate | Adding a team member before they exist: the add form, with the lawful-employment confirmation, and the KYC outcome |
| `TeamMember` | Aggregate | Belongs to one business: role, registered name, photo, team status, and the verified-until and last verification dates. Only a `MEMBER` is on the team |
| `TeamStatus` | Enum | `MEMBER` (verified), `LAPSED` (the KYC provider reports a missed re-verification), `REMOVED` |
| `KycResult`, `KybResult` | Value object | A result from the KYC provider: verified, with the registered name or SSM registration and the verified-until date, or rejected |
| `VerificationOutcome` | Enum | What a result did: `VERIFIED`, `REJECTED`, `REVERIFIED`, `RECHECK_FAILED` or `UNCHANGED` |
| `VerificationStatus` | Enum | `PENDING`, `VERIFIED`, `REJECTED`, `LAPSED` (a business's state; never a result) |
| `Address` | Value object | Street text, city, coordinates (from the geoservice) |

```java
PhoneVerification.start(PhoneNumber phone, String code, Instant now)
PhoneVerification.confirm(String code, Instant now) : boolean
CustomerAccount.register(PhoneVerification verified, String name, Address address)
CustomerAccount.changeAddress(Address address)
CustomerAccount.rename(String name) : boolean     // whether it changed; see "aligning the product spec, domain plan and code"

ProviderAccount.register(PhoneVerification verified)
ProviderAccount.recordKybResult(KybResult result, Instant now) : VerificationOutcome
ProviderAccount.lapse()                          // the KYC provider reports a missed re-verification
ProviderAccount.requireReverification(String reason, Instant deadline)   // monitoring: verified-until moves sooner
ProviderAccount.recordSeriousFlag(String reason)                         // monitoring: lapses at once
ProviderAccount.recordPayoutAccountReady() : boolean   // whether it changed: a repeated report changes nothing
ProviderAccount.isReadyForGoLive() : boolean    // verified, not lapsed, payout ready

MemberVerification.start(ProviderAccount business, TeamRole role, Photo photo, Instant now)
    // only for a verified business that hasn't lapsed; the photo is taken live; see "adding a team member, photo and confirmation"
MemberVerification.recordKycResult(KycResult result, PhotoMatch photo, Instant now) : VerificationOutcome
    // REJECTED for a rejected check, a person under 18, or a profile photo that isn't the verified person
MemberVerification.confirmEmployment(EmploymentConfirmation confirmation)   // after VERIFIED, for the named person
TeamMember.add(MemberVerification verified, Instant now)     // needs the confirmation
TeamMember.changeRole(TeamRole role) : boolean               // no new verification
TeamMember.recordKycResult(KycResult result, Instant now) : VerificationOutcome   // re-verification
TeamMember.lapse()                                           // the KYC provider reports a missed re-verification or a serious flag
TeamMember.requireReverification(String reason, Instant deadline)       // monitoring, such as an expiring document
TeamMember.remove()                                          // removes a member -> REMOVED
TeamMember.isTeamMember() : boolean                          // true only for MEMBER
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

> **Superseded:** verification is now the business's KYB by the KYC provider, not eKYC ("business providers only, verification by a KYC provider"). A business is ready when its KYB has passed, it has not lapsed, and its payout account is ready. The domain audit maps these scenarios to the tests of today's rule.

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

> **Superseded:** there are no invites. The provider fills in one add form (role, photo, lawful-employment confirmation) and the person verifies on the spot with the KYC provider; they are on the team only once verified ("adding a team member" and "verification on the spot"). The domain audit maps these scenarios to the tests of today's rule.

Feature: Team membership
  Scenario: An invited helper is not yet a team member
    When the provider invites "Aina" with a photo
    Then Aina is INVITED
    And Aina is not a team member

  Scenario: A verified invite joins the team
    Given Aina is INVITED
    When Aina's eKYC is VERIFIED
    Then Aina is a MEMBER
    And Aina is a team member

  Scenario: A rejected invite stays off the team and can retry
    Given Aina is INVITED
    When Aina's eKYC is REJECTED
    Then Aina is INVITE_REJECTED and not a team member
    When Aina's eKYC is later VERIFIED
    Then Aina is a MEMBER

  Scenario Outline: The provider can withdraw an invite or remove a member
    Given Aina is <status>
    When the provider removes Aina
    Then Aina is REMOVED and not a team member
    Examples:
      | status          |
      | INVITED         |
      | INVITE_REJECTED |
      | MEMBER          |

  Scenario: A removed helper cannot be verified back onto the team
    Given Aina is REMOVED
    When Aina's eKYC is VERIFIED
    Then it is rejected with HelperRemovedException

  Scenario: An invite needs a name and a photo
    When the provider invites a helper with no photo
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
| `HelpersRange` | Value object | Min and max helpers, at least 1, at most the helpers on the team; no Homa upper limit |
| `TravelFee` | Value object | `separate(amount)` or `included()` |
| `TeamRoster` | Value object | Helper ids (plus the provider if they are a helper) and supervisor ids. Helpers cap the helper range; both are who can be named. Only team members are on it |
| `SupervisorRule` | Value object | When a supervisor is sent: from a minimum number of helpers, of hours, or both |
| `OfferingSnapshot` | Value object | What `reservation` copies at request time: rate, ranges, travel fee, jobs, cities, tier, roster, supervisor rule, whether it takes requests |

```java
ProviderListing.draft(String providerId)
ProviderListing.setProfile(Profile profile)
ProviderListing.offerJobs(Set<JobCode> jobs, Set<JobCode> activeCatalogueJobs)
ProviderListing.bringTools(Set<ToolCode> tools, Set<ToolCode> activeCatalogueTools)
ProviderListing.setPricing(HourlyRate rate, TravelFee travelFee)
ProviderListing.setHours(HoursRange hours, HomaLimits limits)       // offering's own view of the Homa-set ranges
ProviderListing.setHelpers(HelpersRange helpers)                // at most the helpers on the team
ProviderListing.setSupervisorRule(SupervisorRule rule)          // needs a supervisor on the team
ProviderListing.clearSupervisorRule()
ProviderListing.coverCities(Set<City> cities, ServiceAreaView area)
ProviderListing.chooseCancellationTier(CancellationTier tier)
ProviderListing.updateRoster(TeamRoster roster)    // shrinks the helper range with the helpers; no helper, or no supervisor for a rule, takes a live listing offline
ProviderListing.withdrawJob(JobCode retired) : JobWithdrawal   // NOT_OFFERED, WITHDRAWN, or WITHDRAWN_AND_OFFLINE for a live listing's last job
ProviderListing.withdrawTool(ToolCode retired) : boolean       // whether the provider brought it; the listing stays live
ProviderListing.goLive(boolean providerReady)                 // the roster must cover the minimum helpers
ProviderListing.missingForGoLive() : Set<GoLiveRequirement>
ProviderListing.snapshot() : OfferingSnapshot
ProviderListing.fitHours(HomaLimits limits) : HoursFit   // Homa narrowed its duration range; see "aligning the product spec, domain plan and code"
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
    Given the provider has 3 team members
    When the provider sets helpers from 1 to 4
    Then it is rejected with HelpersExceedTeamSizeException

  # Superseded: Homa sets no upper limit on helpers; the maximum is the helpers on the team.
  Scenario: Max helpers cannot exceed 10
    Given the provider has 12 team members
    When the provider sets helpers from 1 to 11
    Then it is rejected with HelpersOutOfRangeException

  Scenario: The helper range shrinks when the team shrinks
    Given a live provider with helpers from 1 to 4 and 4 team members
    When one helper is removed from the roster
    Then the helper range is 1 to 3

  Scenario: Cities must be launch cities
    When the provider covers "Kajang"
    Then it is rejected with CityNotInServiceAreaException

Feature: Going live
  Scenario: A complete, verified provider goes live
    Given a provider listing with a profile, at least one job, pricing, hours, helpers, cities and a tier
    And the provider is verified with a ready payout account
    When the provider goes live
    Then the listing is live

  Scenario: Missing requirements block go-live and are listed
    Given a provider listing with no profile picture and no cities
    When the provider goes live
    Then it is rejected with ListingNotReadyException
    And the missing requirements are "profile picture" and "cities"

  # Superseded: there are no invites; someone still waiting for verification does not block go-live.
  Scenario: A pending invite does not block go-live
    Given a complete listing and a verified provider
    And a team of 2 members with minimum helpers 1
    And one more helper still INVITED
    When the provider goes live
    Then the listing is live
    And the team size is 2
```


---

## availability

**Purpose:** answers "is this provider free for this slot?" from the provider's own windows, synced calendar busy blocks and accepted reservations.

| Object | Kind | Responsibility |
|---|---|---|
| `ProviderCalendar` | Aggregate | Availability windows, busy blocks, reservation holds, buffer between jobs |
| `TimeSlot` | Value object | Start and end; start on a 15-minute step |
| `AvailabilityWindow` | Value object | When the provider says they can work |
| `BusyBlock` | Value object | From Google or Apple Calendar sync |
| `ReservationHold` | Value object | An accepted reservation's slot |

```java
ProviderCalendar.setWindows(List<AvailabilityWindow> windows)
ProviderCalendar.replaceBusyBlocks(List<BusyBlock> blocks)    // each sync replaces the last
ProviderCalendar.setBufferBetweenJobs(Duration buffer)        // kept free before and after each held slot
ProviderCalendar.hold(String reservationId, TimeSlot slot)
ProviderCalendar.release(String reservationId)
ProviderCalendar.isAvailable(TimeSlot slot) : boolean
ProviderCalendar.nearestAvailableStart(TimeSlot wanted, Duration within, Instant earliest, Instant latest) : Optional<Instant>
    // only starts the customer can book; see "aligning the product spec, domain plan and code"
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

Feature: Buffer between jobs
  Background:
    Given the provider is available Saturday 08:00 to 18:00
    And reservation R1 holds Saturday 10:00 to 13:00
    And the provider keeps a buffer of 30 minutes between jobs

  Scenario: A job cannot start within the buffer after another job
    Then Saturday 13:00 to 14:00 is not available
    And Saturday 13:30 to 14:30 is available

  Scenario: A job cannot end within the buffer before another job
    Then Saturday 08:30 to 09:45 is not available
    And Saturday 08:30 to 09:30 is available

  Scenario: Holding a slot inside the buffer is rejected
    When reservation R2 tries to hold Saturday 13:15 to 15:15
    Then it is rejected with SlotNotAvailableException

  Scenario: The buffer is kept around jobs, not around synced busy blocks
    Given a synced busy block Saturday 14:00 to 15:00
    Then Saturday 15:00 to 16:00 is available

  Scenario: The buffer cannot be negative
    When the provider sets a buffer of minus 15 minutes
    Then it is rejected with NegativeBufferBetweenJobsException
```


---

## discovery

**Purpose:** classify each provider for a search as an exact match, a close match or no match.

| Object | Kind | Responsibility |
|---|---|---|
| `SearchCriteria` | Value object | Jobs, start, duration, helpers, customer city; validated against Homa's ranges |
| `SearchLimits` | Value object | Discovery's own view of the Homa-set values a search respects |
| `ProviderCandidate` | Value object | Read model: jobs, hours range, helpers range, cities, `nearestFreeStart`, the nearest free start within 2 hours either side, from availability, and `verifiedUntil`, the provider's re-verification due date |
| `MatchClassifier` | Domain service | Returns `EXACT`, `CLOSE` or `NONE` with the reason |

**Exact match:** covers the city, offers every chosen job, the helpers and duration are within its ranges, and it's free at the chosen time.
**Close match:** covers the city and fits helpers and duration, but differs in one way only: it offers only some of the jobs, or it is free near the chosen time, never both. A start it suggests is one the customer can book (see "aligning the product spec, domain plan and code").
**No match:** doesn't cover the city, helpers or duration outside its ranges, offers none of the jobs, or not free near the time.

```java
SearchCriteria.of(Set<JobCode> jobs, Instant start, int hours, int helpers, City city, Instant now, SearchLimits limits)
MatchClassifier.classify(SearchCriteria criteria, ProviderCandidate candidate) : MatchResult
```

```gherkin
> **Superseded:** in part: Homa sets no upper limit on helpers, so a search needs at least one helper and has no maximum. The domain audit maps these scenarios to the tests of today's rule.

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
| `ReservationDetails` | Value object | Jobs, start, hours, helpers, address. The payment method is passed to `request` separately, since an enquiry has the same details but no payment |
| `PriceBreakdown` | Value object | Hours fee, travel fee, provider price, service fee, total |
| `ReservationStatus` | Enum | `REQUESTED`, `RESERVED`, `DECLINED`, `EXPIRED`, `CANCELLED`, `DISPUTED`, `COMPLETED` |
| `TeamAssignment` | Value object | Helpers and any supervisors named for this reservation |
| `CheckIn` | Value object | Time checked in, distance from the customer |
| `Completion` | Value object | Time submitted and evidence references |
| `Report` | Entity | Filed by the customer or the provider: type, reason, evidence |
| `Ending` | Enum | Why it ended: `CUSTOMER_CANCELLED_FREE`, `CUSTOMER_CANCELLED_IN_FEE_WINDOW`, `CUSTOMER_NO_SHOW`, `PROVIDER_CANCELLED`, `PROVIDER_NO_SHOW`, `COMPLETED`, `DECIDED_BY_HOMA`, and so on |
| `Settlement` | Value object | Who gets what: customer refund, provider earning, Homa service fee, when it's released |
| `FineCharge` | Value object | Provider fine amount and reason |
| `CaseDecision` | Value object | The founders' decision, received from `resolution` |
| `CancellationTerms` | Value object | What the review page shows: free until, the fee from then, the locked period, the cost after check-in |

### Lifecycle

```text
REQUESTED ─┬─accept──────────────────► RESERVED
           ├─decline─────────────────► DECLINED
           ├─expire──────────────────► EXPIRED
           └─cancel / payment failed─► CANCELLED

RESERVED ─┬─dispute window closes, no report──────────────────────────────► COMPLETED
          ├─report filed / completion deadline missed──► DISPUTED ──decision──► COMPLETED
          ├─customer or provider cancels, provider no-show──────────────────► CANCELLED
          ├─customer no-show──► CANCELLED ──customer challenges──► DISPUTED ──decision──► CANCELLED
          └─business seriously flagged──► DISPUTED ─┬─cancelled, or undecided at the start──► CANCELLED
                                                    └─flag cleared, before the start──────► RESERVED (restored)
```

### Methods

```java
// Enquiry
Enquiry.create(String customerId, String providerId, ReservationDetails details, Instant now)
Enquiry.bookAgain(String customerId, String providerId, ReservationDetails past, Address currentAddress, Instant newStart, Instant now)
    // from a past enquiry or reservation; replaces bookAgainFrom; see "aligning the product spec, domain plan and code"
Enquiry.isClosed(Instant now) : boolean
Enquiry.markRequested(String reservationId)   // behind EnquiryRequestedIE

// Request
Reservation.request(String customerId, ReservationDetails d, OfferingSnapshot o, PolicySnapshot p,
                    Optional<String> enquiryId, PaymentMethod method, Instant providerVerifiedUntil, Instant now)
Reservation.price() : PriceBreakdown
Reservation.requestExpiresAt() : Instant
Reservation.edit(ReservationDetails d, Instant providerVerifiedUntil, Instant now) : PriceChange   // the address can't change
Reservation.confirmPayment(Money secured)                             // the amount authorised or paid so far
Reservation.isPaymentSecured() : boolean                              // it covers the total
Reservation.paymentFailed()                                           // ends the request: Cancelled, PAYMENT_FAILED
Reservation.accept(Instant providerVerifiedUntil, Instant now)        // only once the payment is secured
Reservation.decline(Instant now)
Reservation.declineForTakenTime(Instant now)                          // another accepted booking took the time
Reservation.expire(Instant now)

// Cancelling
Reservation.cancellationTerms() : CancellationTerms   // free until, fee from then, locked period, after check-in
Reservation.cancelByCustomer(Instant now) : Settlement
Reservation.cancelByProvider(Instant now) : CancellationResult   // Settlement + FineCharge

// Job day
Reservation.nameTeam(TeamAssignment team, TeamRoster roster, Instant now)
Reservation.dropFromTeam(String memberId, Instant now) : TeamDrop     // a named person lapsed or was removed
Reservation.isTeamComplete() : boolean
Reservation.checkIn(int distanceMetres, Instant now)                 // the location check; then face checks (see "face check-in")
Reservation.recordFaceCheck(String memberId, FaceCheckResult result, Instant now)
Reservation.recordFaceCheckUnavailable(Instant now)
Reservation.markProviderNoShow(Instant now) : CancellationResult
Reservation.disputeForProviderFlag(Instant now)                       // the provider's business was seriously flagged; also takes over a job dispute without completion
Reservation.cancelForProviderFlag(Instant now) : Settlement           // full refund, no fine
Reservation.restoreAfterFlagCleared(Instant now)                      // before the booked start, once the flag is cleared
Reservation.isFlagCancellationDue(Instant now) : boolean              // undecided at the booked start
Reservation.reportCustomerNoShow(Instant now) : Settlement           // up to and including the booked end
Reservation.challengeNoShow(Instant now)
Reservation.submitCompletion(List<EvidenceRef> evidence, Instant now)  // also while disputed over the job

// After the job
Reservation.disputeWindowEndsAt() : Optional<Instant>
Reservation.fileReport(Party party, ReportType type, String reason, List<EvidenceRef> evidence, Instant now)
Reservation.missCompletionDeadline(Instant now)
Reservation.closeDisputeWindow(Instant now) : Settlement
Reservation.resolve(CaseDecision decision, Instant now) : Settlement   // ends COMPLETED, or CANCELLED for a challenged no-show

// Reminders (queries for the scheduler)
Reservation.isTeamReminderDue(Instant now) : boolean                 // also while the team is incomplete
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

# Changed in "the 7-star experience": a request expires by the response deadline, 1 hour before the
# booked start, not at the start. Saturday 06:00 for Saturday 10:00 now expires at Saturday 09:00.
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

> **Superseded:** there is no lead. The provider names the helpers, and a supervisor when the booking reaches the supervisor rule, all from the team ("provider types, team roles, documents and face check-in"). The domain audit maps these scenarios to the tests of today's rule.

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

> **Superseded:** in part: check-in is done on the provider app, not by a lead, and after the location check every named person takes a face check with the KYC provider ("face check-in"). The domain audit maps these scenarios to the tests of today's rule.

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
| `Payment` | Aggregate | One per reservation: method, amount, states `AUTHORISED` (card only), `AUTHORISATION_RELEASED` (card only: ended before acceptance, nothing charged), `HELD`, `RELEASED`, `REFUNDED`, with a record of each part |
| `PaymentMethod` | Enum | `CARD`, `FPX` |
| `Payout` | Aggregate | One per reservation that pays the provider: gross earning, fine deductions, net |
| `Fine` | Aggregate | Amount, reason, deducted so far, waived |
| `FineDeductionPolicy` | Domain service | Applies outstanding fines oldest first, at most 50% of a payout |

```java
Payment.authoriseCard(String reservationId, Money total, Instant now)
Payment.payUpfrontByFpx(String reservationId, Money total, Instant now)
Payment.capture(Instant now)                         // on accept: a card is charged; FPX, already held, is kept for the booking
Payment.adjustTotal(Money newTotal) : Money           // edit while requested; returns the difference
Payment.cancelBeforeAcceptance(Instant now) : PaymentInstruction // card: release authorisation (AUTHORISATION_RELEASED); FPX: refund
Payment.settle(Settlement s, Instant now) : List<PaymentInstruction>  // refund now, release from the settlement's release time; once only
Payment.isFullyAccounted() : boolean                 // released + refunded == charged

Fine.charge(String providerId, FineCharge charge, Instant now)
Fine.waive(String reason, Instant now)
Fine.outstanding() : Money

Payout.create(String providerId, String reservationId, Money earning, PayoutKind kind)   // fines come off COMPLETED_JOB payouts only
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
    And the payment is AUTHORISATION_RELEASED

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
| `Case` | Aggregate | Source, reservation, evidence, whether it is about safety, status `OPEN`, `DECIDED` or `CLOSED` (taken over by a flag), decision |
| `CaseSource` | Enum | `CUSTOMER_REPORT`, `PROVIDER_REPORT`, `NO_SHOW_CHALLENGE`, `MISSED_COMPLETION` |
| `Decision` | Value object | `release()`, `partialRefund(providerReceives)`, `fullRefund()`, plus a required reason |

```java
Case.open(String reservationId, CaseSource source, List<EvidenceRef> evidence, Money atStake, Instant now)
    // atStake: the provider price for a job, the customer no-show fee for a challenged no-show
Case.addEvidence(List<EvidenceRef> more)          // while open, for example when the other party also reports
Case.decide(Decision decision, String reason, Instant now)
Case.providerReceives() : Optional<Money>         // behind CaseDecidedIE
Case.isOpen() : boolean
Case.markSafety()                                 // a safety, damage or loss report opened it or joined it
Case.closeForProviderFlag(Instant now)            // a flag took the reservation over; no decision
Case.QUEUE_ORDER                                  // safety cases first, then oldest first
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
Conversation.start(Party startedBy, ChatSubject subject, String customerId, String providerId, String firstMessage, Instant now)
    // a provider start is rejected; startByCustomer(...) is the usual entry point
Conversation.startByCustomer(ChatSubject subject, String customerId, String providerId, String firstMessage, Instant now)
Conversation.post(Party sender, String text, Instant now)
Conversation.linkReservation(String reservationId)   // when the enquiry is requested; same chat carries on; once
Conversation.archive(Instant now)               // enquiry closed without a request
Conversation.openForReservation(String reservationId, String customerId, String providerId, Instant now)
    // a direct request opens the chat with no message; both can write
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

## feedback

**Purpose:** customers' ratings of a provider and of each helper who came, and the averages shown on the provider page and on "who is coming". Called **rating** and **feedback** in code, never "review", because "review" already means the review page and Homa reviewing reports.

| Object | Kind | Responsibility |
|---|---|---|
| `Rating` | Aggregate | One per Completed reservation: the provider rating, helper ratings, when it was submitted, hidden flag and reason |
| `RateableReservation` | Value object | feedback's own snapshot: reservation id, status, customer, provider, named helpers, when it became Completed, and whether it has already been rated |
| `Stars` | Value object | A whole number from 1 to 5 |
| `ProviderRating` | Value object | Overall `Stars` (required), `Stars` per `ProviderCategory` (optional, any number), written feedback (optional, up to 1,000 characters) |
| `ProviderCategory` | Enum | `QUALITY_OF_WORK`, `PUNCTUALITY`, `COMMUNICATION`, `CARE_FOR_THE_HOME` |
| `HelperRating` | Value object | Overall `Stars` (required) and `Stars` per `HelperCategory` (optional) for one helper; no text |
| `HelperCategory` | Enum | `FRIENDLINESS`, `HARD_WORK`, `CARE_WITH_BELONGINGS`, `COOPERATION`, `THOROUGHNESS` |
| `RatingSummary` | Aggregate | One per provider and one per helper: overall count and average, and a count and average per category |
| `FeedbackLimits` | Value object | feedback's view of the Homa-set values: rating window, minimum ratings to show |

```java
ProviderRating.of(Stars overall)                                    // overall stars only: no category stars, no text
ProviderRating.of(Stars overall, Map<ProviderCategory, Stars> categories, String writtenFeedback)   // categories and text optional
HelperRating.of(Stars overall)                                      // overall stars only
HelperRating.of(Stars overall, Map<HelperCategory, Stars> categories) // categories optional

Rating.submit(RateableReservation r, String customerId, ProviderRating provider,
              Map<String, HelperRating> helpers, FeedbackLimits limits, Instant now)
Rating.hide(String reason, Instant now)
Rating.isHidden() : boolean

RateableReservation.completed(String reservationId, String customerId, String providerId, Set<String> namedHelperIds, Instant completedAt)
RateableReservation.notCompleted(String reservationId, ReservationStatus status, String customerId, String providerId, Set<String> namedHelperIds)
RateableReservation.rated() : RateableReservation   // the same reservation, already rated

RatingSummary.forProvider(String providerId)
RatingSummary.forHelper(String helperId)
RatingSummary.record(Stars overall, Map<String, Stars> categories)    // category names of the provider or helper
RatingSummary.withdraw(Stars overall, Map<String, Stars> categories)
RatingSummary.overallAverage() : BigDecimal                          // one decimal place, half-up
RatingSummary.categoryAverage(String category) : BigDecimal
RatingSummary.isOverallShown(FeedbackLimits limits) : boolean
RatingSummary.isCategoryShown(String category, FeedbackLimits limits) : boolean
RatingSummary.categoryCount(String category) : int     // how many ratings rated this category
RatingSummary.ratingCount() : int
```

```gherkin
Feature: Rating a completed reservation
  Background:
    Given a reservation that became Completed on Saturday 14:00
    And the customer is C1, the provider is P1, and the named helpers are Aina and Ben

  Scenario: The customer rates the provider
    When C1 gives P1 4 stars on Sunday 10:00
    Then the rating is recorded

  Scenario: Only the reservation's customer can rate it
    When C2 rates the reservation
    Then it is rejected with NotTheReservationCustomerException

  Scenario: A reservation is rated once
    Given C1 has rated the reservation
    When C1 rates it again
    Then it is rejected with AlreadyRatedException

  Scenario Outline: Ratings are accepted only within the rating window
    When C1 rates the reservation at <time>
    Then the result is <result>
    Examples:
      | time                         | result                      |
      | 14 days after, at 14:00      | recorded                    |
      | 14 days after, at 14:01      | RatingWindowClosedException |

  Scenario Outline: Stars are whole numbers from 1 to 5
    When C1 gives P1 <stars> stars
    Then the result is <result>
    Examples:
      | stars | result                   |
      | 1     | recorded                 |
      | 5     | recorded                 |
      | 0     | StarsOutOfRangeException |
      | 6     | StarsOutOfRangeException |

  Scenario: Written feedback is optional
    When C1 gives P1 5 stars overall and 5 stars for PUNCTUALITY, with no written feedback
    Then the rating is recorded

  Scenario: Provider category stars are optional
    When C1 gives P1 4 stars overall and no category stars
    Then the rating is recorded

  Scenario: Any number of provider categories can be rated
    When C1 gives P1 4 stars overall, 5 for QUALITY_OF_WORK, 3 for PUNCTUALITY, 4 for COMMUNICATION and 5 for CARE_FOR_THE_HOME, with written feedback
    Then the rating has 4 stars overall and 4 category ratings

  Scenario: The provider rating needs overall stars
    When C1 gives P1 stars for QUALITY_OF_WORK only, with no overall stars
    Then it is rejected with OverallStarsRequiredException

  Scenario: Written feedback has a length limit
    When C1 writes 1,001 characters of feedback
    Then it is rejected with FeedbackTooLongException

  Scenario: Only helpers who were named can be rated
    When C1 rates helper Chan
    Then it is rejected with HelperNotOnReservationException

  Scenario: Helper ratings are optional
    When C1 rates P1 and no helpers
    Then the rating is recorded

  Scenario: A helper can receive overall and category stars
    When C1 gives Aina 5 stars overall, 5 for FRIENDLINESS and 4 for THOROUGHNESS
    Then Aina's rating has 5 stars overall and 2 category ratings

  Scenario: A rated helper needs overall stars
    When C1 gives Aina 5 stars for FRIENDLINESS only
    Then it is rejected with OverallStarsRequiredException

  Scenario: Helper category stars are optional
    When C1 gives Aina 4 stars overall and no category stars
    Then the rating is recorded

Feature: What can be rated
  Scenario Outline: Only Completed reservations can be rated
    Given a reservation that is <status>
    When the customer rates it
    Then it is rejected with ReservationNotRateableException
    Examples:
      | status    |
      | CANCELLED |
      | DISPUTED  |
      | RESERVED  |

Feature: Hiding a rating
  Scenario: Homa hides a rating with a reason
    Given a recorded rating
    When Homa hides it with the reason "Personal phone number in the text"
    Then the rating is hidden

  Scenario: Hiding needs a reason
    When Homa hides a rating with no reason
    Then it is rejected with HidingReasonRequiredException

  Scenario: A rating is hidden once
    Given a hidden rating
    When Homa hides it again
    Then it is rejected with RatingAlreadyHiddenException

Feature: Rating summaries
  Scenario Outline: The overall average is shown only after the minimum number of ratings
    Given a provider with <count> ratings
    Then the overall average is shown is <shown>
    Examples:
      | count | shown |
      | 2     | false |
      | 3     | true  |

  Scenario: The average is rounded to one decimal place
    Given a provider rated 5, 4 and 4 stars overall
    Then the overall average is 4.3

  Scenario: Withdrawing a hidden rating removes it from the averages
    Given a provider rated 5, 4 and 1 stars overall
    When the 1-star rating is withdrawn
    Then the overall average is 4.5
    And the rating count is 2

  Scenario: Category averages count only the ratings that rated that category
    Given a provider with PUNCTUALITY rated 5 and 3, and two other ratings without PUNCTUALITY
    Then the PUNCTUALITY average is 4.0
    And the PUNCTUALITY count is 2

  Scenario: A category average is shown once that category has the minimum ratings
    Given a provider with 5 ratings, of which 2 rated COMMUNICATION
    Then the overall average is shown
    And the COMMUNICATION average is not shown

  Scenario: A summary cannot withdraw more ratings than it has
    Given a helper with no ratings
    When a rating is withdrawn
    Then it is rejected with NoRatingToWithdrawException
```

---

## Not in the domain phase

These have no business rules of their own, or they belong to usecase or infra later:

- **Notifications:** what to send and when follows from the IEs above; there's no domain object.
- **Admin console tracking:** read models over the contexts above.
- **Privacy data requests:** a workflow for the usecase layer, no domain rules beyond recording each request.
- **Evidence storage and retention:** the 7-day retention is a scheduled infra job driven by `ReservationCompletedIE` and `CaseDecidedIE`.
- **Distance calculation:** the geoservice computes the distance; the domain only checks it against the radius.
- **Identity verification:** the KYC provider runs KYB, KYC, re-verification and face matching; the domain only records the results it reports.

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
| `HomaPolicyChangedIE` | policy | offering, reservation, discovery | `HomaPolicy.change` |
| `CustomerRegisteredIE` | account | conversation | `CustomerAccount.register` |
| `CustomerRenamedIE` | account | conversation | `CustomerAccount.rename` returning `true` |
| `ProviderVerifiedIE` | account | offering, reservation, discovery | `ProviderAccount.recordKybResult` returning `VERIFIED`, with `verifiedUntil()` |
| `ProviderVerificationRejectedIE` | account | offering, (notifications) | `ProviderAccount.recordKybResult` returning `REJECTED` |
| `PayoutAccountReadyIE` | account | offering, payment | `ProviderAccount.recordPayoutAccountReady` |
| `MemberVerificationStartedIE` | account | (provider app, notifications) | `MemberVerification.start` |
| `MemberVerificationRejectedIE` | account | (provider app, notifications) | `MemberVerification.recordKycResult` returning `REJECTED` |
| `TeamMemberAddedIE` | account | offering | `TeamMember.add`, with the role, registered name and lawful-employment confirmation |
| `RecheckFailedIE` | account | (notifications) | `TeamMember.recordKycResult`, `ProviderAccount.recordKybResult` returning `RECHECK_FAILED` |
| `TeamMemberReverifiedIE` | account | offering | `TeamMember.recordKycResult` returning `REVERIFIED`, with `verifiedUntil()` |
| `TeamMemberLapsedIE` | account | offering | `TeamMember.lapse` |
| `ProviderReverifiedIE` | account | offering, reservation, discovery | `ProviderAccount.recordKybResult` returning `REVERIFIED`, with `verifiedUntil()` |
| `ProviderVerificationLapsedIE` | account | offering | `ProviderAccount.lapse` |
| `ReverificationRequiredIE` | account | reservation, discovery, (notifications) | `ProviderAccount.requireReverification`, `TeamMember.requireReverification` |
| `ProviderFlaggedIE` | account | offering, reservation, payment, (notifications) | `ProviderAccount.recordSeriousFlag` |
| `TeamMemberRemovedIE` | account | offering | `TeamMember.remove` |
| `ListingWentLiveIE` | offering | discovery | `ProviderListing.goLive` |
| `ListingChangedIE` | offering | discovery | any `ProviderListing` setter on a live listing |
| `ListingWentOfflineIE` | offering | discovery | `ProviderListing.updateRoster` leaving a live listing with no helper, or with a supervisor rule and no supervisor |
| `ListingPausedIE` | offering | discovery, reservation | `ProviderListing.pause` |
| `ListingResumedIE` | offering | discovery, reservation | `ProviderListing.resume` |
| `EnquiryCreatedIE` | reservation | conversation | `Enquiry.create`, `Enquiry.bookAgain` |
| `RequestDeclinedForTakenTimeIE` | reservation | payment, (notifications) | `Reservation.declineForTakenTime` |
| `TeamMemberDroppedIE` | reservation | (notifications) | `Reservation.dropFromTeam` returning `REMOVED` or `TEAM_INCOMPLETE`, with whether the team is complete |
| `TeamMemberStoppedIE` | reservation | (notifications) | `Reservation.dropFromTeam` returning `CANNOT_WORK` |
| `EnquiryRequestedIE` | reservation | conversation | `Enquiry.markRequested` |
| `EnquiryClosedIE` | reservation | conversation | `Enquiry.isClosed` turning true |
| `ReservationRequestedIE` | reservation | payment | `Reservation.request` |
| `ReservationEditedIE` | reservation | payment | `Reservation.edit` (with the price change) |
| `ReservationAcceptedIE` | reservation | payment, availability | `Reservation.accept` |
| `ReservationDeclinedIE` | reservation | payment | `Reservation.decline` |
| `ReservationExpiredIE` | reservation | payment | `Reservation.expire` |
| `ReservationCancelledIE` | reservation | availability | any cancel or no-show after acceptance |
| `TeamNamedIE` | reservation | (notifications) | `Reservation.nameTeam` |
| `ProviderCheckedInIE` | reservation | (notifications) | `Reservation.checkIn`, the location check |
| `FaceCheckRecordedIE` | reservation | (notifications) | `Reservation.recordFaceCheck` |
| `CheckInCompletedIE` | reservation | (notifications) | `Reservation.recordFaceCheck` with the first pass, or `Reservation.recordFaceCheckUnavailable` |
| `CompletionSubmittedIE` | reservation | (notifications) | `Reservation.submitCompletion` |
| `ReservationFlaggedIE` | reservation | (notifications, admin) | `Reservation.disputeForProviderFlag` |
| `ReservationRestoredIE` | reservation | (notifications) | `Reservation.restoreAfterFlagCleared` |
| `ReportFiledIE` | reservation | resolution | `Reservation.fileReport`, with the report type |
| `CaseClosedForProviderFlagIE` | resolution | (admin tracking) | `Case.closeForProviderFlag` |
| `NoShowChallengedIE` | reservation | resolution | `Reservation.challengeNoShow` |
| `CompletionDeadlineMissedIE` | reservation | resolution | `Reservation.missCompletionDeadline` |
| `ReservationCompletedIE` | reservation | availability | `Reservation.closeDisputeWindow`, `Reservation.resolve` |
| `SettlementDecidedIE` | reservation | payment | every method returning a `Settlement` |
| `FineChargedIE` | reservation | payment | every method returning a `FineCharge` |
| `CaseDecidedIE` | resolution | reservation | `Case.decide`, with `Case.providerReceives` |
| `RatingSubmittedIE` | feedback | (provider page, notifications) | `Rating.submit` |
| `RatingHiddenIE` | feedback | (provider page) | `Rating.hide` |
| `PaymentCapturedIE` | payment | (notifications) | `Payment.capture` |
| `PaymentRefundedIE` | payment | (notifications) | refund instructions from `Payment.settle`, `Payment.cancelBeforeAcceptance` |
| `PayoutCreatedIE` | payment | (notifications) | `FineDeductionPolicy.apply` |
| `PayoutHeldIE` | payment | (admin, notifications) | `Payout.hold` |
| `PayoutHoldReleasedIE` | payment | (notifications) | `Payout.releaseHold` |
| `PayoutWithheldIE` | payment | (admin, notifications) | `Payout.withhold` |
| `FineWaivedIE` | payment | (notifications) | `Fine.waive` |
| `ServiceAreaPublishedIE` | catalogue | offering | `ServiceArea.launch` |
| `JobRenamedIE` | catalogue | discovery, (provider page) | `JobList.rename` |
| `JobRegroupedIE` | catalogue | discovery, (provider page) | `JobList.regroup` |
| `ToolRenamedIE` | catalogue | discovery, (provider page) | `ToolList.rename` |
| `JobWithdrawnFromListingIE` | offering | (notifications) | `ProviderListing.withdrawJob` returning `WITHDRAWN` on a live listing |
| `ListingWentOfflineForRetiredJobIE` | offering | (notifications) | `ProviderListing.withdrawJob` returning `WITHDRAWN_AND_OFFLINE` |
| `ToolWithdrawnFromListingIE` | offering | (notifications) | `ProviderListing.withdrawTool` returning `true` on a live listing |
| `RequestCancelledIE` | reservation | payment | `Reservation.cancelByCustomer` on a request |
| `PaymentFailedIE` | payment | reservation | a failed authorisation or FPX payment; no `Payment` is created |
| `PaymentSecuredIE` | payment | reservation, (notifications) | `Payment.authoriseCard`, `Payment.payUpfrontByFpx`, `Payment.adjustTotal` |
| `SearchPerformedIE` | discovery | (admin tracking) | `MatchClassifier`'s results for one `SearchCriteria` |
| `ListingHoursNarrowedIE` | offering | discovery, (notifications) | `ProviderListing.fitHours` returning `NARROWED` on a live listing |
| `ListingWentOfflineForHoursIE` | offering | discovery, (notifications) | `ProviderListing.fitHours` returning `CLEARED_AND_OFFLINE` |

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
12. `feedback`, added after the domain phase closed:
    1. `policy`: add the rating window (14 days) and minimum ratings to show (3) as Homa-set values
    2. `feedback` domain
    3. `ie/`: add `RatingSubmittedIE` and `RatingHiddenIE`, and the two new values to `HomaPolicyChangedIE`

## Testing approach

- **Gherkin is the spec, JUnit is the test.** Each feature above becomes a test class; each scenario a test method named after it; each Examples row a parameterised case. Cucumber is optional: if used, it runs with no Spring, matching the framework-free domain rule.
### Test naming convention

Tests are named in the **ubiquitous language** of the bounded context (the glossary is in `docs/homa-domain-stories.md`, under "Ubiquitous language"): they describe business behaviour the way a founder or provider would say it, never the method or class being called. A test name should still make sense if the code inside were rewritten.

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

## Decisions made while building

These are folded in from the build. The method blocks and object tables above already match them;
this section records the reasoning and the edge cases. The product-level decisions are also stated in
`docs/homa-product-description.md`.

> **Read with the changes below.** This log is kept as it was decided during the domain phase. The
> "Change after the domain phase" sections that follow it take precedence; entries they replaced are
> marked *Superseded* here.

All decisions below are confirmed by the product owner, including: customers have a name; `HomaLimits` for hours and helpers; a live
listing whose verified team becomes empty is no longer live; every decision listed under the
reservation and payment details below; touching
windows, touching blocks and the later-start tie-break in availability, plus a buffer between jobs;
`Helper.remove()`.

Open questions answered: #1 the customer no-show wait starts from the later of the booked start and
check-in; #2 percentages round half-up and the refund is computed last; #3 editing a request does not
reset its expiry; #6 the lead is one of the named helpers, and a provider who is their own helper can
be the lead (*Superseded: there is no lead; see "provider types, team roles, documents and face
check-in"*); #5 SMS codes are 6 digits, valid 5 minutes, with a new code after 5 wrong attempts; #8 a request keeps the offering snapshot it was made with; #4 near the chosen time means within 2
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
  tool is retired only once and its code is never reused; featured searches are ordered from 1. A
  preset search needs at least one job, hour and helper. Homa's duration range can change, so it is
  checked when the search runs, in `discovery`.
- **Account details:**
  - Customers register with a name as well as a phone number and address, because the spec says
    providers see "the customer's name, address and reservation details".
  - Phone numbers are Malaysian mobiles (`+601` then 8 or 9 digits).
  - The SMS code (open question 5) is 6 digits and expires 5 minutes after it is sent, exactly at the
    5-minute mark. After 5 wrong codes, a new code is needed.
  - An eKYC result is `VERIFIED` or `REJECTED`; a rejected provider can be verified on a later attempt.
    *Superseded: the business is verified through the KYC provider's KYB (`KybResult`), and a
    rejected KYB can be redone; see "business providers only, verification by a KYC provider".*
  - A provider is their own helper by default. *Superseded: a business starts with no team members,
    and an owner or director who works on site is added like anyone else.*
  - `Helper.remove()` is added, so `HelperRemovedIE` has a domain method behind it. *Superseded:
    `TeamMember.remove()` and `TeamMemberRemovedIE`.*
- **Offering details:**
  - `setHours` and `setHelpers` both take `HomaLimits`, offering's own view of Homa's duration range and
    helpers per search, since the 10-helper cap is a Homa-set value. *Superseded: Homa sets no limit
    on helpers, so only `setHours` takes `HomaLimits`; `setHelpers` caps the maximum at the helpers on
    the team.*
  - A listing offers at least one job and covers at least one city. Bringing no tools is allowed.
  - A separate travel fee is above zero; charging none means travel is included.
  - When the team shrinks, max helpers shrinks to the team size, and min helpers with it if needed. A
    growing team doesn't widen the range. A live listing whose verified team becomes empty is no
    longer live.
  - Go-live lists every missing requirement, including the provider's own verification. Helpers'
    verification is no longer a go-live requirement: only team members are on the roster, and people
    still waiting for verification never block going live (see "Change after the domain phase: adding
    a team member", which replaced invites). Only a live
    listing gives an `OfferingSnapshot`, and a live listing doesn't go live twice.
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
  - `Reservation.cancellationTerms()` gives what the review page shows: when the fee starts (the booked
    start on Flexible), the fee from then, the period when cancelling is locked, and the customer no-show
    fee once checked in. `cancelByCustomer` reads its fee from the same terms.
  - Who checks in is not recorded or checked (confirmed). Helpers have no accounts and the provider is
    accountable either way.
  - Report evidence is optional and a reason is required (confirmed). The spec now says "with a reason,
    and evidence where available".
  - Availability at request and at acceptance is checked by the usecases, not by `Reservation`
    (confirmed).
  - **Payment phase note:** a customer no-show settlement is released when the report window ends.
    If it is challenged, Homa's decision gives a final settlement that replaces it.
  - The team reminder is a new Homa-set value, 6 hours (confirmed; changed from 24 hours).
    `isTeamReminderDue` is true while the booking is reserved and the team is not named, from 6 hours
    before the booked start until the
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
  - Eight publishing contexts each have an event interface in `ie/port`, and the 57 events (68 since the usecase review) in the
    plan's table are records in `ie/events`, including `feedback`'s two, the team membership events
    and the re-verification events.
    Availability, conversation and discovery only listen.
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
    *Superseded for helpers: today a verified person is added with `TeamMemberAddedIE`, and a rejected
    one gives `MemberVerificationRejectedIE`; the provider's own result comes from KYB, not eKYC (see
    "adding a team member" and "business providers only").*
  - The tests check that each event can't be built with a required field missing, can't be changed
    after it is built, and belongs to its context's interface. ArchUnit checks that `ie` imports only
    `kernel` and Java and that no domain imports `ie`. The mutation gate doesn't apply to `ie`.
- **Ids are Strings.** Following `XId().generate()` returning a `String`, signatures the plan writes
  with an id type, such as `Helper.add(ProviderId provider, ...)`, take a `String` id.
- **Jobs and tools have no `equals`.** They are only reached through their list, keyed by code.

- **Feedback details** (added after the domain phase closed, confirmed by the product owner):
  - `RateableReservation` also carries whether the reservation is already rated, so `Rating.submit` can
    enforce "a reservation is rated once". The usecase sets it from storage.
  - The rating window includes its last moment: 14 days after 14:00 is still in, 14:01 is not.
  - Checks run in this order: the reservation is Completed, the customer is its customer, it is not
    rated yet, the window is open, and every rated helper was named.
  - Only the overall stars are required, for the provider and for each rated helper. Category stars (any
    number, or none) and the provider's written feedback are optional.
  - Written feedback is counted in characters, not bytes, and kept without surrounding spaces. Empty
    means none.
  - A summary is rated only on its own categories: provider categories for a provider, helper
    categories for a helper. Any other name, when recording, withdrawing or reading an average, is
    rejected with `RatingDoesNotFitSummaryException`. Withdrawing a rating, or a category, that was
    never counted is rejected with `NoRatingToWithdrawException`.
  - Before any rating, an average is 0.0 and not shown. The spec shows "New".
  - The minimum ratings to show is at least 1, and the same value applies to the overall average and to
    each category.
  - Events carry no maps, so `RatingSubmittedIE` carries the overall stars, whether there is written
    feedback, and the ids of the helpers rated. Category stars stay in `feedback`'s own summaries.

## Change after the domain phase: team membership

Decided after the domain phase closed. A helper is **invited** first and becomes a **team member** only once their eKYC is verified, so "team" means verified everywhere.

- `account`: `Helper.add` becomes `Helper.invite`. `Helper` replaces its eKYC status and removed flag with one `TeamStatus`: `INVITED`, `INVITE_REJECTED`, `MEMBER`, `REMOVED`. `isVerifiedTeamMember()` and `isOnTeam()` become one method, `isTeamMember()`, true only for `MEMBER`. A rejected invite can retry; a removed helper cannot come back.
- `offering`: `goLive` drops the `allHelpersVerified` flag and the `ALL_HELPERS_VERIFIED` requirement. Instead the roster must have at least the minimum helpers per booking. Pending invites never block going live.
- `ie/`: `HelperVerifiedIE` becomes `HelperJoinedTeamIE`, `HelperVerificationRejectedIE` becomes `HelperInviteRejectedIE`, and `HelperInvitedIE` is added.
- **Usecase phase, required test:** a provider's `TeamRoster` is built only from helpers where `isTeamMember()` is true, plus the provider if they include themselves, and is rebuilt on `HelperJoinedTeamIE` and `HelperRemovedIE`.
- **Decided while building:**
  - A `MEMBER` is already verified, so a new eKYC result for a member is rejected with
    `HelperAlreadyTeamMemberException`. Otherwise a late rejection would drop the member off the team
    without a `HelperRemovedIE`. To take a member off the team, the provider removes them.
  - "The roster must cover the minimum helpers" needs no separate go-live check. The roster holds only
    team members, and the helper range is set within it and shrinks with it, so the `HELPERS`
    requirement already guarantees it.
  - `TeamRoster`'s field, in `offering` and in `reservation`, is renamed from `verifiedHelperIds` to
    `memberIds`. The listing events' field is renamed to `teamMemberIds`.

## Change after the domain phase: periodic re-verification

> **Superseded in part** by "business providers only, verification by a KYC provider": the verification
> mechanics described here (documents, due-date maths, re-checks, face match attempts, the provider
> types) now belong to the KYC provider. The marketplace rules stay.

Decided after the domain phase closed, for customer safety. Every provider and team member is re-verified every **3 months**.

**New Homa-set values (`policy`):** re-verification period 3 months; re-verification reminder 14 days before the due date; early re-check deadline 14 days.

**`account`**

- `Helper` and `ProviderAccount` hold a `Verification`: last verified, due date (last verified + period) and whether a re-check is open.
- `TeamStatus` gains `LAPSED`. `ProviderAccount` gains a lapsed state too; `isReadyForGoLive()` and taking new requests need it not lapsed.
- The re-check opens at the reminder (due date − 14 days), or straight away when Homa asks for an early re-check, which also moves the due date to now + 14 days if that is sooner.
- eKYC results for someone already verified:
  - With a re-check open: `VERIFIED` resets last verified and the due date and closes the re-check. `REJECTED` changes nothing yet; they can retry until the due date.
  - With no re-check open: `VERIFIED` is a no-op (Kenal may deliver the same result twice). `REJECTED` is refused with `HelperAlreadyTeamMemberException` (or the provider equivalent).
  - `LAPSED`: `VERIFIED` makes them a `MEMBER` again with a new due date. `REJECTED` keeps them lapsed.
- Lapsing is an explicit step taken by a scheduler at the due date, not something computed on read, so `isTeamMember()` stays a plain status check.

```java
Helper.openRecheck(VerificationLimits limits, Instant now)                 // from due date - reminder
Helper.requestEarlyRecheck(String reason, VerificationLimits limits, Instant now)
Helper.recordEkycResult(VerificationStatus result, VerificationLimits limits, Instant now)
Helper.lapse(Instant now)                                                   // MEMBER whose due date has passed -> LAPSED
Helper.verification() : Verification
// ProviderAccount gets the same four methods; a lapsed provider is not ready for go-live and takes no new requests
```

**`offering`:** `ProviderListing.pause()` and `resume()`. A paused listing takes no new requests and is left out of search; it stays live, so accepted reservations go ahead. `OfferingSnapshot` carries whether the listing takes requests, and `Reservation.request` refuses a paused listing with `ListingPausedException`.

**`ie/`:** `RecheckOpenedIE` (provider and team member, for the reminder), `HelperReverifiedIE`, `HelperLapsedIE`, `ProviderReverifiedIE`, `ProviderVerificationLapsedIE`. `offering` listens to `HelperLapsedIE` and `HelperJoinedTeamIE`/`HelperReverifiedIE` to rebuild the roster, and to `ProviderVerificationLapsedIE`/`ProviderReverifiedIE` to pause and resume.

**Usecase phase:** a lapsed helper named on an upcoming reservation means the provider must name someone else before the start; the reservation's existing "team must be on the roster" rule then applies. *Changed in "aligning the product spec, domain plan and code": the person comes off the team with `Reservation.dropFromTeam`, so the team is incomplete until it is named again.* This rule and the other rules from this change are listed as required rules R1 to R5 in the earlier usecase plan (removed before the usecase phase restarts; in git history up to `170c533`).

**Decided while building** (all confirmed by the product owner):

- **Outcomes:** `recordEkycResult` returns an `EkycOutcome`, so the usecase publishes one event per
  outcome and nothing for a duplicate. The outcomes are `VERIFIED` (first pass: the provider is
  verified, or the invite joins the team), `REJECTED` (first attempt failed), `REVERIFIED`,
  `RECHECK_FAILED` (a re-check or lapsed person failed; nothing changes) and `UNCHANGED` (a repeated
  `VERIFIED` with no re-check open). Each outcome but `UNCHANGED` has one event:

  | Outcome | Helper | Provider |
  |---|---|---|
  | `VERIFIED` | `TeamMemberAddedIE`, after `TeamMember.add` | `ProviderVerifiedIE` |
  | `REJECTED` | `MemberVerificationRejectedIE` | `ProviderVerificationRejectedIE` |
  | `REVERIFIED` | `TeamMemberReverifiedIE` | `ProviderReverifiedIE` |
  | `RECHECK_FAILED` | `RecheckFailedIE` | `RecheckFailedIE` |
  | `UNCHANGED` | none | none |
- **Months on Malaysian dates:** the due date is the last verification plus the period in months,
  counted on `Asia/Kuala_Lumpur` dates. A date with no match in the later month lands on that month's
  last day, so 30 November gives 29 February in a leap year.
- **`Verification`** has four fields: `lastVerified`, `dueAt`, `recheckOpen`, and
  `earlyRecheckReason`, which is set only while an early re-check is open. Passing a re-check starts a
  new `Verification` with no re-check open. `Helper.verification()` and
  `ProviderAccount.verification()` return `Optional`, which is empty until the first pass.
- **`policy`:**
  - The three values are `ReverificationPeriod(int months)` (at least 1), `ReverificationReminder`
    and `EarlyRecheckDeadline`. There are now 29 Homa-set values.
  - `HomaPolicyChangedIE` carries all three, so account can build its `VerificationLimits`.
- **Provider state:**
  - A lapsed provider is `VerificationStatus.LAPSED`. It is never a result from Kenal:
    `PENDING` and `LAPSED` are both refused as results.
  - The provider equivalent of `HelperAlreadyTeamMemberException` is
    `ProviderAlreadyVerifiedException`. Before this change, a later `REJECTED` would overwrite a
    verified provider.
- **Who can be re-checked:** only a `MEMBER` or a `VERIFIED` provider can have a re-check opened, be
  asked for an early re-check, or lapse. Anyone else is refused with `HelperNotTeamMemberException`
  or `ProviderNotVerifiedException`. A removed helper is refused with `HelperRemovedException`.
- **Opening the re-check:**
  - Opening a re-check that is already open is refused with `RecheckAlreadyOpenException`.
  - An early re-check can still be asked for while the regular one is open; it moves the due date
    only if that is sooner.
- **Lapsing:**
  - A member with a re-check open still lapses at the due date.
  - A missed early re-check lapses at its deadline, because that deadline is the due date.
- **`offering`:**
  - `pause()` needs a live listing (`ListingNotLiveException`) that is not already paused
    (`ListingAlreadyPausedException`). `resume()` needs a paused listing (`ListingNotPausedException`).
  - A team change keeps the pause. A listing that goes offline is no longer paused, and `goLive`
    already needs a provider who is not lapsed.
  - Going offline, like pausing, changes only the listing. It never touches an accepted reservation,
    which keeps its own snapshot.
  - `takesRequests()` is live and not paused, and the snapshot copies it.
- **Search needs to know:**
  - `ListingPausedIE` and `ListingResumedIE` were added (offering to discovery and reservation), so
    search can leave paused listings out and reservation knows when acceptance is blocked.
  - `RecheckFailedIE` was added, so the provider is asked to have the person retry. It carries
    `providerId`, `helperId` (empty for the provider's own re-check) and `dueAt`. That makes 49 events.
  - `RecheckOpenedIE` carries `providerId`, `helperId` (empty for the provider's own re-check),
    `dueAt` and `earlyRecheckReason` (empty for the regular re-check).
- **`reservation`:**
  - In the domain, only `Reservation.request` checks the pause, through the snapshot's
    `takesRequests`.
  - A paused provider can't accept any request, including one made before the pause. That request
    stays Requested and expires normally, unless the provider re-verifies in time and then accepts it.
    Accepted reservations go ahead.
  - The pause lives in offering, and a waiting request's snapshot is from before the pause. So this is
    a required usecase rule, and the domain stays as it is: `ProviderAcceptRequest` refuses with
    `ListingPausedException` while the listing is paused (see the earlier usecase plan (removed before the usecase phase restarts; in git history up to `170c533`)).

```gherkin
Feature: Periodic re-verification
  Background:
    Given Aina became a team member on 1 January

  Scenario: Re-verification is due 3 months after the last verification
    Then Aina's re-verification is due on 1 April

  Scenario Outline: The re-check opens 14 days before the due date
    When Homa opens the re-check on <date>
    Then the result is <result>
    Examples:
      | date     | result                       |
      | 18 March | re-check open                |
      | 17 March | RecheckNotYetDueException    |

  Scenario: Passing the re-check resets the due date
    Given the re-check is open
    When Aina's eKYC is VERIFIED on 20 March
    Then Aina is still a MEMBER
    And Aina's re-verification is due on 20 June
    And no re-check is open

  Scenario: A failed re-check can be retried before the due date
    Given the re-check is open
    When Aina's eKYC is REJECTED on 20 March
    Then Aina is still a MEMBER
    And the re-check is still open

  Scenario: A repeated VERIFIED result with no re-check open changes nothing
    Given no re-check is open
    When Aina's eKYC is VERIFIED again
    Then Aina's re-verification is still due on 1 April

  Scenario: A REJECTED result with no re-check open is refused
    Given no re-check is open
    When Aina's eKYC is REJECTED
    Then it is rejected with HelperAlreadyTeamMemberException

  Scenario Outline: Missing the due date lapses the member
    When Homa lapses Aina on <date>
    Then the result is <result>
    Examples:
      | date     | result                           |
      | 1 April  | Aina is LAPSED and not a team member |
      | 31 March | ReverificationNotYetDueException |

  Scenario: A lapsed member who passes rejoins the team
    Given Aina is LAPSED
    When Aina's eKYC is VERIFIED on 5 April
    Then Aina is a MEMBER
    And Aina's re-verification is due on 5 July

  Scenario: An early re-check moves the due date sooner
    When Homa asks for an early re-check on 10 January with the reason "Report R12: different person attended"
    Then the re-check is open
    And Aina's re-verification is due on 24 January

  Scenario: An early re-check never moves the due date later
    When Homa asks for an early re-check on 25 March
    Then Aina's re-verification is still due on 1 April

  Scenario: An early re-check needs a reason
    When Homa asks for an early re-check with no reason
    Then it is rejected with RecheckReasonRequiredException

Feature: Provider re-verification
  Scenario: A lapsed provider is not ready and takes no new requests
    Given a live provider whose re-verification was due on 1 April
    When Homa lapses the provider on 1 April
    Then the provider is not ready for go-live

  Scenario: A paused listing refuses new requests but keeps accepted ones
    Given a live listing with an accepted reservation for 5 April
    When the listing is paused
    Then a new request is rejected with ListingPausedException
    And the accepted reservation is unchanged

  Scenario: A paused listing resumes when the provider passes
    Given a paused listing
    When the listing is resumed
    Then it takes new requests
```

## Change after the domain phase: verified for the whole job

Decided after the re-verification change. A provider can be requested, and can accept, only for a job
that **ends by** their re-verification due date. Search leaves them out for any job that would end
later. Passing a re-check moves the due date 3 months out, and those dates open up.

**`reservation`:**

- `request`, `edit` and `accept` take `Instant providerVerifiedUntil`, the provider's due date as it is
  now. A job whose end is after it is refused with `JobEndsAfterProviderVerificationException`.
- A job ending exactly at the due date is allowed, because the provider lapses only from that moment.
- `accept` checks the due date as it is at acceptance, not as it was at request time. So an early
  re-check can block acceptance, and a passed re-check unblocks it.

**`discovery`:**

- `ProviderCandidate` carries `verifiedUntil`.
- A job at the nearest free start that would end after it is no match, with reason
  `VERIFICATION_DUE_BEFORE_END`. A provider with no free time stays `NOT_FREE`.

**`ie/`:**

- `ProviderVerifiedIE` and `ProviderReverifiedIE` carry `reverificationDueAt`.
- reservation and discovery keep each provider's due date from those two events and from
  `RecheckOpenedIE`, whose `dueAt` an early re-check moves sooner.

**Usecase phase:** the request, edit and accept usecases pass the due date from reservation's
projection (required rule R6 in the earlier usecase plan (removed before the usecase phase restarts; in git history up to `170c533`)).

**Note:** bookings go up to 30 days ahead, but the regular re-check opens only 14 days before the
due date. So from 30 to 14 days before it, a job after the due date can't be booked, and the provider
can't yet re-verify to open those dates up.

```gherkin
Feature: Verified for the whole job
  Background:
    Given a booking on Saturday from 10:00 to 13:00

  Scenario Outline: A job must end by the provider's due date
    Given the provider's re-verification is due on Saturday at <due>
    When the customer requests the booking
    Then the result is <result>
    Examples:
      | due   | result                                     |
      | 13:00 | the request is made                        |
      | 12:59 | JobEndsAfterProviderVerificationException  |

  Scenario: An edit cannot move the end past the due date
    Given a request with the provider due on Saturday at 13:00
    When the customer changes the booking to 4 hours
    Then it is rejected with JobEndsAfterProviderVerificationException

  Scenario: The provider cannot accept a job ending after their due date as it is now
    Given a request
    And an early re-check moved the provider's due date to Saturday at 12:00
    When the provider accepts
    Then it is rejected with JobEndsAfterProviderVerificationException

  Scenario: The provider can accept once a passed re-check moves the due date later
    Given a request the provider could not accept because of their due date
    When the provider passes the re-check and accepts
    Then the reservation is Reserved

  Scenario: Search leaves out a provider whose due date comes before the job ends
    Given a provider who fits the search and whose due date is a second before the job ends
    When the customer searches
    Then the provider is no match, because their verification is due before the job ends
```

## Change after the domain phase: provider types, team roles, documents and face check-in

> **Superseded in part** by "business providers only, verification by a KYC provider": the verification
> mechanics described here (documents, due-date maths, re-checks, face match attempts, the provider
> types) now belong to the KYC provider. The marketplace rules stay.

Decided after the domain phase closed. Safety principle: only verified people enter a customer's home, and the customer knows who they are before they arrive.

**`policy`**

- Remove the Homa limit on helpers (the old 1–10 range). Helpers per booking and team size have no Homa upper limit.
- Add **face check attempts** (3 per person per check-in) and the **accepted documents** list: for providers and owners or directors, Malaysian MyKad or Malaysian passport; for team members of an independent provider, a Malaysian identity card or passport; for team members of a business provider, an identity card or passport from any country.

**`account`**

| Object | Kind | Responsibility |
|---|---|---|
| `ProviderType` | Enum | `INDEPENDENT`, `BUSINESS` |
| `ProviderAccount` | Aggregate | Gains its `ProviderType`; a business also holds its SSM registration number and whether Homa has checked it |
| `IdentityDocument` | Value object | Issuing country, type (`IDENTITY_CARD`, `PASSPORT`) and the **registered name** read from the eKYC result |
| `TeamMember` | Aggregate | Renames `Helper`. Holds a `TeamRole` (`HELPER`, `SUPERVISOR`), the `IdentityDocument`, the profile photo and whether it matched the eKYC face, the `TeamStatus` and `Verification` |
| `EmploymentConfirmation` | Value object | The provider's confirmation that every team member they add is lawfully employed or engaged and allowed to work in Malaysia: when it was given and the terms version |
| `AcceptedDocuments` | Value object | account's view of the Homa-set accepted documents list |

- The registered name comes only from the eKYC result; there is no method to set or change it.
- A document outside the accepted list is refused with `DocumentNotAcceptedException`. A foreign document for an independent provider's member is refused the same way.
- Homa does not check work permits. A provider must give an `EmploymentConfirmation` before inviting team members and before going live (`EmploymentNotConfirmedException`).
- **Due date** = last verified + re-verification period. Document expiry does not change it.
- A business provider cannot go live until its registration is checked.

```java
ProviderAccount.register(PhoneVerification verified, ProviderType type)
ProviderAccount.recordBusinessRegistrationChecked(String ssmNumber, Instant now)   // BUSINESS only
ProviderAccount.confirmLawfulEmployment(String termsVersion, Instant now)
ProviderAccount.recordEkycResult(EkycResult result, AcceptedDocuments accepted, VerificationLimits limits, Instant now)

TeamMember.invite(String providerId, ProviderType providerType, TeamRole role, Photo photo)
TeamMember.recordEkycResult(EkycResult result, AcceptedDocuments accepted, VerificationLimits limits, Instant now)
TeamMember.recordPhotoMatch(boolean matches)
TeamMember.registeredName() : String
TeamMember.role() : TeamRole
```

**`offering`**

- `HelpersRange` has no Homa upper limit: minimum at least 1, maximum at most the number of **helpers** on the roster.
- `TeamRoster` holds members by role: helper ids and supervisor ids. Supervisors never count towards the helpers range.
- New `SupervisorRule` (value object, optional): from a minimum number of helpers, a minimum number of hours, or both. `requiresSupervisor(helpers, hours)` is true when either is reached.
- `ProviderListing.setSupervisorRule(SupervisorRule rule)` needs at least one supervisor on the roster. Go-live needs a supervisor when a rule is set, and `updateRoster` takes a live listing offline if a rule is set and no supervisor is left.
- `OfferingSnapshot` carries the `SupervisorRule` and the provider type.

**`discovery`:** `SearchCriteria` needs at least 1 helper and has no upper limit.

**`reservation`**

- `TeamAssignment` becomes helpers plus supervisors; the lead is removed. Rules: the number of helpers matches the booking; every person is on the roster in that role; at least one supervisor when the offering snapshot's `SupervisorRule` requires it (`SupervisorRequiredException`); the same person cannot be named twice.
- Check-in becomes two steps:

```java
Reservation.checkIn(int distanceMetres, Instant now)                 // location check, as before; check-in is not complete yet
Reservation.recordFaceCheck(String memberId, FaceCheckResult result, Instant now)   // PASSED or FAILED, per named person
Reservation.recordFaceCheckUnavailable(Instant now)                  // service down: check-in completes on location only
Reservation.isCheckedIn() : boolean                                  // location passed and (one face passed or face check unavailable)
Reservation.canWork(String memberId) : boolean                       // passed their face check, or face check unavailable
```

- A person who fails the **face check attempts** cannot work on that job (`FaceCheckAttemptsUsedException` on a further attempt).
- The provider no-show rule now uses `isCheckedIn()`: if it is still false at the end of the check-in period, the reservation ends as a provider no-show. A face check service outage never causes a no-show.
- The pricing is unchanged: supervisors are not charged.

**`feedback`:** supervisors are rated exactly like helpers, with the helper categories. `RateableReservation` carries all named members.

**`ie/`:** `TeamMemberInvitedIE`, `TeamMemberJoinedIE` and `TeamMemberInviteRejectedIE` replace the helper events and carry the role; add `BusinessRegistrationCheckedIE`, `LawfulEmploymentConfirmedIE`, `FaceCheckRecordedIE` and `CheckInCompletedIE`.

```gherkin
Feature: Provider types and documents
  Scenario Outline: The provider must be Malaysian
    When a provider verifies with a <document> issued by <country>
    Then the result is <result>
    Examples:
      | document      | country   | result                         |
      | IDENTITY_CARD | Malaysia  | verified                       |
      | PASSPORT      | Malaysia  | verified                       |
      | PASSPORT      | Indonesia | DocumentNotAcceptedException   |

  Scenario Outline: Only a business provider can have foreign team members
    Given a <type> provider
    When a member verifies with a passport issued by Indonesia
    Then the result is <result>
    Examples:
      | type        | result                         |
      | INDEPENDENT | DocumentNotAcceptedException   |
      | BUSINESS    | MEMBER                         |

  Scenario: Team members can only be invited after the lawful-employment confirmation
    Given a provider who has not confirmed lawful employment
    When the provider invites a helper
    Then it is rejected with EmploymentNotConfirmedException

  Scenario: The confirmation is recorded with its time and terms version
    When the provider confirms lawful employment under terms "2026-11" at 10:00
    Then the confirmation records terms "2026-11" and 10:00

  Scenario: The due date follows the cycle only
    Given a member verified on 1 January with a passport that expires on 15 February
    Then their re-verification is due on 1 April

  Scenario: The registered name comes from the eKYC result
    When a member's eKYC is VERIFIED with the registered name "Ahmad bin Ismail"
    Then the member's registered name is "Ahmad bin Ismail"

  Scenario: A business cannot go live before its registration is checked
    Given a complete business listing whose SSM registration is not checked
    When the provider goes live
    Then it is rejected with ListingNotReadyException

Feature: Supervisors
  Background:
    Given a provider with 4 helpers and 1 supervisor, sending a supervisor from 3 helpers or from 4 hours

  Scenario Outline: A supervisor is required from either threshold
    When a booking is for <helpers> helpers and <hours> hours
    Then a supervisor is required is <required>
    Examples:
      | helpers | hours | required |
      | 2       | 3     | false    |
      | 3       | 2     | true     |
      | 1       | 4     | true     |

  Scenario: Naming the team without a required supervisor is refused
    Given a reserved booking for 3 helpers
    When the provider names 3 helpers and no supervisor
    Then it is rejected with SupervisorRequiredException

  Scenario: Supervisors do not count towards the helpers range
    When the provider sets helpers from 1 to 5
    Then it is rejected with HelpersExceedTeamSizeException

  Scenario: Supervisors are not charged
    Given a booking for 3 helpers at RM30 per helper per hour for 2 hours with a supervisor
    Then the hours fee is RM180.00

  Scenario: A supervisor rule needs a supervisor on the team
    Given a provider with no supervisors
    When the provider sets a supervisor rule from 3 helpers
    Then it is rejected with NoSupervisorOnTeamException

Feature: Face check at check-in
  Background:
    Given a reserved booking starting Saturday 10:00 with Aina and Ben named
    And the location check passed at 09:58

  Scenario: Check-in completes when the first person passes
    When Aina's face check PASSED at 09:59
    Then the reservation is checked in
    And Aina can work
    And Ben cannot work yet

  Scenario: Location alone is not a check-in
    Then the reservation is not checked in

  Scenario: A person who fails every attempt cannot work
    Given Aina passed
    When Ben's face check FAILED 3 times
    Then Ben cannot work
    And a 4th attempt is rejected with FaceCheckAttemptsUsedException

  Scenario: A face check outage never makes a no-show
    When the face check service is unavailable at 10:05
    Then the reservation is checked in
    And Aina and Ben can work

  Scenario: No face pass by the end of the check-in period is a provider no-show
    Given no face check has passed
    When Homa marks a provider no-show at 10:16
    Then the reservation is CANCELLED
```

## Change after the domain phase: adding a team member

> **Superseded in part** by "business providers only, verification by a KYC provider": the verification
> mechanics described here (documents, due-date maths, re-checks, face match attempts, the provider
> types) now belong to the KYC provider. The marketplace rules stay.

Supersedes the invite model in "team membership" and "provider types". There are no invited members. The provider confirms lawful employment **on the add form**, together with the role, document type and (business only) issuing country; the person is added automatically once their eKYC is verified. Before that, nothing is on the team.

**`account`**

| Object | Kind | Responsibility |
|---|---|---|
| `MemberVerification` | Aggregate | The pending step before a member exists: provider, role, document type, issuing country (Malaysia for an independent provider), photo, the `EmploymentConfirmation` and the eKYC outcome. Not a team member and never shown to customers |
| `TeamMember` | Aggregate | Created only from a verified `MemberVerification`. `TeamStatus` is now `MEMBER`, `LAPSED`, `REMOVED` (`INVITED` and `INVITE_REJECTED` are gone) |
| `EmploymentConfirmation` | Value object | Given on the add form for one person: when it was given and the terms version. Required to start a `MemberVerification`. Replaces the one-off provider-level confirmation |

```java
MemberVerification.start(String providerId, ProviderType type, TeamRole role, DocumentType documentType,
                         String issuingCountry, Photo photo, EmploymentConfirmation confirmation,
                         AcceptedDocuments accepted, VerificationLimits limits, Instant now)
MemberVerification.recordEkycResult(EkycResult result, AcceptedDocuments accepted, Instant now)  // VERIFIED -> ready to add; REJECTED -> ended
TeamMember.add(MemberVerification verified, Instant now)                                       // the only way a TeamMember exists; the usecase calls it on VERIFIED
```

- `ProviderAccount.confirmLawfulEmployment` and `EmploymentNotConfirmedException` at provider level are removed; going live no longer needs a provider-level confirmation.
- `MemberVerification.start` refuses a missing confirmation (`EmploymentNotConfirmedException`), a country other than Malaysia for an independent provider, and a document outside the accepted list (`DocumentNotAcceptedException`). An eKYC result whose document doesn't match the chosen type and country is refused the same way.
- `TeamMember.add` refuses a `MemberVerification` that is not verified (`MemberNotVerifiedException`) or has already been used (`MemberAlreadyAddedException`).
- Re-verification of an existing member is unchanged; it does not need a new confirmation.

**`policy`:** no new value. eKYC is done on the spot on the provider's phone, with the person present (the person scans their own document and takes their own selfie); there is no link and no time limit.

**`ie/`:** `MemberVerificationStartedIE`, `MemberVerificationRejectedIE` and `TeamMemberAddedIE` replace `TeamMemberInvitedIE`, `TeamMemberInviteRejectedIE` and `TeamMemberJoinedIE`; `LawfulEmploymentConfirmedIE` is folded into `TeamMemberAddedIE`.

```gherkin
Feature: Adding a team member
  Scenario: A confirmed and verified person is added
    Given a provider started adding a helper with a MyKad, confirming lawful employment under terms "2026-11"
    When the eKYC is VERIFIED with the registered name "Aina binti Ahmad"
    Then Aina binti Ahmad is a MEMBER and a helper
    And the confirmation records the time and terms "2026-11"

  Scenario: The add form needs the confirmation
    When the provider starts adding a helper without ticking the confirmation
    Then it is rejected with EmploymentNotConfirmedException

  Scenario Outline: Only a business chooses the issuing country
    Given a <type> provider
    When the provider starts adding a helper with a passport issued by <country>
    Then the result is <result>
    Examples:
      | type        | country   | result                        |
      | INDEPENDENT | Malaysia  | waiting for verification      |
      | INDEPENDENT | Indonesia | DocumentNotAcceptedException  |
      | BUSINESS    | Indonesia | waiting for verification      |

  Scenario: A person cannot be added before verification
    Given Aina's eKYC is still waiting
    When the provider tries to add Aina
    Then it is rejected with MemberNotVerifiedException

  Scenario: A rejected eKYC adds nothing
    When Aina's eKYC is REJECTED
    Then the verification has ended
    And Aina is not on the team

  Scenario: A verification is used once
    Given Aina was added from this verification
    When the provider adds from it again
    Then it is rejected with MemberAlreadyAddedException
```

## Decided while building: provider types, team roles, documents, face check-in and adding a team member

These cover both sections above. All of them are confirmed by the product owner.

**`account`**

- **Results carry the document:** `EkycResult` is now a value: `verified(IdentityDocument)` or
  `rejected()`. A verified result must carry the document, which gives the registered name.
- **Countries:**
  - A country is an ISO 3166 two-letter code, such as `MY` or `ID`.
  - An accepted document is a holder (`PROVIDER`, `INDEPENDENT_TEAM_MEMBER`, `BUSINESS_TEAM_MEMBER`),
    a type, and a country, which is empty for any country.
- **The provider:**
  - `ProviderAccount.register` takes the type and no legal name; the registered name comes from the
    eKYC result.
  - *Superseded by "business verification through eKYB":* a business registration was first checked
    by Homa once. A business is now verified through eKYB, and Homa does no check of its own.
- **`MemberVerification`:**
  - Its statuses are `WAITING`, `VERIFIED`, `REJECTED` and `ADDED`.
  - The verified document must be the type and country chosen on the form, and still accepted.
    Otherwise `DocumentNotAcceptedException`, and it stays waiting.
  - A repeated result changes nothing (`UNCHANGED`). A different result after it has ended or been
    verified is refused (`VerificationNotWaitingException`).
  - It holds the `VerificationLimits` in force at the start, so the new member's due date comes from
    the verification time.
- **`TeamMember`:**
  - It keeps the document read at each pass, so a re-check can use a different accepted document.
  - The photo match is recorded once (`PhotoMatchAlreadyRecordedException`).
  - An independent provider's members must be Malaysian at every re-check too, whatever the list says.
- **Renamed exceptions:** `TeamMemberRemovedException`, `NotATeamMemberException`,
  `TeamMemberAlreadyVerifiedException` and `PhotoRequiredException`.
- **Accepted documents list (`policy`):** the list itself refuses a foreign document for providers
  or independent members (`ForeignDocumentNotAllowedException`), and needs an entry for every holder.

**`offering`**

- **The listing:**
  - `ProviderListing.draft` takes the provider type.
  - `setHelpers` no longer takes Homa limits, and `HomaLimits` holds hours only.
- **`TeamRoster`:** a person is never both a helper and a supervisor (`TeamMemberInTwoRolesException`).
- **The supervisor rule:**
  - `clearSupervisorRule()` removes the rule.
  - A listing that goes offline for want of a supervisor keeps its rule, so it goes live again once a
    supervisor is added, or after the rule is cleared.
  - The snapshot carries the rule as `Optional`.

**`reservation`**

- **Naming the team:**
  - `TeamAssignment` is helpers plus supervisors.
  - A supervisor must be on the roster as a supervisor (`SupervisorNotOnRosterException`).
  - Naming the same person twice is refused (`MemberNamedTwiceException`).
- **The location step:** `checkIn` is the location check, made once. `checkInRecord()` returns it,
  and `checkedInAt()` is when check-in completed.
- **Face checks:**
  - They come after the location check (`LocationNotCheckedException`).
  - Only a named person takes one (`MemberNotNamedException`), and someone who passed takes no other
    (`FaceCheckAlreadyPassedException`).
  - Check-in must complete within the check-in period. After that, a face check or an outage report
    is refused for a booking not yet checked in.
  - Once check-in is complete, the others' face checks can carry on.
- **Outages:**
  - A face check outage lets everyone named work, except someone who already failed every attempt.
  - An outage after check-in keeps the check-in time.
- **The customer no-show wait and cancelling:**
  - The wait runs from the completed check-in.
  - A cancellation after only the location check is not a customer no-show.

**`ie/`**

- **Events added:**
  - `ProviderRegisteredIE` gives offering the provider type.
  - `MemberVerificationEndedIE` was split into `MemberVerificationRejectedIE` and
    `MemberVerificationExpiredIE`, following the one-event-per-outcome rule. The expired event was
    removed later, with the time limit (see "verification on the spot").
- **Events renamed:** the remaining helper events are renamed for team members, since supervisors are
  members too: `TeamMemberRemovedIE`, `TeamMemberReverifiedIE` and `TeamMemberLapsedIE`. They carry
  `memberId` and the role.
- **New fields:**
  - The listing events carry `providerType`, `helperIds`, `supervisorIds`, `supervisorFromHelpers` and
    `supervisorFromHours`; a threshold that isn't set is 0.
  - `HomaPolicyChangedIE` drops the helper limits and adds `faceCheckAttempts` and
    `acceptedDocuments`, each written as "holder:type:country", with `*` for any country.
  - `TeamNamedIE` carries `helperIds` and `supervisorIds`.
- **53 events in all** at the time. `BusinessRegistrationCheckedIE` was removed later with business
  verification through eKYB, which makes 52.

**Decided:** when Homa learns a team member may not work, it removes the team member
(`TeamMember.remove`) and records what happened. Removing the provider was dropped from the spec: it is
provider suspension, which is not in the MVP.

## Decided while building: verification on the spot

> **Superseded in part** by "business providers only, verification by a KYC provider": the verification
> mechanics described here (documents, due-date maths, re-checks, face match attempts, the provider
> types) now belong to the KYC provider. The marketplace rules stay.

Adding a team member has no Homa time limit, as the product owner decided: the person verifies there
and then, on the provider's phone. All of these are confirmed by the product owner.

- **The time limit is gone:**
  - The 1-hour session expiry and the 7-day link expiry before it are removed: from `policy`, from
    `VerificationLimits` and from `HomaPolicyChangedIE`.
  - `MemberVerification.expire`, the `EXPIRED` status, `VerificationNotExpiredException` and
    `MemberVerificationExpiredIE` are removed too.
  - A waiting verification takes its eKYC result whenever it arrives.
- **Kenal owns the session and its timeout.** Kenal runs the on-the-spot session, times out one that
  is not finished, and always reports a final result:
  - A timed-out or abandoned session counts as a failed result, `EkycResult.rejected()`. For adding a
    member, the verification ends (`REJECTED`), nothing is added, and the provider starts again with a
    new add form. The domain needs no new state, and no verification stays waiting for ever.
  - The provider app can say "timed out" rather than "rejected" from Kenal's own session status. The
    rules are the same.
- **Re-verification works the same way.** A rejected, timed-out or abandoned session changes nothing,
  and the re-check stays open until the due date, as before. The provider starts another session with
  the person present.
- **A lost result is an infra concern.** If Kenal's result never arrives, the provider app or a
  scheduled job asks Kenal for the session's status. This is not a domain rule.
- **Domain rules unchanged:** that the person is present and uses the provider's phone is how the
  provider app runs the eKYC. The domain rules do not change.


## Change after the domain phase: business verification through eKYB

> **Superseded in part** by "business providers only, verification by a KYC provider": the verification
> mechanics described here (documents, due-date maths, re-checks, face match attempts, the provider
> types) now belong to the KYC provider. The marketplace rules stay.

Decided after the domain phase closed. A business provider is verified through **eKYB**. The eKYB
provider checks the business's SSM registration, and the owner's or director's eKYC is part of the same
flow. Homa does no check of its own, just as with eKYC. This supersedes the business registration check
by Homa in "provider types, team roles, documents and face check-in". Everything in this section, including the
rename of `ekycStatus()` to `verificationStatus()`, is confirmed by the product owner.

**`account`**

| Object | Kind | Responsibility |
|---|---|---|
| `EkybResult` | Value object | `verified(BusinessRegistration, IdentityDocument director)` or `rejected()` |
| `BusinessRegistration` | Value object | The SSM number and the registered business name, read by eKYB; kept for Homa's records |

```java
ProviderAccount.recordEkybResult(EkybResult result, AcceptedDocuments accepted, VerificationLimits limits, Instant now) : EkycOutcome
ProviderAccount.recordEkycResult(EkycResult result, AcceptedDocuments accepted, VerificationLimits limits, Instant now) : EkycOutcome   // independent only
ProviderAccount.verificationStatus() : VerificationStatus    // replaces ekycStatus(): eKYC or eKYB
```

- **The rules are the same as eKYC:**
  - A rejected first attempt can be redone.
  - A business is re-verified through eKYB every re-verification period, with the same reminder,
    early re-check and lapse rules. A business that misses its due date lapses, and its listing pauses.
  - The one event per outcome is the same too: `ProviderVerifiedIE`, `ProviderVerificationRejectedIE`,
    `ProviderReverifiedIE` and `RecheckFailedIE`.
- **The owner or director** must hold a document on the accepted list for providers: a Malaysian MyKad
  or passport (`DocumentNotAcceptedException`).
- **One flow per provider type:**
  - A business takes no separate eKYC result (`BusinessVerifiesThroughEkybException`).
  - An independent provider takes no eKYB result (`NotABusinessProviderException`).
- **Going live:** a business is ready once its eKYB is verified and its payout account is ready. There
  is no separate registration check.
- **No team at the start:** a business is never a helper on its own team (`BusinessIsNotAHelperException`).
  After eKYB it has no team members, and adds its helpers and supervisors, including an owner or
  director who works on site.
- **Name:** the provider page shows the profile's display name. The registered business name is kept
  in account, for Homa's records only.
- **Removed:**
  - `ProviderAccount.recordBusinessRegistrationChecked` and `BusinessRegistrationAlreadyCheckedException`.
  - `BusinessRegistrationCheckedIE`, which makes 52 events.
- **Providers:** the domain never names the eKYC or eKYB provider. Changing provider, or using
  different providers for eKYC and eKYB, changes only the infra adapters.

```gherkin
Feature: Business verification through eKYB
  Scenario: A business is verified through eKYB
    Given a business provider
    When its eKYB is VERIFIED with SSM number "202601012345" and director "Nur Aina binti Ahmad"
    Then the business is verified, with the SSM registration and the director's registered name
    And its re-verification is due 3 months later

  Scenario: A business has no team members after eKYB
    When a business's eKYB is VERIFIED
    Then the business is not a helper on its own team

  Scenario: A rejected eKYB can be redone
    Given a business's eKYB was REJECTED
    When its eKYB is VERIFIED on a later attempt
    Then the business is verified

  Scenario: The owner or director must be Malaysian
    When a business's eKYB is VERIFIED with an Indonesian passport for the director
    Then it is rejected with DocumentNotAcceptedException

  Scenario: A business does not take a separate eKYC
    When an eKYC result is recorded for a business
    Then it is rejected with BusinessVerifiesThroughEkybException

  Scenario: A business cannot go live before its eKYB passes
    Given a business with a ready payout account and no eKYB yet
    Then the business is not ready for go-live

  Scenario: A business is re-verified through eKYB
    Given a business verified on 1 January, with its re-check open on 18 March
    When its eKYB is VERIFIED on 20 March
    Then its re-verification is due on 20 June
```


## Change after the domain phase: business providers only, verification by a KYC provider

Decided after the domain phase closed. Homa's development focuses on value for homeowners and homecare
providers, so identity verification is handled entirely by a KYC provider (to be chosen; Persona and Innov8tif are
candidates). Homa has only business providers. Confirmed by the product owner.

**What moves to the KYC provider:** the verification onboarding pages, KYB with the owner's or director's
KYC, team members' KYC on the spot, the accepted documents, face matching (at onboarding and at
check-in, with its own attempt limit), re-verification schedules and reminders, early re-checks, and
session timeouts. Homa's requirements for it are in the product description, under External Services.

**What Homa keeps:** the marketplace rules that use the results. Only verified businesses and team
members are on Homa; the lawful-employment confirmation on the add form; a lapsed business is paused; a
job must end by the business's verified-until date; only team members can be named; and check-in
completes on the first face pass.

**`policy`:** the accepted documents list, the re-verification period, reminder and early re-check
deadline, and the face check attempts are removed. That makes 25 Homa-set values.

**`account`**

- `ProviderAccount` is a business only: no `ProviderType`, no eKYC, and no "helper on its own team".
  `recordKybResult(KybResult, now)` records the KYC provider's result, and `lapse()` records a missed
  re-verification it reports.
- `MemberVerification.start(providerId, role, photo, confirmation, now)` has no document type or country.
  `recordKycResult(KycResult, now)` records the result.
- `TeamMember.recordKycResult(KycResult, now)` records a re-verification, and `lapse()` a missed one.
- **The result rules,** the same for a business and a team member:
  - The first pass is `VERIFIED`, and a first failure is `REJECTED`, which can be redone. For a team
    member, a rejection ends the verification and nothing is added.
  - Once verified, a pass with a new verified-until date is `REVERIFIED`. A pass with the same date is
    `UNCHANGED`. A failure is `RECHECK_FAILED` and changes nothing: the KYC provider decides when to
    report a lapse.
  - A lapsed business or member who passes is `REVERIFIED` and back.
  - A verified result must carry the registered name (or the SSM registration) and the verified-until
    date (`VerificationDetailsRequiredException`).
- **Removed:**
  - `Country`, `IdentityDocument`, `AcceptedDocument(s)`, `DocumentHolder`, `DocumentType` and
    `DocumentRules`.
  - `EkycResult`, `EkybResult` and `EkycOutcome`, replaced by `KycResult`, `KybResult` and
    `VerificationOutcome`.
  - `Verification` and `VerificationLimits`.
  - `openRecheck`, `requestEarlyRecheck`, `recordPhotoMatch`, `includeSelfAsHelper` and `ProviderType`,
    with their exceptions.

**`offering`:** `ProviderType` is removed: `ProviderListing.draft(providerId)`, and the snapshot no
longer carries a provider type.

**`reservation`**

- `recordFaceCheck` takes the KYC provider's final result for each named person. `PASSED` lets them
  work, and the first pass completes check-in. `FAILED`, after the attempts the KYC provider allows,
  means they cannot work.
- Each person has one result (`FaceCheckAlreadyRecordedException`). `FaceCheckAttemptsUsedException`
  is removed.
- During an outage, everyone named works, except someone whose face check already failed.
- `PolicySnapshot` no longer carries face check attempts.

**`ie/`**

- `ProviderRegisteredIE` and `RecheckOpenedIE` are removed.
- `ProviderVerifiedIE` carries the SSM number and `verifiedUntil`, and `ProviderReverifiedIE` and
  `TeamMemberReverifiedIE` carry `verifiedUntil`.
- `RecheckFailedIE` carries no due date.
- `MemberVerificationStartedIE` carries only the role.
- `HomaPolicyChangedIE` and the listing events drop the removed values and the provider type.
- That makes 50 events.

```gherkin
Feature: A business provider is verified by the KYC provider
  Scenario: A business is verified through KYB
    Given a business provider
    When the KYC provider's KYB is VERIFIED until 1 April
    Then the business is verified until 1 April, with its SSM registration

  Scenario: A rejected KYB can be redone
    Given a business whose KYB was REJECTED
    When its KYB is VERIFIED on a later attempt
    Then the business is verified

  Scenario: A business is ready for go-live only when verified and payout-ready
    Given a business that is verified, with a ready payout account
    Then it is ready for go-live
    And a lapsed business is not

Feature: The KYC provider re-verifies a business
  Background:
    Given Kemas is verified until 1 April

  Scenario: A passed re-verification keeps the business verified until the new date
    When the KYC provider reports a pass until 20 June
    Then Kemas is verified until 20 June

  Scenario: A repeated pass changes nothing
    When the KYC provider reports a pass until 1 April again
    Then nothing changes

  Scenario: A failed re-verification changes nothing
    When the KYC provider reports a failed re-verification
    Then Kemas is still verified until 1 April

  Scenario: A lapsed business takes no new requests until it passes
    When the KYC provider reports a missed re-verification
    Then Kemas is lapsed and not ready for go-live
    And when it passes, Kemas is verified again

Feature: The KYC provider re-verifies a team member
  Background:
    Given Aina is a team member verified until 1 April

  Scenario: A passed re-verification keeps the member verified until the new date
    When the KYC provider reports a pass until 20 June
    Then Aina is verified until 20 June

  Scenario: A lapsed member leaves the team until they pass
    When the KYC provider reports a missed re-verification
    Then Aina is not on the team
    And when she passes, Aina rejoins the team

Feature: Face check at check-in by the KYC provider
  Scenario: A person whose face check failed cannot work
    Given Aina passed her face check
    When the KYC provider reports that Ben's face check FAILED
    Then Ben cannot work

  Scenario: Each person has one face check result
    Given Aina passed her face check
    When another result is reported for Aina
    Then it is rejected with FaceCheckAlreadyRecordedException
```

**Decided while building** (confirmed by the product owner):

- **The lapse comes from the KYC provider.** Homa no longer works out due dates. It keeps the
  verified-until date it is given: a job must end by it, and search uses it.
- **The registered name** follows the latest pass.
- **The lawful-employment confirmation** stays in Homa: it is Homa's terms, not an identity check.


## Change after the domain phase: ongoing monitoring and annual re-verification

Decided after the domain phase closed, and confirmed by the product owner. A business's registration
does not expire, but it can change. So the KYC provider:
- re-verifies businesses **every year**;
- re-verifies team members **every year and when their identity document expires**;
- **monitors** both in between, and tells Homa what it finds.

The cadence is the KYC provider's setting, so the domain needs no change for it. The monitoring signals
are new.

**`account`**

- `ProviderAccount.requireReverification(reason, deadline)` and
  `TeamMember.requireReverification(reason, deadline)` record "re-verification needed by a date", for
  example after a director change or before a document expires.
  - The business or person keeps working, and the verified-until date moves to the deadline if that is
    sooner, never later.
  - Only a verified business or a member on the team takes one (`ProviderNotVerifiedException`,
    `NotATeamMemberException`, `TeamMemberRemovedException`).
  - A reason and a deadline are required.
  - If they don't pass by the date, the KYC provider reports the lapse, as before.
- `ProviderAccount.recordSeriousFlag(reason)` records a serious flag, such as being struck off, wound up
  or dissolved, or a sanctions match.
  - The business lapses at once, from verified or already lapsed, and keeps the reason
    (`seriousFlag()`).
  - A later pass clears the flag.
  - A seriously flagged team member is lapsed with `TeamMember.lapse()`.

**`reservation`**

- `disputeForProviderFlag(now)`: when the provider's business is seriously flagged, every accepted
  reservation whose job is not completed moves to **Disputed**, with the ending `PROVIDER_FLAGGED`. The
  team cannot check in, and nobody files reports on it. A job whose completion was submitted is not
  disputed. *Changed in "aligning the product spec, domain plan and code": a reservation already disputed over the job, with no completion,
  is taken over by the flag too, and its open case is closed.*
- A job under way (checked in, not completed) is disputed too, and the team stops.
- The founders decide each one:
  - `cancelForProviderFlag(now)` cancels it, ending **Cancelled** with a full refund, no fine and no
    cancellation fee.
  - `restoreAfterFlagCleared(now)` returns it to **Reserved** when the KYC provider has cleared the
    flag. It works only before the booked start (`FlaggedReservationStartedException`), and the job
    goes ahead as booked.
- **The safe default:** `isFlagCancellationDue(now)` is true for a flagged reservation still undecided at
  its booked start, and Homa then cancels it with a full refund.
- Only a flagged reservation is cancelled or restored this way. `resolve` refuses it.

**`payment`**

- A payout to a seriously flagged business is **held** (`Payout.hold(reason)`), including those for jobs
  completed before the flag.
- The founders either `releaseHold()`, so it is paid out as normal, or `withhold(reason)` to keep it
  back.
- Only a payable payout is held, and only a held one is released or withheld. A reason is required.
  `isPayable()` says whether it can be paid out now.

**`ie/`:**
- `ReverificationRequiredIE`, `ProviderFlaggedIE`, `ReservationFlaggedIE`, `ReservationRestoredIE`,
  `PayoutHeldIE`, `PayoutHoldReleasedIE` and `PayoutWithheldIE` are added, which makes 57 events. offering pauses the listing on `ProviderFlaggedIE`, as on a lapse. reservation disputes
the upcoming accepted reservations.

```gherkin
Feature: Ongoing monitoring by the KYC provider
  Background:
    Given Kemas and Aina are verified until 1 April

  Scenario: A required re-verification moves the date sooner
    When the KYC provider asks Kemas to re-verify by 15 February, because a director changed
    Then Kemas is verified until 15 February and keeps working

  Scenario: A required re-verification never moves the date later
    When the KYC provider asks Kemas to re-verify by 1 May
    Then Kemas is still verified until 1 April

  Scenario: An expiring document moves a team member's date sooner
    When the KYC provider asks Aina to re-verify by 10 March, because her passport expires
    Then Aina is verified until 10 March and stays on the team

  Scenario: A serious flag lapses the business at once
    When the KYC provider reports that Kemas was struck off the SSM register
    Then Kemas is lapsed and not ready for go-live

  Scenario: A pass after a flag clears it
    Given Kemas was seriously flagged
    When its KYB is VERIFIED again
    Then Kemas is verified and no longer flagged

Feature: A seriously flagged provider
  Scenario: An accepted reservation is disputed when the provider is flagged
    Given a reserved booking with Kemas
    When Kemas is seriously flagged
    Then the reservation is Disputed

  Scenario: A flagged reservation is cancelled with a full refund and no fine
    Given a reservation disputed because Kemas was flagged
    When Homa cancels it
    Then the reservation is Cancelled and Sarah is refunded RM230.00

  Scenario: A completed job is not disputed for a flag
    Given Kemas submitted completion for a job
    When Kemas is seriously flagged
    Then it is rejected with CompletionAlreadySubmittedException

  Scenario: A job under way stops when the provider is flagged
    Given Aina checked in at 10:00
    When Kemas is seriously flagged at 11:00
    Then the reservation is Disputed

  Scenario: A cleared flag restores the reservation before its start
    Given a reservation disputed because Kemas was flagged
    When the KYC provider clears the flag and Homa restores it on Saturday at 09:59
    Then the reservation is Reserved again

  Scenario: A flagged reservation is not restored from its start
    Given a reservation disputed because Kemas was flagged
    When Homa tries to restore it on Saturday at 10:00
    Then it is rejected with FlaggedReservationStartedException

  Scenario: An undecided flagged reservation is cancelled at its start
    Given a reservation disputed because Kemas was flagged
    Then its cancellation is due from Saturday at 10:00, not at 09:59

Feature: A seriously flagged provider's payouts
  Scenario: A flagged provider's payout is held
    When Kemas is seriously flagged
    Then its payout of RM200.00 is held and not paid out

  Scenario: The founders release a held payout
    Given Kemas's payout is held
    When the founders release it
    Then it is paid out as normal

  Scenario: The founders withhold a held payout
    Given Kemas's payout is held
    When the founders withhold it with the reason "Confirmed sanctions match"
    Then it is not paid out
```

**Decided while building** (from the recommendation the product owner chose):

- The founders restore a flagged reservation only after the KYC provider clears the flag. The usecase
  checks the flag; the domain checks that the booked start hasn't come.
- A withheld payout stays with Homa; what happens to it next, such as with the authorities, is outside
  the domain.
- **A serious flag is a red-code event: Homa does not compromise.** Confirmed by the product owner.
  - **A job under way** is always cancelled with a full refund, and the provider earns nothing for the
    partial work.
  - **The timing rules:** restoring is possible only before the booked start. A flagged reservation is
    never a provider no-show, so there is no fine. A job whose completion was submitted stands, with its
    payout held.
  - **What the app must do:** tell the team on site to stop and leave, and tell the customer at the
    same moment. The `ProviderFlaggedIE` handler runs promptly (usecase rule R10).

## Change after the domain phase: getting ready for the usecase phase

Decided while reviewing the usecase plan, and confirmed by the product owner (usecase plan questions U2
to U5, U8 and U12). Three rules were missing from the domain, and five events from `ie/`.

**`offering`: a retired job or tool comes off the listings**

- `ProviderListing.withdrawJob(JobCode)` takes a job Homa retired off the listing.
  - A live listing left with no job is no longer live, and no longer paused. It is missing `JOBS`
    until the provider offers a job again and goes live.
  - A listing that still has a job stays as it was, live or paused.
  - A request already made keeps the offering it was made with, so it keeps the job.
- `ProviderListing.withdrawTool(ToolCode)` takes a retired tool off the listing. Bringing no tools is
  allowed, so the listing stays live.
- A job or tool the listing doesn't have changes nothing.

**`reservation`: a payment that fails at request**

- `Reservation.paymentFailed()` ends a request whose payment failed: the card was declined, or the FPX
  payment failed or was abandoned.
  - The reservation ends **Cancelled**, with the new ending `PAYMENT_FAILED`.
  - Nothing was charged, so there is no settlement, no fee and no fine.
  - Only a request can end this way (`InvalidStatusTransitionException`).

**`reservation`: a request is paid before the provider answers**

- `Reservation.confirmPayment(Money secured)` records the payment secured so far: the amount authorised
  on the card, or paid by FPX. Only a request takes one (`InvalidStatusTransitionException`).
- `Reservation.isPaymentSecured()` is true once that amount covers the total.
- `Reservation.accept` refuses a request whose payment isn't secured (`PaymentNotSecuredException`).
- An edit that raises the total makes the request wait for payment again, because the secured amount no
  longer covers it. An edit that lowers the total, or keeps it, stays paid.
- A confirmation for less than the total, such as one that arrives late for the total before an edit,
  is recorded and is not enough.
- Declining and expiring don't depend on payment.

**`ie/`: five events, which makes 62**

- `ServiceAreaPublishedIE` (catalogue): the cities Homa serves, for offering.
- `RequestCancelledIE` (reservation): the customer cancelled a request before the provider answered.
  payment handles it like a decline, because nothing is owed.
- `PaymentFailedIE` (payment): the payment failed at request; reservation ends the request.
- `PaymentSecuredIE` (payment): the card is authorised, or the FPX payment is paid, with the amount;
  reservation records it, and the provider can then answer.
- `SearchPerformedIE` (discovery): what was searched for and how many providers matched, with nothing
  about the customer. discovery gets its own event interface, `DiscoveryIE`.

```gherkin
Feature: Retired jobs and tools

  Background:
    Given Kemas's live listing offers Kitchen and Laundry fold, and brings a steam mop

  Scenario: A retired job comes off the listing
    When Homa retires Kitchen
    Then the listing offers Laundry fold only
    And it is still live

  Scenario: A live listing left with no job goes offline
    When Homa retires Kitchen and Laundry fold
    Then the listing is no longer live
    And it is missing "JOBS"

  Scenario: The listing goes live again once the provider offers another job
    Given Homa retired every job the listing offered
    When Kemas offers Toilet care and goes live
    Then the listing is live with Toilet care

  Scenario: A request already made keeps the retired job
    Given a request made while the listing offered Kitchen
    When Homa retires Kitchen
    Then the request still has Kitchen in its offering

  Scenario: A retired tool comes off the listing and it stays live
    When Homa retires the steam mop
    Then the listing brings no tools
    And it is still live

Feature: A payment that fails at request

  Scenario: A failed payment ends the request
    Given a request waiting for the provider
    When its payment fails
    Then the reservation is Cancelled with the ending "PAYMENT_FAILED"

  Scenario: The provider cannot accept a request whose payment failed
    Given a request whose payment failed
    When the provider accepts it
    Then it is refused with "InvalidStatusTransitionException"

  Scenario: Only a request ends for a failed payment
    Given a reserved booking
    When a failed payment is reported for it
    Then it is refused with "InvalidStatusTransitionException"
    And the booking stays Reserved

Feature: A request is paid before the provider answers

  Background:
    Given a request with a total of RM230.00

  Scenario: The provider cannot accept a request before its payment is secured
    Given no payment is secured yet
    When the provider accepts it
    Then it is refused with "PaymentNotSecuredException"
    And it stays Requested

  Scenario: The provider accepts once the payment is secured
    Given RM230.00 is secured
    When the provider accepts it
    Then the reservation is Reserved

  Scenario: A payment below the total is not enough
    Given RM229.99 is secured
    When the provider accepts it
    Then it is refused with "PaymentNotSecuredException"

  Scenario: An edit that raises the total waits for payment again
    Given RM230.00 is secured
    When the customer edits the request to 4 hours, a total of RM299.00
    Then the provider cannot accept it until RM299.00 is secured

  Scenario: An edit that lowers the total stays paid
    Given RM230.00 is secured
    When the customer edits the request to 1 helper
    Then the provider can still accept it
```

**Decided while building:**

- **A retired tool** follows the same rule as a retired job: it comes off the listings. The usecase
  question asked only about jobs.
- **No event after a failed payment.** `PaymentFailedIE` already says what happened, so reservation ends
  the request and publishes nothing more.
- **`RequestCancelledIE` replaces `ReservationCancelledIE` and `SettlementDecidedIE`** for a request the
  customer cancels. A request holds no slot and owes nothing, so one event is enough, as for a decline
  or an expiry.
- **The secured amount, not a yes or no.** `confirmPayment` takes the amount, so the reservation itself
  knows when an edit has outgrown the payment, and a late confirmation for an old total can't unlock an
  edited request.
- **A failed payment after an edit ends the request**, like any failed payment (`paymentFailed`). The
  customer can request again.

## Change after the domain phase: changes to the job and tool lists

Decided while building the `catalogue` usecases, and confirmed by the product owner. A listing keeps
only job and tool **codes**, so what happens to a provider depends on whether Homa's change keeps the
code.

| Homa's change | The provider's listing | Requests and reservations already made |
|---|---|---|
| Add a job or tool | Nothing; the provider picks it if they want it | Unaffected |
| Rename, or move a job to another group | Keeps it; only the name or group shown changes | Unaffected |
| Retire a job | It comes off. A live listing left with no job goes offline until the provider offers another job and goes live again | Keep it |
| Retire a tool | It comes off; the listing stays live | Unaffected |
| Replace a job | A retire plus an add with a new code; the provider picks the new job themselves | Keep the old job |

**`catalogue`**

- `JobList.regroup(code, group)` moves a job to another display group. Its code and name stay.
- Featuring a search takes the jobs providers can offer now and refuses any other
  (`FeaturedSearchJobNotOfferedException`, naming the first one alphabetically). A search can't be
  featured for a job that doesn't exist or is retired.
- `Showcase.withdrawJob(code)` takes a retired job off every featured search that has it and says
  whether any did. One left with no job is no longer shown (`isShown()`), and keeps its last search so
  the founders can see what it was.

**`offering`**

- `withdrawJob` now says what it did (`JobWithdrawal`): `NOT_OFFERED`, `WITHDRAWN`, or
  `WITHDRAWN_AND_OFFLINE` when it took a live listing's last job. A draft losing its last job was
  never live, so it is `WITHDRAWN`.
- `withdrawTool` says whether the provider brought the tool.

**`ie/`: six events, which makes 68**

- `JobRenamedIE`, `JobRegroupedIE` and `ToolRenamedIE` (catalogue), so a context that shows names or
  groups updates them.
- `JobWithdrawnFromListingIE`, `ListingWentOfflineForRetiredJobIE` and `ToolWithdrawnFromListingIE`
  (offering), so the provider is told what a retirement did to their listing and, when it went
  offline, to offer another job and go live again.

```gherkin
Feature: Changes to the job list

  Scenario: A job moves to another group and keeps its code and name
    Given the job list has "SOFA_VACUUM" in Kitchen
    When Homa moves it to Living room
    Then it is in Living room with the same code and name
    And providers can still offer it

  Scenario: Only a job on the list can be regrouped
    When Homa moves "SOFA_VACUUM", which is not on the list
    Then it is refused with "JobNotFoundException"

Feature: Featured searches search only for jobs providers can offer

  Scenario: A featured search searches only for jobs providers can offer
    When Homa features a search for Kitchen and Garden, and Garden is not on the job list
    Then it is refused with "FeaturedSearchJobNotOfferedException"

  Scenario: A retired job comes off the featured searches that search for it
    Given a featured search for Kitchen and Laundry fold
    When Homa retires Kitchen
    Then it searches for Laundry fold only
    And it is still shown

  Scenario: A featured search left with no job is no longer shown
    Given a featured search for Kitchen only
    When Homa retires Kitchen
    Then it is no longer shown, and keeps its place and its last search

Feature: Telling the provider what a retirement did

  Scenario: Withdrawing a job says the listing is still live
    Given Kemas's live listing offers Kitchen and Laundry fold
    When Kitchen comes off
    Then the withdrawal is "WITHDRAWN"

  Scenario: Withdrawing the last job says the listing went offline
    Given Kemas's live listing offers only Laundry fold
    When Laundry fold comes off
    Then the withdrawal is "WITHDRAWN_AND_OFFLINE"

  Scenario: A draft losing its last job was never live
    Given a draft listing that offers only Laundry fold
    When Laundry fold comes off
    Then the withdrawal is "WITHDRAWN"
```

**Decided while building:**

- **A separate event for a regroup.** One event per outcome: `JobRegroupedIE`, not a rename event that
  also carries the group.
- **The founders see hidden featured searches.** `HomaViewShowcase` lists every featured search with
  whether it is shown, so one emptied by a retirement can be replaced. Customers see only shown ones.
- **The listing events stay as they are.** reservation and discovery still get `ListingChangedIE` or
  `ListingWentOfflineIE`; the three new offering events are for telling the provider why.

## Change after the domain phase: the homepage showcase and changes that change nothing

Found while reviewing the `catalogue` and `policy` usecases, and confirmed by the product owner.

**`catalogue`**

- **Shelves are now featured searches in a showcase.** What the homepage shows is a search Homa
  features, so the names say so: a `FeaturedSearch` (once `Shelf`) in the `Showcase` (once
  `Homepage`). The app may still draw each one as a row; in the code and the docs it is a featured
  search.
- **A featured search fills in the search; it is not a search of its own.** Tapping one fills in
  discovery's ordinary search with its jobs, hours and helpers. The customer picks the date and time,
  and the city comes from their address.
- **The `Showcase` owns its featured searches and their order.** A shelf's place was a number on each
  shelf, so two could share one and their order was then arbitrary. Now one aggregate holds them all,
  so each change is one save:
  - `Showcase.feature(title, preset, position, offeredJobs)` features a new search at a position from 1
    to one past the last; the ones from there move down one.
  - `Showcase.move(id, position)` moves one from 1 to the last place; the ones in between move along.
    It says whether it moved.
  - `Showcase.remove(id)` takes one off; the ones after it move up.
  - `Showcase.rename(id, title)` gives one a new title.
  - Only a featured search in the showcase is renamed, moved or removed
    (`FeaturedSearchNotInShowcaseException`).
- **Changes that change nothing say so.** `JobList.rename`, `JobList.regroup`, `ToolList.rename` and
  `Showcase.rename` return whether anything changed, so the usecase saves, tells and records nothing for
  a rename to the same name.

- **A repeated payout account report changes nothing.** Found while building the `account` usecases:
  `ProviderAccount.recordPayoutAccountReady()` returns whether it changed anything, so a repeated report
  from the payment provider is told to the other contexts only once.

```gherkin
Feature: The order of the homepage showcase

  Background:
    Given the showcase features A, B and C, in that order

  Scenario: A search featured in the middle moves the rest down one
    When Homa features a new search at position 2
    Then the order is A, new, B, C

  Scenario: A search is featured from the first place to one past the last
    When Homa features a new search at position 5
    Then it is refused with "InvalidShowcasePositionException"

  Scenario: Only a featured search in the showcase moves
    When Homa moves a search that is not featured
    Then it is refused with "FeaturedSearchNotInShowcaseException"

  Scenario: A search moved up moves the ones in between down
    When Homa moves C to position 1
    Then the order is C, A, B

  Scenario: Moving a search to its own place changes nothing
    When Homa moves B to position 2
    Then nothing moved

  Scenario: A removed search leaves no gap
    When Homa removes A
    Then the order is B, C

Feature: Changes that change nothing

  Scenario: Renaming a job to its own name changes nothing
    Given the job list has "KITCHEN_GREASE" named "Job KITCHEN_GREASE"
    When Homa renames it "Job KITCHEN_GREASE"
    Then nothing changed

  Scenario: Moving a job to its own group changes nothing
    Given "SOFA_VACUUM" is in Kitchen
    When Homa moves it to Kitchen
    Then nothing changed
```

## Change after the domain phase: aligning the product spec, domain plan and code

Found by comparing the product spec, this plan and the domain code rule by rule, and confirmed by the
product owner. Each decision says which source it follows: the **spec**, this **plan**, the **code**, or
a **new** rule. The product description states every one of them.

| # | Decision | Follows | Domain change |
|---|---|---|---|
| 1 | A challenged customer no-show ends with the no-show upheld, part of the fee to the provider, or a full refund | Code | None |
| 2 | A flagged reservation is restored to Reserved if the flag is cleared before the booked start | Code | None |
| 3 | A customer who cancels after check-in pays the customer no-show fee, released straight away and not challengeable | Code | None |
| 4 | When Homa narrows its duration range, listings' hours shrink to fit; a listing with none left goes offline. A request also checks Homa's range | New | offering, reservation |
| 5 | A business adds team members only once its KYB has passed and while it hasn't lapsed | Spec | account |
| 6 | Book again from any past enquiry or reservation, at the customer's current address | New | reservation |
| 7 | The address can't be changed on a request | Spec | reservation |
| 8 | Flagged businesses are listed in the founders' queue but are not cases | Code | None |
| 9 | A flag disputes a reservation whose completion hasn't been submitted | Plan, code | None |
| 10 | A third report type: safety, damage or loss, first in the queue | New | reservation, resolution |
| 11 | A lapsed or removed person comes off every upcoming job's team; during a job they stop working | New | reservation |
| 12 | Completion can be submitted while the reservation is disputed over the job | New | reservation |
| 13 | A flag takes over a job dispute that has no completion; its open case is closed | New | reservation, resolution |
| 14 | A withheld payout stays with Homa as owed, never revenue; the customer isn't refunded | New (spec only) | None |
| 15 | A live listing with a supervisor rule and no supervisor left goes offline | Code | None |
| 16 | A close match differs in one way only, and a suggested start is bookable | New | discovery, availability |
| 17 | Accepting a request declines the provider's other open requests that can no longer be accepted | New | reservation |
| 18 | A customer no-show can be reported up to and including the booked end | New | reservation |
| 19 | Customers can change their own name | New | account |
| 20 | Requesting opens the reservation's chat with no message; both can write | New | conversation |
| 21 | "Jobs done" and "cancellations" on the provider page are defined | New (read model) | None |
| 22 | A reservation decided with a full refund can still be rated | Spec, plan, code | None |

**`account`**

- `CustomerAccount.rename(name)` changes the customer's name and returns whether it changed. A blank
  name is refused (`AccountDetailRequiredException`, as at registration).
- `MemberVerification.start(business, role, photo, confirmation, now)` takes the `ProviderAccount` and
  refuses a business that is not `VERIFIED` (pending, rejected or lapsed) with
  `ProviderNotVerifiedException`. The provider id comes from the business.

**`offering`**

- `ProviderListing.fitHours(HomaLimits limits) : HoursFit` keeps the listing's hours inside Homa's
  duration range after Homa changes it. Like the helper range, it only ever shrinks:
  - `NOT_AFFECTED`: no hours set, or the hours are already inside.
  - `NARROWED`: the hours shrink to the overlap, for example 2 to 6 becomes 2 to 4 under 1 to 4.
  - `CLEARED`: no overlap is left, so the hours are cleared and the listing needs them again (a draft).
  - `CLEARED_AND_OFFLINE`: the same for a live listing, which stops being live, as with no helper left.
- A wider range changes nothing: the provider sets their hours again to use it.

**`availability`**

- `nearestAvailableStart(wanted, within, earliest, latest)` returns only starts from `earliest` to
  `latest`: the minimum notice and the booking horizon from now. A start outside them is never
  suggested.

**`discovery`**

- `SearchCriteria` keeps its earliest and latest bookable starts, worked out in `SearchCriteria.of`.
- `MatchClassifier` gives `CLOSE` for exactly one difference, `MISSING_JOBS` or `NEARBY_TIME`. Both
  together are `NONE`, with both reasons. A nearest free start outside the bookable starts counts as
  `NOT_FREE`.

**`reservation`**

- **Homa's range at request (4):** reservation's `PolicySnapshot` gains Homa's duration range
  (`minHours`, `maxHours`, `allowsHours`). `request` and `edit` refuse hours outside it
  (`HoursOutsideHomaRangeException`), as well as outside the provider's.
- **Book again (6):** `Enquiry.bookAgain(customerId, providerId, pastDetails, currentAddress, newStart,
  now)` creates an enquiry with the past jobs, hours and helpers, the current address and the new start
  (`ReservationDetails.forBookingAgain`, which replaces `startingAt`). The usecase passes the details of
  a past enquiry or reservation. `bookAgainFrom(Enquiry, ...)` is removed.
- **Address (7):** `edit` refuses details with a different address (`AddressNotEditableException`).
- **Safety reports (10):** `ReportType` gains `SAFETY_DAMAGE_OR_LOSS`. It follows every other report
  rule: one per party, from the booked start until the dispute window ends, and it moves the
  reservation to Disputed.
- **A person comes off the team (11):** `dropFromTeam(memberId, now) : TeamDrop`.
  - `NOT_AFFECTED`: they weren't named, or the reservation isn't reserved or disputed over the job, so
    nothing changes.
  - `REMOVED`: before the booked start, they come off the named team and it is still complete, for
    example an optional supervisor.
  - `TEAM_INCOMPLETE`: before the booked start, they come off the named team. The team stays named
    with the others, but `isTeamComplete()` (as many helpers as booked, and a supervisor when the rule
    requires one) is false until the provider names it again with `nameTeam`. While it is incomplete, `checkIn` refuses (`TeamNotNamedException`) and
    `isTeamReminderDue` is true from the team reminder time. A team still incomplete at the end of the
    check-in period gives a provider no-show, as today.
  - `CANNOT_WORK`: from the booked start the team is locked, so they stay named but `canWork` is false
    for them, the same as after a failed face check.
- **Completion during a job dispute (12):** `submitCompletion` also works while the reservation is
  Disputed over the job (a report or a missed completion deadline). The status stays Disputed. It still
  needs a check-in and evidence, and still happens once. A challenged no-show or a flagged reservation
  refuses it.
- **A flag takes over a job dispute (13):** `disputeForProviderFlag` also works on a reservation
  Disputed over the job with no completion submitted. The ending becomes `PROVIDER_FLAGGED` and its
  reports are kept. A reservation whose completion was submitted, or a challenged no-show, is refused
  as today.
- **Taken time (17):** `declineForTakenTime(now)` ends an open request as **Declined**, with the ending
  `TIME_TAKEN`, so tracking can leave it out of the provider's decline rate. The usecase calls it for
  each of the provider's open requests that availability says is no longer free once another is
  accepted. It doesn't wait for the request expiry.
- **No-show deadline (18):** `reportCustomerNoShow` refuses after the booked end
  (`NoShowReportTooLateException`). The end itself is still in time.

**`resolution`**

- **Safety first (10):** `Case.markSafety()` marks a case about safety, damage or loss, when a safety
  report opens it or joins it. `Case.QUEUE_ORDER` puts safety cases first, then the oldest first.
- **Closed by a flag (13):** `Case.closeForProviderFlag(now)` closes an open case with no decision
  (`CLOSED`) and records `closedAt`. A closed case can't be decided or take evidence (`CaseNotOpenException`).

**`conversation`**

- **Requesting opens the chat (20):** `Conversation.openForReservation(reservationId, customerId,
  providerId, now)` opens a reservation's chat with no message. The customer's request opened it, so
  "only the customer starts a chat" still holds; once it is open, either party can post. A chat
  started from an enquiry is linked as today, so a reservation never has two chats.

**`ie/`**

- `CustomerRenamedIE`, `RequestDeclinedForTakenTimeIE`, `TeamMemberDroppedIE`, `TeamMemberStoppedIE`,
  `CaseClosedForProviderFlagIE`, `ListingHoursNarrowedIE` and `ListingWentOfflineForHoursIE` are added,
  which makes 75 events. A person coming off the team is two events, one per outcome: dropped before
  the start (with whether the team is still complete), or stopped from the start.
- `ReportFiledIE` already carries the report type, so resolution can mark a safety case.
- `EnquiryCreatedIE` comes from `Enquiry.bookAgain` instead of `bookAgainFrom`.
- offering listens to `HomaPolicyChangedIE` to fit the hours.

**Spec and plan only:** decisions 1, 2, 3, 8, 9, 14, 15, 21 and 22 match the code already. #21 is worked
out by the provider page's read model from the reservation and fine events. #14 needs no method:
`Payout.withhold` is final in Homa.

```gherkin
Feature: Homa narrows its duration range

  Scenario Outline: A listing's hours shrink to fit
    Given a live listing with hours <hours>
    When Homa's duration range becomes <range>
    Then the result is <result>
    And the listing's hours are <after>
    Examples:
      | hours  | range  | result              | after  |
      | 2 to 6 | 1 to 4 | NARROWED            | 2 to 4 |
      | 2 to 4 | 1 to 4 | NOT_AFFECTED        | 2 to 4 |
      | 2 to 4 | 1 to 6 | NOT_AFFECTED        | 2 to 4 |
      | 5 to 6 | 1 to 4 | CLEARED_AND_OFFLINE | none   |

  Scenario: A draft left with no hours needs them again
    Given a draft listing with hours 5 to 6
    When Homa's duration range becomes 1 to 4
    Then the result is CLEARED
    And the listing is missing HOURS for go-live

  Scenario: A request must fit Homa's range too
    Given a provider with hours 1 to 6 and Homa's duration range 1 to 4
    When the customer requests 5 hours
    Then it is rejected with HoursOutsideHomaRangeException

Feature: Adding team members needs a verified business

  Scenario Outline: Only a verified business adds team members
    Given a business whose verification is <status>
    When the provider starts adding a helper
    Then the result is <result>
    Examples:
      | status   | result                       |
      | VERIFIED | waiting for verification     |
      | PENDING  | ProviderNotVerifiedException |
      | REJECTED | ProviderNotVerifiedException |
      | LAPSED   | ProviderNotVerifiedException |

Feature: Customers change their name

  Scenario: A customer changes their name
    Given a customer named "Siti"
    When they change their name to "Siti Aminah"
    Then their name is "Siti Aminah"

  Scenario: Changing to the same name changes nothing
    Given a customer named "Siti"
    When they change their name to "Siti"
    Then nothing changed

Feature: Book again

  Scenario: Booking again keeps the job and uses the current address
    Given a past reservation with provider Kemas for KITCHEN, 3 hours, 2 helpers at "1 Jalan Ara"
    And the customer now lives at "8 Jalan Bunga"
    When the customer books again for next Saturday 10:00
    Then a new enquiry with Kemas has KITCHEN, 3 hours, 2 helpers, next Saturday 10:00 at "8 Jalan Bunga"

Feature: The address can't be edited on a request

  Scenario: Editing a request to another address is refused
    Given a REQUESTED reservation at "1 Jalan Ara"
    When the customer edits it to "8 Jalan Bunga"
    Then it is rejected with AddressNotEditableException

Feature: A person who lapses or is removed comes off the team

  Background:
    Given a RESERVED reservation for Saturday 10:00 to 13:00 for 2 helpers
    And the provider named Aina and Ben

  Scenario: Before the start, the team is incomplete until named again
    When Ben comes off the team on Friday 18:00
    Then the team is not complete
    And the team reminder is due at Saturday 04:00
    And check-in at Saturday 09:50 is rejected with TeamNotNamedException

  Scenario: Naming someone in their place completes the team
    Given Ben came off the team on Friday 18:00
    When the provider names Aina and Chan on Friday 19:00
    Then the team is complete

  Scenario: A team still incomplete at the end of the check-in period is a provider no-show
    Given Ben came off the team on Friday 18:00
    When Homa marks a provider no-show at Saturday 10:16
    Then the reservation is CANCELLED
    And the provider is fined RM54.00

  Scenario: During the job, they stop working
    Given the provider checked in at 10:00 and Aina and Ben passed their face checks
    When Ben comes off the team at Saturday 11:00
    Then Ben cannot work
    And the check-in still stands

  Scenario: Someone not named changes nothing
    When Dina comes off the team
    Then the result is NOT_AFFECTED

  Scenario: An optional supervisor comes off and the team stays complete
    Given the provider also named Siti as a supervisor, with no supervisor rule
    When Siti comes off the team on Friday 18:00
    Then the result is REMOVED
    And the team is complete

Feature: Completion while disputed over the job

  Background:
    Given a RESERVED reservation for Saturday 10:00 to 13:00 and the provider checked in at 10:00

  Scenario: The provider submits completion after the customer reports
    Given the customer filed a DURING_JOB report at Saturday 11:00
    When the provider submits completion at Saturday 12:45 with a photo
    Then the completion is recorded
    And the reservation is still DISPUTED
    And the provider can file their report until Saturday 14:00

  Scenario: The provider submits completion after missing the deadline
    Given the completion deadline was missed at Sunday 13:00
    When the provider submits completion at Sunday 14:00 with a photo
    Then the completion is recorded
    And the reservation is still DISPUTED

Feature: A flag takes over a job dispute

  Scenario: A disputed job with no completion is taken over
    Given a reservation DISPUTED over a customer report filed at Saturday 10:30, with no completion
    When the provider's business is seriously flagged at Saturday 11:00
    Then the reservation is disputed for the flag
    And the customer's report is kept
    And cancelling it for the flag refunds the customer RM230.00

  Scenario: A disputed job with completion is not taken over
    Given a reservation DISPUTED over a report, with completion submitted
    When the provider's business is seriously flagged
    Then it is rejected with CompletionAlreadySubmittedException

  Scenario: A case taken over by a flag is closed
    Given an OPEN case for that reservation
    When the case is closed for the provider flag
    Then the case is CLOSED with no decision
    And deciding it is rejected with CaseNotOpenException

Feature: Safety, damage or loss reports

  Scenario: The customer reports a safety problem
    Given a RESERVED reservation for Saturday 10:00 to 13:00
    When the customer files a SAFETY_DAMAGE_OR_LOSS report at Saturday 11:00
    Then the reservation is DISPUTED

  Scenario: Safety cases come first in the queue
    Given a case opened Friday and a safety case opened Saturday
    Then the queue lists the safety case first

  Scenario: A safety report joining a case marks it
    Given an OPEN case from a FINISHED_WORK report
    When the provider's SAFETY_DAMAGE_OR_LOSS report joins it
    Then the case is about safety

Feature: A challenged no-show can end with part of the fee

  Scenario: Homa gives the provider part of the no-show fee
    Given a customer no-show challenged by the customer, with a no-show fee of RM74.00 and a total of RM230.00
    When Homa decides the provider receives RM40.00
    Then the reservation is CANCELLED
    And the provider earns RM40.00
    And Homa's service fee is RM6.00
    And the customer is refunded RM184.00

Feature: The customer no-show deadline

  Background:
    Given a RESERVED reservation for Saturday 10:00 to 13:00 and the provider checked in at 10:00

  Scenario Outline: A customer no-show is reported by the booked end
    When the provider reports a customer no-show at <time>
    Then the result is <result>
    Examples:
      | time           | result                       |
      | Saturday 13:00 | CANCELLED                    |
      | Saturday 13:01 | NoShowReportTooLateException |

Feature: Requests declined when the time is taken

  Scenario: An open request is declined when another booking takes its time
    Given a REQUESTED reservation that expires Friday 09:00
    When it is declined for taken time at Thursday 15:00
    Then the reservation is DECLINED with the ending TIME_TAKEN

  Scenario: Only a request can be declined for taken time
    Given a RESERVED reservation
    When it is declined for taken time
    Then it is rejected with InvalidStatusTransitionException

Feature: Close matches differ in one way only

  Background:
    Given it is Thursday 09:00, the minimum notice is 2 hours
    And the customer searches for KITCHEN and LAUNDRY_FOLD on Saturday 10:00

  Scenario Outline: One difference is close, two are no match
    Given a provider offering <jobs> who is free from <free>
    Then the provider is <match> with reasons <reasons>
    Examples:
      | jobs                  | free           | match | reasons                   |
      | KITCHEN, LAUNDRY_FOLD | Saturday 10:00 | EXACT | none                      |
      | KITCHEN               | Saturday 10:00 | CLOSE | MISSING_JOBS              |
      | KITCHEN, LAUNDRY_FOLD | Saturday 11:00 | CLOSE | NEARBY_TIME               |
      | KITCHEN               | Saturday 11:00 | NONE  | MISSING_JOBS, NEARBY_TIME |

  Scenario: A suggested start must be bookable
    Given the customer searches for Thursday 12:00
    And the only start near it when the provider is free is Thursday 10:30, before the minimum notice
    Then the provider is NONE with reason NOT_FREE

Feature: Requesting opens the reservation's chat

  Scenario: A direct request opens the chat with no message
    When the customer's request opens the reservation's chat
    Then the chat belongs to the reservation and has no messages

  Scenario: The provider can write first in a reservation's chat
    Given the reservation's chat was opened by the request
    When the provider posts "Running 10 minutes late"
    Then the chat has 1 message
```

## Change after the domain phase: the 7-star experience

Decided by the product owner after a product critique of the spec. Homa targets **7 stars** for its core
moment, "I booked help, and it turned up": customers know at once or by a set time that they are booked,
the team gets in, customers can leave after check-in, and booking the same provider again takes one
tap. The product description's new Part 1 (product brief) states the target, the principles, and the
goals and measures; Part 2 states the rules below.

| # | Decision | Domain change |
|---|---|---|
| S1 | Guests search by a city or a pin; the account is asked for at Reserve or Chat | None: search already takes a city |
| S2 | Each provider chooses an **acceptance policy**: Request and accept (the default) or **Instant book**, which accepts a request the moment its payment is secured. The provider can't decline an instant-book request | offering, reservation |
| S3 | A **response deadline** (1 hour before the booked start): a request expires by then at the latest. It must be shorter than the minimum notice | policy, reservation |
| S4 | Evidence is kept 120 days; a card chargeback opens a case, and what the provider must cover is recovered like a fine | policy (value), resolution, payment |
| S5 | Recurring bookings and same-team requests are listed as cut from the MVP | None |
| S6 | The dispute window extension is 24 hours; the customer is home for check-in and free to leave after | policy (value) |
| S7 | The address is confirmed with a map pin; the account keeps **access notes**, copied to each request and changeable until completion | account, reservation |
| S8 | A card edit replaces the authorisation; Homa pays processing fees; receipts and statements; tax checked before launch | None |
| S9 | Homa aims to decide each case within 3 working days, shown to both parties | None: the queue already has each case's age |
| S10 | A provider can declare liability insurance, shown as an **Insured** badge until it expires | offering |
| S11 | The apps are in Bahasa Melayu and English | None |
| S12 | The spec is split into a product brief (Part 1) and product rules (Part 2) | None |

**`policy`**

- A new value, `ResponseDeadline(Duration beforeStart)`, 1 hour at launch, which makes 26 values.
- Launch values change: the dispute window extension becomes 24 hours, and evidence retention 120 days.
- A cross-value rule: the response deadline must be shorter than the minimum notice, so a request is
  never made after its own deadline (`ResponseDeadlineNotBeforeMinimumNoticeException`).

**`offering`**

- `AcceptancePolicy`: `REQUEST_AND_ACCEPT` (the default for every listing) or `INSTANT_BOOK`.
  `ProviderListing.chooseAcceptancePolicy(policy)` sets it, and the snapshot carries it.
- `InsuranceDeclaration(insurer, validUntil)`. `ProviderListing.declareInsurance(declaration, now)`
  refuses one already expired (`InsuranceExpiredException`); `clearInsurance()` removes it;
  `isInsured(now)` is true until `validUntil`. Homa doesn't check the policy.

**`account`**

- `AccessNotes`: free text for the team, up to 500 characters; blank means none.
  `CustomerAccount.changeAccessNotes(text)` returns whether it changed (`AccessNotesTooLongException`).

**`reservation`**

- **Response deadline (S3):** the `PolicySnapshot` gains `responseDeadline`. `requestExpiresAt()` is the
  earlier of the request expiry time after the request and the booked start minus the response
  deadline.
- **Instant book (S2):** the `OfferingSnapshot` gains its `AcceptancePolicy`.
  - `acceptInstantly(providerVerifiedUntil, now)` accepts an instant-book request with the same checks as
    `accept`: payment secured, not expired, and the job ends by the provider's verified-until date. It
    refuses a request-and-accept listing's request (`NotInstantBookException`).
  - `decline` refuses an instant-book request (`InstantBookCannotBeDeclinedException`).
    `declineForTakenTime` still ends one whose time is no longer free.
- **Access notes (S7):** `changeAccessNotes(text, now)` sets the reservation's notes while it is
  requested, or reserved or disputed over the job with no completion. The usecase copies the account's
  notes right after the request. The provider sees them from acceptance.

**`resolution`**

- `CaseSource.CHARGEBACK`: the amount at stake is what the customer's bank took back.
  `Case.providerCovers()` is what the provider must cover under the decision: the amount at stake less
  what the provider receives (all of it for a full refund, none for a release).

**`payment`**

- `FineReason.CHARGEBACK`. What a provider covers is charged as a `Fine` and deducted from their next
  completed-job payouts within the fine deduction cap, oldest first, like any fine.

**`ie/`**

- `HomaPolicyChangedIE` carries `responseDeadline`; `ListingWentLiveIE` and `ListingChangedIE` carry
  `instantBook`; `ReservationAcceptedIE` carries `instantBook`.
- Added: `CustomerAccessNotesChangedIE` (account, for reservation's projection),
  `AccessNotesChangedIE` (reservation, for the provider), `ProviderInsuranceDeclaredIE` and
  `ProviderInsuranceClearedIE` (offering, for the provider page) and `ChargebackDecidedIE`
  (resolution, for payment). That makes 80 events.

**Test values:** the reservation tests keep a dispute window extension of 1 hour and their worked times,
as test values; the launch value is 24 hours. The policy tests check the launch values.

```gherkin
Feature: The response deadline

  Scenario Outline: A request is answered by the response deadline at the latest
    Given a request made at <requested> for a start at <start>
    Then the request expires at <expires>
    Examples:
      | requested      | start          | expires        |
      | Thursday 09:00 | Saturday 10:00 | Friday 09:00   |
      | Saturday 06:00 | Saturday 10:00 | Saturday 09:00 |

  Scenario: The response deadline is shorter than the minimum notice
    Given the minimum notice is 2 hours
    When Homa changes the response deadline to 2 hours
    Then it is rejected with ResponseDeadlineNotBeforeMinimumNoticeException

  Scenario: The launch values give time back
    Then the dispute window extension is 24 hours
    And evidence is kept for 120 days
    And the response deadline is 1 hour

Feature: Instant book

  Scenario: A listing accepts by request unless the provider chooses Instant book
    Given a new listing
    Then its acceptance policy is REQUEST_AND_ACCEPT
    When the provider chooses INSTANT_BOOK
    Then the listing's snapshot says INSTANT_BOOK

  Scenario: An instant-book request is accepted once it is paid
    Given a request to an Instant book provider whose payment is secured
    When it is accepted instantly
    Then the reservation is RESERVED

  Scenario: An instant-book request is not accepted before it is paid
    Given a request to an Instant book provider whose payment is not secured
    When it is accepted instantly
    Then it is rejected with PaymentNotSecuredException

  Scenario: Only an instant-book request is accepted instantly
    Given a request to a Request and accept provider
    When it is accepted instantly
    Then it is rejected with NotInstantBookException

  Scenario: The provider can't decline an instant-book request
    Given a request to an Instant book provider
    When the provider declines it
    Then it is rejected with InstantBookCannotBeDeclinedException

  Scenario: An instant-book request whose time is taken still ends
    Given a request to an Instant book provider
    When it is declined for taken time
    Then the reservation is DECLINED with the ending TIME_TAKEN

Feature: Access notes

  Scenario: A customer keeps access notes on their account
    When the customer saves "Unit 12-3, register at the guardhouse"
    Then their access notes are "Unit 12-3, register at the guardhouse"

  Scenario: Access notes are at most 500 characters
    When the customer saves 501 characters of access notes
    Then it is rejected with AccessNotesTooLongException

  Scenario: A reservation's notes change until the job is completed
    Given a RESERVED reservation
    When the customer changes its access notes to "Park at B2"
    Then its access notes are "Park at B2"
    And after completion is submitted, a change is rejected with CompletionAlreadySubmittedException

Feature: The Insured badge

  Scenario: Declared insurance shows until it expires
    Given the provider declared insurance with "Etiqa" valid until 1 March
    Then the listing is insured on 28 February
    And it is not insured on 1 March

  Scenario: Insurance that has already expired can't be declared
    When the provider declares insurance valid until yesterday
    Then it is rejected with InsuranceExpiredException

Feature: Chargebacks

  Scenario Outline: The provider covers what the decision doesn't give them
    Given a chargeback case with RM200.00 at stake
    When the founders decide <decision>
    Then the provider covers <covers>
    Examples:
      | decision                           | covers  |
      | release                            | RM0.00  |
      | full refund                        | RM200.00 |
      | partial refund, provider gets RM50 | RM150.00 |

  Scenario: What the provider covers is recovered like a fine
    Given the provider covers RM150.00 for a chargeback
    When their next completed job pays RM200.00
    Then RM100.00 is deducted, the fine deduction cap
```

## Change after the domain phase: adding a team member, photo and confirmation

Decided by the product owner after a product critique of the add form. The form put a provider's
uploaded photo in front of customers without tying it to the verified person, asked for the
lawful-employment tick before Homa knew who the person was, allowed a minor, and fixed the role for good.

| # | Decision | Domain change |
|---|---|---|
| T1 | The profile photo is taken live on the provider's phone, and the KYC provider checks it is the same person as the verified selfie. A photo that doesn't match fails the check | account |
| T2 | The lawful-employment tick comes after the check passes, for the named person and their document country | account |
| T3 | Team members are 18 or older, from the date of birth in the KYC result | account |
| T4 | The provider changes a member's role without verifying them again | account |
| T5 | For a document from another country, the confirmation says the person holds a valid work pass for this provider; a lawyer settles the wording | None (the terms version records it) |

**`account`**

- `MemberVerification.start(business, role, photo, now)` no longer takes the confirmation. The photo is
  the live camera photo; whether it is live is the app's job.
- `KycResult` gains an optional `dateOfBirth`. A member's verified result must carry it
  (`VerificationDetailsRequiredException`); a re-verification needn't.
- `MemberVerification.recordKycResult(result, photoMatch, now)` takes the KYC provider's face comparison
  of the profile photo with the selfie (`PhotoMatch.MATCHED` or `NOT_MATCHED`). It ends as `REJECTED`,
  with a `MemberRejection`, for:
  - `CHECK_REJECTED`: the KYC provider rejected the check or the session timed out;
  - `UNDER_18`: the person is under 18 on that day in Malaysia;
  - `PHOTO_DOES_NOT_MATCH`: the profile photo isn't the verified person.
  Otherwise it is `VERIFIED`, waiting for the provider's confirmation. A repeated result changes nothing.
- `MemberVerification.confirmEmployment(confirmation)` records the tick, only once verified
  (`MemberNotVerifiedException`).
- `TeamMember.add` needs the confirmation (`EmploymentNotConfirmedException`), so a verified person joins
  only when the provider confirms.
- `TeamMember.changeRole(role)` changes helper and supervisor and returns whether it changed. A removed
  member can't change (`TeamMemberRemovedException`).

**Usecases already built (`account`):** `ProviderStartAddingTeamMember` takes the role and photo only;
`KycRecordMemberVerificationResult` records the result with the date of birth and the photo match, and
publishes `MemberVerifiedIE` or `MemberVerificationRejectedIE` with the reason; the new
`ProviderConfirmTeamMember` records the tick and adds the person with `TeamMemberAddedIE`.

**`ie/`:** `MemberVerifiedIE` and `TeamMemberRoleChangedIE` are added (82 events);
`MemberVerificationRejectedIE` carries the reason.

```gherkin
Feature: Adding a team member, photo and confirmation

  Background:
    Given Kemas, a verified business, started adding Aina as a helper with a live photo

  Scenario: A verified person waits for the provider's confirmation
    When the KYC provider verifies Aina, born 1 May 1998, and her photo matches
    Then the verification is VERIFIED
    And Aina is not on the team until Kemas confirms

  Scenario: The provider confirms the named person and they join
    Given Aina was verified
    When Kemas confirms she is lawfully employed
    Then Aina is added to the team, with the confirmation

  Scenario: Nobody joins without the confirmation
    Given Aina was verified
    When Kemas adds her without confirming
    Then it is rejected with EmploymentNotConfirmedException

  Scenario: The confirmation comes after the check
    When Kemas confirms before the KYC provider verifies Aina
    Then it is rejected with MemberNotVerifiedException

  Scenario Outline: The check fails for the person's age or photo
    When the KYC provider verifies Aina, born <born>, and her photo <photo>
    Then the verification is <result>
    Examples:
      | born          | photo         | result                        |
      | 2 March 2009  | matches       | REJECTED, UNDER_18            |
      | 1 March 2009  | matches       | VERIFIED                      |
      | 1 May 1998    | doesn't match | REJECTED, PHOTO_DOES_NOT_MATCH |

  Scenario: A verified result needs a date of birth
    When the KYC provider verifies Aina with no date of birth
    Then it is rejected with VerificationDetailsRequiredException

  Scenario: A team member's role changes without a new check
    Given Aina is a helper on the team
    When Kemas makes her a supervisor
    Then Aina is a supervisor
    And making her a supervisor again changes nothing
```

(The ages are checked on 1 March 2027 in Malaysia: born 1 March 2009 is 18 that day.)

## Open questions

### Decided

- **Provider cancels after the booked start but before checking in:** 30% of the hours fee, the same as a no-show.
- **Challenged customer no-show:** the reservation moves to Disputed. After Homa reviews it, it ends **Cancelled**: Homa upholds the no-show (the no-show fee stands), gives the provider part of the no-show fee, or finds it wrongful (full refund). *The partial outcome was confirmed in "aligning the product spec, domain plan and code"; the code always allowed it.*
- **Earliest check-in:** 15 minutes before the booked start.
- **Shared kernel:** domains may depend on `kernel/` (`Money`, `Percentage`). The backend module discipline is updated.
- **SMS code rules:** 6 digits, valid 5 minutes, a new code after 5 wrong attempts.
- **Who checks in:** not recorded or checked. Helpers have no accounts, so the domain cannot know who held the phone, and the provider is accountable either way. Check-in already requires the team to be named.
- **Availability at request and acceptance:** checked by the request and accept usecases, not by `Reservation`.
- **Report evidence:** optional. A reason is required; evidence is encouraged, since some problems (a helper not turning up) have nothing to photograph, and Homa weighs missing evidence when deciding.
- **Cancellation terms:** `Reservation.cancellationTerms()` gives what the review page shows, using the same base as the cancel rule.
- **A waived fine and the cancellation count:** the waiver covers only the money. `Fine.waive` changes
  nothing else, so the late cancellation or no-show still counts in the provider's cancellation count,
  which the provider page and admin tracking work out from the reservation events.

### To check before launch

- **The KYC provider:** choose it (Persona and Innov8tif are candidates), and confirm that it meets Homa's
  requirements: KYB with the owner's or director's KYC; a Malaysian owner or director; team members
  from any country; on-the-spot checks on the provider's phone; re-verification every year for
  businesses, and every year and on document expiry for team members, with a reminder; ongoing
  monitoring that reports "re-verification needed by a date" and serious flags; early re-checks on
  request; face checks at check-in with a limit on attempts; and a final
  result for every check, including a timed-out session.
- **Data protection:** a data processing agreement with the KYC provider, and the PDPA's rules on
  transfers if it processes data outside Malaysia.

### Answered while building

These came up while turning the spec into rules. All are now decided by the product owner.

1. **Where the customer no-show wait starts** (slice 6). *Decided:* from the later of the booked start and the check-in time.
2. **Rounding** (slice 1). Percentages of RM amounts can produce half sen. *Decided:* round half-up to the sen, and compute the customer refund last so the parts always add up to the total.
3. **Does editing a request reset its expiry?** (slice 2). *Decided:* no, the expiry stays from the original request.
4. **What "near the chosen time" means for close matches** (discovery). *Decided:* the provider is free for the same duration starting within 2 hours before or after. *Since "aligning the product spec, domain plan and code": a close match differs in one way only, and a suggested start must be bookable.*
5. **SMS code rules** (account). *Decided:* 6 digits, valid 5 minutes, 5 wrong attempts then a new code is needed.
6. **Is the lead one of the helpers?** (slice 5). *Decided:* yes, the lead is one of the named helpers, and a provider who is their own helper can be the lead. *Superseded: there is no lead. The provider names the helpers, and a supervisor when the booking reaches the supervisor rule; check-in is done on the provider app.*
7. **Fine deductions: which payouts count?** The spec says "next successful reservations". *Decided:* only payouts from Completed reservations, not cancellation or no-show fee payouts.
8. **Changes to an offering while a request is open.** *Decided:* the request keeps the offering snapshot it was made with; the provider's change affects new requests only.
