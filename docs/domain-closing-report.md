# Homa domain business logic: closing report

**Phase:** domain (Backend Module Discipline §8, step 1)
**Status:** complete
**Closed:** 8 November 2026, at commit `24a771e` on `main`
**Sources:** `docs/homa-product-description.md`, `docs/homa-domain-plan.md`, `docs/backend_module_discipline.md`
**Evidence:** `docs/domain-audit.md`

## 1. Summary

The domain layer of every bounded context in the plan is built, tested and passing its gates. It covers
the shared kernel, ten bounded contexts and the integration event contracts. Every product question
raised along the way has been answered by the product owner. The usecase phase was started and then
paused at the product owner's request, so the repository holds the domain modules only.

| Measure | Result |
|---|---|
| Bounded contexts with a domain layer | 10 of 10, plus `kernel` and `ie` |
| Gherkin scenarios in the plan with a matching test | 127 of 127 (122 at closing, plus five for the buffer between jobs added to the plan afterwards) |
| Unit tests | 827, all passing |
| Mutants | 1,040, all killed: 100% mutation score and test strength in every context |
| Worst CRAP score | 5.0 (limit: below 6) |
| Dependency rule | Passing in every context, and proven by planted violations |
| Integration events | 38, all with contract tests |
| Open product questions for the domain | 0 |

## 2. What was built

| Context | Owns | Main objects | Source files | Lines | Tests | Mutants | Worst CRAP |
|---|---|---|---|---|---|---|---|
| `kernel` | Money and shares, shared by every domain | `Money` (whole sen), `Percentage` (basis points, half-up) | 2 | 136 | 59 | 37 | 3.0 |
| `catalogue` | Homa's job and tool lists, launch cities, homepage shelves | `JobList`, `ToolList`, `ServiceArea`, `Shelf` | 29 | 626 | 64 | 80 | 4.0 |
| `policy` | The Homa-set values and their versions | `HomaPolicy`, `PolicySnapshot`, one `PolicyValue` per value | 13 | 613 | 70 | 58 | 3.0 |
| `account` | Customers, providers, helpers, phone and eKYC verification | `PhoneVerification`, `CustomerAccount`, `ProviderAccount`, `Helper` | 28 | 576 | 65 | 86 | 5.0 |
| `offering` | A provider's listing and going live | `ProviderListing`, `OfferingSnapshot` | 36 | 783 | 67 | 158 | 4.0 |
| `availability` | Windows, synced busy blocks, holds, buffer between jobs | `ProviderCalendar` | 16 | 343 | 46 | 66 | 4.0 |
| `reservation` | Enquiries and the reservation lifecycle, with every time and money rule | `Enquiry`, `Reservation`, `Settlement`, `CancellationTerms` | 69 | 1,590 | 187 | 280 | 5.0 |
| `payment` | Money movement, fines and payouts | `Payment`, `Fine`, `Payout`, `FineDeductionPolicy` | 32 | 752 | 50 | 127 | 5.0 |
| `resolution` | The founders' case queue | `Case` | 11 | 250 | 17 | 40 | 3.0 |
| `conversation` | Chats for enquiries and reservations | `Conversation` | 13 | 273 | 13 | 38 | 3.0 |
| `discovery` | Search criteria and matching | `SearchCriteria`, `MatchClassifier` | 18 | 332 | 32 | 70 | 5.0 |
| `ie` | Integration event contracts | 7 event interfaces, 38 event records | 45 | 921 | 157 | n/a | 1.0 |
| **Total** | | | **312** | **7,195** | **827** | **1,040** | **5.0** |

Every business rule violation has its own exception, named for the rule, in its context's
`domain/exception` package: 134 in all.

## 3. Quality gates

All gates run on every build: `mvn verify`, then `python3 tools/quality_gates.py`.

| Gate | Threshold | Result | How it is enforced |
|---|---|---|---|
| Unit tests | Robust and intent-focused | 827 passing | JUnit 5, framework-free, one fixed clock per test, shared builders in test-support files |
| CRAP index | Below 6 for every method | Worst 5.0 | `tools/quality_gates.py` reads JaCoCo's report |
| Mutation, `kernel/` and domain | At least 90%, with the ratchet | 100% score and test strength in every context | PIT with STRONGER mutators. The script gates each context, and `quality/ratchet.json` holds each at 100% |
| Dependency rule | Domain imports only `kernel/`; `ie/` imports only `kernel/`; `kernel/` imports only the JDK | Passing | ArchUnit in each context's test package, plus `KernelArchitectureTest` and `IntegrationEventArchitectureTest` |
| Startup budget | | Not applicable yet | Applies at `app/`, a later phase |

The dependency rule also stops one context importing another. A planted `offering → policy` import and
a planted `payment → ie` import each failed the build, so the rules are proven to work.

Surviving mutants also caught problems while the code was being built:
- **One design flaw:** discovery's two booleans for "free at the chosen time" and "free near it"
  overlapped, so no test could tell them apart. They became a single `nearestFreeStart`.
- **Missing tests:** for example, a card edit downward that must refund nothing, a request that takes
  no reports, a draft listing staying a draft when its team changes, and a team shrinking to one helper.
  Each was added before the gate passed.

## 4. Coverage of the plan and the spec

- **Scenarios:** all 127 Gherkin scenarios in the plan have a test, named by the convention. Every row
  of the 12 Scenario Outlines is a test case, and most rules are also tested on the other side of their
  boundary. The scenario-to-test mapping is in `docs/domain-audit.md`, section 1.
- **Money invariants:** for every ending, on every tier and at several rates that force rounding, the
  provider earning plus the service fee plus the customer refund equals the total. No cancellation fee
  is above the customer no-show fee.
- **Spec rules with no domain test:** the audit found four. All are closed:
  - G1, the cancellation fee shown on the review page: built as `Reservation.cancellationTerms()`.
  - G2, who checks in: no rule. Helpers have no accounts and the provider is accountable either way.
  - G3, availability at request and acceptance: belongs in the usecases.
  - G4, report evidence: optional. The spec now says "with a reason, and evidence where available".
- **Rules outside the domain phase** are listed in the audit, section 2b: notifications, read models,
  schedulers, payment and storage infra, and privacy.

## 5. Changes from the original plan

All of these were agreed with the product owner as they came up.

**Repository structure**
- One Maven project with a single source tree, `src/main/java/<context>/domain`, instead of a module per
  context. Context isolation, which the compiler had enforced, is now enforced by ArchUnit.
- The product description, plan and discipline are kept in `docs/`.

**Domain behaviour added**
- A provider can set a buffer between jobs (`availability`).
- The team reminder: a new Homa-set value, 6 hours before the booked start (changed from 24 hours
  after the report was first written), and `Reservation.isTeamReminderDue`.
- `Reservation.cancellationTerms()`, from audit gap G1.
- `Helper.remove()`, so `HelperRemovedIE` has a domain method behind it.
- `Case.providerReceives()`, behind `CaseDecidedIE`.
- Customers have a name, since providers see it.

**Signatures adjusted**
- `HomaPolicy.initial(now)`, `Reservation.resolve(decision, now)` and `Payment.settle(settlement, now)`
  take the time, following the "time is passed in" convention.
- `ProviderListing.setHours` and `setHelpers` take `HomaLimits`, offering's own view of the Homa-set
  values.
- `Reservation.request` takes the payment method separately from `ReservationDetails`, because an
  enquiry has the same details but no payment.
- Ids are Strings throughout, following `XId().generate()`.
- Contexts that consume another context's data define their own snapshot types, such as reservation's
  `OfferingSnapshot` and `PolicySnapshot`. A domain may not import another context.

**Payment**
- A fifth state, `AUTHORISATION_RELEASED`, for a card request that ends before acceptance.
- `PayoutKind`, so fines come off completed-job payouts only.

**Integration events**
- There are 38, up from the plan's 32. The new ones are `JobAddedIE`, `ToolAddedIE`,
  `ListingWentOfflineIE`, `EnquiryRequestedIE`, `ProviderVerificationRejectedIE` and
  `HelperVerificationRejectedIE`.
- The event rule now reads: primitives, Strings, kernel types and JDK value types (`Instant`,
  `Duration`, immutable `List`/`Set`); never domain objects; no `Optional` fields.
- Collections are copied in each record's constructor, and there is one event per outcome.
- Events were split so every field is always present.
- The plan, the discipline and the README all state the same rule.

**Documents updated**
- Domain plan: the new methods, values and events, the event table, and the decided open questions.
- Discipline: the event rule.
- Product description: the Dispute Policy evidence wording.

## 6. Decisions on record

All eight open questions from the plan are confirmed:

| # | Question | Decision |
|---|---|---|
| 1 | Where the customer no-show wait starts | From the later of the booked start and the check-in |
| 2 | Rounding | Half-up to the sen, with the customer refund worked out last. The fine cap rounds in the provider's favour |
| 3 | Does editing a request reset its expiry? | No |
| 4 | What "near the chosen time" means | Free for the same duration within 2 hours either side |
| 5 | SMS code rules | 6 digits, valid 5 minutes, a new code after 5 wrong attempts |
| 6 | Is the lead one of the helpers? | Yes, and a provider who is their own helper can lead |
| 7 | Which payouts have fines deducted | Payouts for completed jobs only |
| 8 | Changes to an offering while a request is open | The request keeps the snapshot it was made with |

The full list of smaller decisions, such as boundary times, edge cases and payment states, is in the
README under "Decisions taken while building". All of them are confirmed.

## 7. Carried forward

**Usecase phase** (paused; draft plan in `docs/homa-usecase-plan.md`)
- **Open questions U1 to U7** need review before it starts. U1 is how reservation and discovery check
  availability. U4 is cancelling a request, and U5 is a payment that fails at request.
- **Obligations the domain leaves to the usecases:**
  - Check availability on request and on acceptance (G3).
  - Send a customer no-show settlement to payment only when the report window ends unchallenged.
  - Run the schedulers for expiry, provider no-show, reminders, completion deadline, dispute window and
    enquiry closing.
  - Keep each context's projections of another's data up to date from events.

**Infra phase**
- **U7:** rebuilding aggregates from storage. The domain constructors are private and generate new
  ids.
- **Services and jobs:** evidence retention, refunds to the original payment method, geoservice
  distance, Kenal and Stripe callbacks, notifications, and read models such as provider page stats and
  admin tracking.

**Housekeeping**
- **Fold the decisions into the plan:** most of the README's decisions are reflected in the plan, but
  some smaller ones (boundary times, edge cases) are only in the README.
- **Build time:** a full mutation run takes about two to three minutes. PIT's incremental analysis would
  help once the usecase layer adds more code.

## 8. Timeline

| Commit | Step |
|---|---|
| `8612e79` | Build setup and gates; steps 0–2: `kernel`, `catalogue`, `policy` |
| `5d4e9b7` | Step 3: `account` |
| `63c1b3d` | Step 4: `offering` |
| `24a3bc8` | Step 5: `availability` |
| `4bea4cb` | Restructured into one source tree |
| `6427035` | Buffer between jobs |
| `b263275`, `b0a0a10`, `456d4fc` | Step 6: `reservation`, slices 1–2, 3–6 and 7 |
| `c8a9bb7` | Step 7: `payment` |
| `c9bd427`, `f0a12e4`, `c0f9d02` | Steps 8–10: `resolution`, `conversation`, `discovery` |
| `45d66be` | Team reminder |
| `5ca1962`, `17015a2`, `219d592` | Step 11: `ie/`; no `Optional` fields; verification rejected events |
| `1a3bf33`, `96a0377` | Domain audit; `cancellationTerms`; decisions recorded |
| `1fd646f`, `24a771e` | Usecase phase started, then paused and removed |

## 9. How to verify

```bash
mvn clean verify                 # 827 tests, ArchUnit dependency rules, PIT mutation
python3 tools/quality_gates.py   # CRAP < 6, mutation >= 90% per context, ratchet
```

Reports: `target/site/jacoco/` for coverage and `target/pit-reports/` for mutation.

## 10. Addendum: feedback

Added after the phase closed, from the updated product description (Rating Policy) and domain plan (step
12). Built and gated like the other contexts.

- **`policy`:** two new Homa-set values, the rating window (14 days) and the minimum ratings to show (3).
  Version 1 now holds 26 values.
- **`feedback` domain:**
  - `Rating`: submitting a rating, and Homa hiding one.
  - `RatingSummary`: for each provider and each helper, the overall count and average, and a count and
    average for each category. A category's average is shown once that category has its own minimum
    ratings, because customers may skip categories.
  - The value objects:
    - stars
    - provider ratings: overall stars, optional stars for any of four provider categories, and
      optional written feedback
    - helper ratings: overall stars, and optional stars for five helper categories
    - the two category lists
    - the reservation as feedback knows it
    - the limits
  - 26 source files, 651 lines, 14 exceptions.
- **`ie/`:** `RatingSubmittedIE` and `RatingHiddenIE`, published by a new `FeedbackIE` interface.
  `HomaPolicyChangedIE` also carries the two new values. That makes 40 events.
- **Scenarios:** all 25 feedback scenarios in the plan map to a test, named by the convention. The audit's
  scenario table now has 152 of 152.
- **Gates:**
  - `feedback.domain`: 105 mutants, all killed (100% score and test strength), worst CRAP 4.0.
  - Whole build: 901 tests, 1,147 mutants, all killed.
  - The dependency rule passes for `feedback`.
- **Decisions while building:**
  - `RateableReservation` also carries whether the reservation is already rated, so "a reservation is
    rated once" can be enforced in the domain.
  - The rating window includes its last minute.
  - Written feedback is counted in characters.
  - A provider summary is rated only on provider categories, and a helper summary only on helper
    categories.
  - Events carry no maps, so `RatingSubmittedIE` carries the overall stars, whether there is written
    feedback, and the rated helper ids. Category stars stay in `feedback`.
- **Revised after review:** the rating model changed twice. First, starred provider categories became
  highlights picked without stars. Then the final model replaced highlights and compliments with
  star-rated categories for both providers and helpers. Only the overall stars are required; category
  stars are optional for both providers and helpers.

  All of these are in the plan under "Decisions made while building". The feedback length limit and the
  window's last minute are also in the spec.

## 11. Addendum: team membership

Changed after the phase closed. A helper is **invited** first and becomes a **team member** only once
their eKYC is verified, so "team" means verified everywhere.

- **`account`:**
  - `Helper.invite` replaces `Helper.add`.
  - `Helper` has one `TeamStatus`: `INVITED`, `INVITE_REJECTED`, `MEMBER` or `REMOVED`.
  - `isTeamMember()` is true only for `MEMBER`.
  - A rejected invite can retry; a removed helper cannot come back.
  - A member is not verified again: a new eKYC result for a member is rejected with
    `HelperAlreadyTeamMemberException`, so it can't silently drop them off the team.
- **`offering`:**
  - `goLive(providerReady)` no longer takes "all helpers verified", and that go-live requirement is
    gone, so a pending invite never blocks going live.
  - The roster holds only team members, so the existing helper-range requirement already covers the
    minimum helpers.
- **Renames:**
  - `ie/`: `HelperVerifiedIE` is now `HelperJoinedTeamIE`, `HelperVerificationRejectedIE` is now
    `HelperInviteRejectedIE`, and `HelperInvitedIE` is new. That makes 41 events.
  - The `TeamRoster` field is now `memberIds`, and the listing events carry `teamMemberIds`.
- **Scenarios:**
  - The "Team membership" feature, with six scenarios, replaces the two "Helpers" scenarios.
  - "A pending invite does not block go-live" replaces "An unverified helper blocks go-live".
  - The audit maps 156 of 156.
- **Gates:** whole build 909 tests, 1,147 mutants, all killed. `account.domain` has 87 mutants and
  `offering.domain` 157, both at 100%.

## 12. Addendum: periodic re-verification

Changed after the phase closed, for customer safety. Every provider and team member is re-verified
through eKYC every **3 months**, counted from their own last successful verification.

- **`policy`:** three new Homa-set values:
  - re-verification period: 3 months
  - reminder: 14 days before the due date
  - early re-check deadline: 14 days

  That makes 29 values. `HomaPolicyChangedIE` carries all three.
- **`account`:**
  - `Helper` and `ProviderAccount` hold a `Verification`: when the person was last verified, when they
    are due, whether a re-check is open, and the reason for an early one.
  - New methods: `openRecheck`, `requestEarlyRecheck` and `lapse`. `recordEkycResult` now takes the
    limits and the time, and returns an `EkycOutcome`.
  - `TeamStatus` gains `LAPSED`, and a lapsed provider is `VerificationStatus.LAPSED`.
  - The old rule "no new result for a member" is replaced:
    - With a re-check open, a pass starts a new period and a rejection changes nothing yet.
    - With no re-check open, a repeated pass changes nothing and a rejection is refused.
    - A lapsed person who passes is back on the team.
- **`offering`:**
  - `pause()` and `resume()`: a paused listing stays live but takes no new requests.
  - `takesRequests()` is copied into the snapshot.
- **`reservation`:** `Reservation.request` refuses a paused listing with `ListingPausedException`.
- **`ie/`:** the plan's five events, plus `ListingPausedIE`, `ListingResumedIE` and `RecheckFailedIE`.
  That makes 49 events.
- **Scenarios:** the two re-verification features add 14 scenarios, and the audit maps 170 of 170.
- **Gates:** whole build 1,026 tests, 1,241 mutants, all killed. `account.domain` has 156 mutants and
  `offering.domain` 177, both at 100%.
- **Decisions:** the decisions made while building are confirmed and listed in the plan, under "Change
  after the domain phase: periodic re-verification". A paused provider can't accept any request,
  including one made before the pause. The request stays Requested and expires normally unless the
  provider re-verifies in time. The accept usecase enforces this (required rule R2 in the usecase
  plan); the domain is unchanged.

## 13. Addendum: verified for the whole job

Changed after re-verification. A provider can be requested, and can accept, only for a job that ends
by their re-verification due date. Search leaves them out for any later job.

- **`reservation`:**
  - `request`, `edit` and `accept` take the provider's due date as it is now.
  - A job ending after it is refused with `JobEndsAfterProviderVerificationException`. A job ending
    exactly at it is allowed.
  - Acceptance uses the due date at the time of accepting. So an early re-check can block it, and a
    passed re-check unblocks it.
- **`discovery`:** `ProviderCandidate` carries `verifiedUntil`. A job that would end after it is no
  match, with reason `VERIFICATION_DUE_BEFORE_END`.
- **`ie/`:** `ProviderVerifiedIE` and `ProviderReverifiedIE` carry `reverificationDueAt`. The usecase
  plan adds required rule R6.
- **Scenarios:** the "Verified for the whole job" feature adds five, and the audit maps 175 of 175.
- **Gates:** whole build 1,038 tests, 1,253 mutants, all killed.

## 14. Addendum: provider types, team roles, documents, face check-in and adding a team member

Changed after the phase closed. Safety principle: only verified people enter a customer's home, and the
customer knows who they are before they arrive.

- **`policy`:**
  - No Homa limit on helpers.
  - New values: face check attempts (3), verification link expiry (7 days; removed in section 15) and the accepted documents
    list. That makes 30 values today.
- **`account`:**
  - **Providers:** a provider is independent or a business. A business's SSM registration is checked
    before it can go live.
  - **Names:** everyone verifies with an accepted document, and their registered name comes from the
    eKYC result.
  - **Adding a team member:** it is a `MemberVerification`: the add form, with the lawful-employment
    confirmation for that person, then the eKYC.
  - **Team members:** a `TeamMember` exists only from a verified `MemberVerification`, as a helper or a
    supervisor. Invites are gone.
- **`offering`:**
  - The roster holds helpers and supervisors, and only helpers cap the helpers range.
  - A `SupervisorRule` sends a supervisor from a number of helpers, of hours, or both. It needs a
    supervisor on the team.
- **`discovery`:** a search is for at least 1 helper.
- **`reservation`:**
  - **The team:** it is helpers plus supervisors, and the lead is gone. A booking that reaches the
    supervisor rule needs a supervisor. Supervisors are not charged.
  - **Check-in:** it is the location check, then face checks. It completes with the first pass, or
    when the face check service is unavailable. Someone who fails every attempt cannot work, and an
    outage never makes a no-show.
- **`feedback`:** named supervisors are rated like helpers.
- **`ie/`:** 53 events today. A rejected eKYC for adding a team member ends with
  `MemberVerificationRejectedIE`.
- **Scenarios:** the audit maps all scenarios (198 today). The older invite, lead and 1 to 10 helper scenarios point
  to the tests of the rules that replaced them.
- **Gates:** whole build 1,149 tests, 1,480 mutants, all killed, worst CRAP 5.0.
- **Decisions:** the decisions made while building are confirmed, and listed in the plan under "Decided
  while building: provider types, team roles, documents, face check-in and adding a team member".

## 15. Addendum: verification on the spot

Changed after the phase closed. A person being added or re-verified does eKYC on the spot, on the
provider's phone: they scan their own document and take their own selfie. There is no link and no time
limit.

- **`policy`:** the verification link expiry is removed, and no session expiry replaces it. That makes
  30 Homa-set values.
- **`account`:**
  - `VerificationLimits` holds the re-verification values only.
  - `MemberVerification` has no `expire` and no `EXPIRED` status. A waiting verification takes its
    result whenever it arrives. Kenal times out an unfinished session and reports it as a failed
    result.
- **`ie/`:**
  - `MemberVerificationExpiredIE` is removed.
  - `MemberVerificationStartedIE` no longer carries `expiresAt`, and `HomaPolicyChangedIE` no longer
    carries an expiry.
  - That makes 53 events.
- **Usecase plan:** required rule R9 covers verification on the spot, with no time limit.
- **Scenarios:** the expiry scenario is removed, and the audit maps 198 of 198.
- **Gates:** whole build 1,138 tests, 1,472 mutants, all killed.
- **Kenal's timeout:** Kenal owns the session timeout and always reports a final result. A timed-out
  session counts as rejected, so no verification stays waiting. All the decisions are confirmed, and
  are in the plan under "Decided while building: verification on the spot".
- **To check before launch:** that Kenal's sessions time out and report a final result. This is listed
  in the plan's open questions.

## 16. Addendum: business verification through eKYB

Changed after the phase closed. A business provider is verified through eKYB. The eKYB provider checks
the SSM registration, and the owner's or director's eKYC is part of the same flow. Homa does no check of
its own, just as with eKYC.

- **`account`:**
  - `ProviderAccount.recordEkybResult` records an `EkybResult`: the SSM number and registered business
    name, and the owner's or director's document, which must be Malaysian.
  - The re-verification rules are the same as eKYC: the 3-month period, reminder, early re-check and
    lapse.
  - A business takes no separate eKYC, and an independent provider takes no eKYB.
  - `ekycStatus()` is now `verificationStatus()`.
- **No team at the start:** a business starts with no team members and is never a helper on its own
  team, so it adds its helpers and supervisors.
- **Going live:** a business is ready once its eKYB is verified and its payout account is ready.
- **Removed:**
  - The registration check by Homa: `recordBusinessRegistrationChecked`,
    `BusinessRegistrationAlreadyCheckedException` and `BusinessRegistrationCheckedIE`. That makes 52
    events.
- **Providers:** the domain never names the eKYC or eKYB provider, so changing provider, or using
  different ones, affects only the infra adapters.
- **Scenarios:** seven new ones, and the audit maps 205 of 205.
- **Gates:** whole build 1,141 tests, 1,487 mutants, all killed, worst CRAP 5.0.

## 17. Addendum: business providers only, verification by a KYC provider

Changed after the phase closed. Homa's development focuses on value for homeowners and homecare
providers:
- **Business providers only:** every provider is a business registered with SSM.
- **Verification by a KYC provider:** identity verification is done entirely by a KYC provider (to be
  chosen; Persona and Innov8tif are candidates). That covers the onboarding pages, KYB with the owner's or
  director's KYC, team members' KYC, document rules, re-verification schedules, early re-checks and
  face checks at check-in.

- **`policy`:** the accepted documents, re-verification values and face check attempts are removed,
  leaving 25 Homa-set values.
- **`account`:**
  - `ProviderAccount` records `KybResult`s, and `MemberVerification` and `TeamMember` record
    `KycResult`s. Each carries a verified-until date.
  - A lapse is reported by the KYC provider.
  - The document rules, due-date maths, re-checks, photo match, provider types and self-helper are
    removed.
- **`offering`:** no provider type.
- **`reservation`:** the face check takes the KYC provider's final result for each person. `FAILED`
  means they cannot work, and the attempt limit is the KYC provider's.
- **What stays in Homa:** only verified businesses and team members are on Homa; the lawful-employment
  confirmation; the pause on lapse; verified for the whole job; and the check-in rules.
- **`ie/`:** `ProviderRegisteredIE` and `RecheckOpenedIE` are removed, which makes 50 events.
- **Scenarios:** 216 in the plan; 204 have a test and 12 are retired.
- **Gates:** whole build 1,039 tests, 1,370 mutants, all killed, worst CRAP 5.0.
- **Decisions:** a failed re-verification changes nothing, the registered name follows the latest
  pass, and Homa uses the verified-until date the KYC provider sends. All are confirmed.
- **To check before launch:** choose the KYC provider and confirm it meets Homa's requirements, and
  set up a data processing agreement covering transfers outside Malaysia.

## 18. Addendum: ongoing monitoring and annual re-verification

Changed after the phase closed. A business's registration doesn't expire, but it can change. The KYC
provider (Persona and Innov8tif are candidates):
- re-verifies businesses every year;
- re-verifies team members every year and when their document expires;
- monitors both in between.

- **`account`:**
  - `requireReverification(reason, deadline)` on `ProviderAccount` and `TeamMember` moves the
    verified-until date sooner, never later, while the business or person keeps working.
  - `ProviderAccount.recordSeriousFlag(reason)` lapses the business at once, and a later pass clears it.
  - A flagged team member is lapsed.
- **`reservation`:**
  - On a serious flag, every accepted reservation whose job isn't completed moves to Disputed
    (`disputeForProviderFlag`).
  - Homa cancels it with a full refund, no fine and no cancellation fee (`cancelForProviderFlag`). No
    other decision is allowed.
- **`ie/`:** `ReverificationRequiredIE`, `ProviderFlaggedIE` and `ReservationFlaggedIE`, which makes 53
  events.
- **Spec:**
  - The KYC provider requirements become annual re-verification, re-verification on document expiry and
    ongoing monitoring.
  - The Disputed status covers a flagged provider.
- **Usecase plan:** required rule R10.
- **Scenarios:** 224 in the plan; 212 have a test and 12 are retired.
- **Gates:** whole build 1,075 tests, 1,393 mutants, all killed, worst CRAP 5.0.
