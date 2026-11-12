# Homa Usecase Development Plan

This plan covers the **usecase phase** (Backend Module Discipline §8, step 2): the `usecase` layer of each bounded context. It sits on the finished domain phase (`docs/homa-domain-plan.md`, `docs/domain-audit.md`). Infra comes after.

**Status:** in progress. `catalogue` is built and passes its gates (see Progress); the other contexts follow in the build order. Every question, U1 to U13, is decided (see the end).

## Conventions

- **Layout** (Discipline §3), per context:

  ```text
  <context>/usecase/
  ├── port/in/        one interface per usecase, and one per integration event handler
  ├── port/out/       repositories, the context's IE publisher, outbound clients (SMS, the KYC provider, Stripe, geoservice)
  ├── dto/            a request and a response record per usecase, never shared
  ├── exception/      orchestration failures, such as a record not found
  └── <Impl>.java     usecase and handler implementations
  ```

- **Naming** (Discipline §6): the implementation is named for the action and the interface for the actor
  of the action, as in `UserRegisterAccount implements UserAccountRegistrator`. Homa's actors are
  `Homa` (the founders, in the admin console), `Customer`, `Provider`, and the external services that
  report back (`Kyc` for the KYC provider, `Stripe`). For example: `HomaAddJob implements HomaJobAdder`, with
  `HomaJobAdder.add(AddJobRequest)`.
- **Event handlers** (Discipline §7): `HandleJobRetiredIE implements JobRetiredIEHandler`, one per event.
- **Dependencies:** a usecase uses only its own context's domain, `kernel/` and `ie/`. It never uses
  another context. ArchUnit checks this in each context.
- **Rules stay in the domain.** A usecase loads, calls the domain, saves, and publishes. If a usecase
  starts deciding something, that rule belongs in the domain.
- **Time:** usecases take a `java.time.Clock` and pass `Instant now` to the domain.
- **Events:** a usecase maps the domain's result into the `ie/` event and publishes it through the
  context's single `<Context>IEPublisher` out port. Domain objects never leave the usecase layer: DTOs
  hold primitives, Strings, kernel and JDK value types, the same as events.
- **Errors:** domain exceptions pass through unchanged. Usecase exceptions cover orchestration only,
  such as `ShelfNotFoundException`.
- **Admin actions:** every usecase whose actor is `Homa` and that changes something takes the founder's
  id (`decidedBy`) in its request and records it, with the action, the reason and the time, through an
  `AdminAuditLog` out port (rule A1). The domain keeps the reason; the audit log keeps who.
  - Each context declares its own `AdminAuditLog` port and `AdminAction` record in `port/out`, because
    a context uses nothing of another. One adapter in `app/` can serve them all.
  - The founder's id is required before anything is done (`DecidedByRequiredException`).
  - A reason is required for a decision about a person, money or providers' listings
    (`AdminReasonRequiredException`). Plain curation, such as adding or renaming a job, records who,
    what and when, with no reason.
- **Queries with no input** have a response DTO only; an empty request record would say nothing.
- **Contexts with no domain layer:** `tracking` (read-only views) and `privacy` (recording data
  requests) have no business rules, so they have `usecase` and `infra` only. They are the only
  exceptions to the three layers of the module discipline.
- **Tests:** framework-free, against in-memory fakes of the out ports kept in test-support files. A
  usecase test proves the orchestration: what is loaded, saved and published, and what is passed to the
  domain. It does not test the domain's rules again. The mutation gate is **at least 80%** for
  `usecase`, with the ratchet. `tools/quality_gates.py` gates each layer separately.

## How a context gets another context's data

Contexts only talk through `ie/`, so a context that needs another's data keeps its own **projection**,
fed by event handlers and stored behind its own out port:

| Consumer | Keeps | Fed by |
|---|---|---|
| offering | active catalogue jobs and tools | `JobAddedIE`, `JobRetiredIE`, `ToolAddedIE`, `ToolRetiredIE` |
| offering | Homa's hours and helpers limits | `HomaPolicyChangedIE` |
| offering | the cities Homa serves | `ServiceAreaPublishedIE` |
| offering | provider readiness, and the team roster by role | `ProviderVerifiedIE`, `ProviderVerificationRejectedIE`, `PayoutAccountReadyIE`, `TeamMemberAddedIE`, `TeamMemberReverifiedIE`, `TeamMemberLapsedIE`, `TeamMemberRemovedIE` |
| offering | whether the provider's re-verification has lapsed or the business is seriously flagged | `ProviderVerificationLapsedIE`, `ProviderFlaggedIE`, `ProviderReverifiedIE` |
| reservation | each live listing's `OfferingSnapshot`, and whether it is paused now | `ListingWentLiveIE`, `ListingChangedIE`, `ListingWentOfflineIE`, `ListingPausedIE`, `ListingResumedIE` |
| reservation, discovery | the policy in force | `HomaPolicyChangedIE` |
| reservation, discovery | each business's verified-until date | `ProviderVerifiedIE`, `ProviderReverifiedIE`, `ReverificationRequiredIE` |
| reservation, payment | whether each business is seriously flagged now | `ProviderFlaggedIE`, `ProviderReverifiedIE` (a pass clears the flag) |
| discovery | each live listing's search fields | the listing events |
| discovery, tracking | job and tool names and job groups, for results and the provider page | `JobAddedIE`, `JobRenamedIE`, `JobRegroupedIE`, `ToolAddedIE`, `ToolRenamedIE` |
| feedback | each reservation's customer, provider, named team and completion time | `ReservationRequestedIE`, `TeamNamedIE`, `ReservationCompletedIE` |
| feedback | the rating window and the minimum ratings to show | `HomaPolicyChangedIE` |
| tracking | its own views of everything | every event (see Admin console) |
| conversation | customer names | `CustomerRegisteredIE` |

Availability is the exception: a request needs a live answer (see U1).

Only a reservation that ends Completed can be rated, so feedback marks a reservation rateable on
`ReservationCompletedIE` only. `ReservationCompletedIE` carries no customer or team, which is why
feedback also listens to `ReservationRequestedIE` and `TeamNamedIE`.

## Usecases per context

`→` shows the event published.

**catalogue**
- `HomaAddJob`, `HomaRenameJob`, `HomaRegroupJob`, `HomaRetireJob` → `JobAddedIE`, `JobRenamedIE`, `JobRegroupedIE`, `JobRetiredIE`
- `HomaAddTool`, `HomaRenameTool`, `HomaRetireTool` → `ToolAddedIE`, `ToolRenamedIE`, `ToolRetiredIE`
- `HomaCreateShelf` (only for jobs providers can offer). `HomaRetireJob` also takes the job off every shelf's search; a shelf left with no job is no longer shown.
- `HomaPublishServiceArea` → `ServiceAreaPublishedIE` (the launch cities; U2). Publishing again sends the same cities again.
- Retiring a job or tool needs a reason, because it changes providers' listings (R11). Renaming and regrouping keep the code, so listings are untouched; the events update the names and groups other contexts show.
- Queries: `HomaViewJobList`, `HomaViewToolList` and `HomaViewShelves` (everything, retired and hidden included), `ProviderViewJobList` (offered jobs by group), `ProviderViewToolList`, `CustomerViewShelves` (in order), `ProviderViewServiceArea`

**policy**
- `HomaLaunchPolicy` (version 1)
- `HomaChangePolicyValue` → `HomaPolicyChangedIE`
- Query: `HomaViewPolicy`

**account**
- Customer: `CustomerStartPhoneVerification` (SMS out port), `CustomerConfirmPhone`, `CustomerRegisterAccount` → `CustomerRegisteredIE`, `CustomerChangeAddress` (geoservice out port)
- Provider: `ProviderRegisterAccount`; `ProviderStartKyb` (opens the KYC provider's KYB pages for the business, with the owner's or director's KYC)
- Team: `ProviderStartAddingTeamMember` (the add form, with the lawful-employment confirmation; opens the KYC provider's on-the-spot check on the provider's phone) → `MemberVerificationStartedIE`; `ProviderStartReverification` (opens the KYC provider's re-verification for the business or a team member, with the person present); `ProviderRemoveTeamMember` and `HomaRemoveTeamMember` → `TeamMemberRemovedIE`
- The KYC provider reporting back (webhooks, R9): `KycRecordBusinessResult`, `KycRecordMemberVerificationResult` (adds the member on `VERIFIED`, R7), `KycRecordTeamMemberResult` and `KycRecordLapse` publish one event per `VerificationOutcome`, and nothing for `UNCHANGED` (see required rule R1)
- Ongoing monitoring (webhooks): `KycRecordReverificationRequired` → `ReverificationRequiredIE`; `KycRecordSeriousFlag` → `ProviderFlaggedIE` for a business, or `TeamMemberLapsedIE` for a team member
- Early re-check: `HomaRequestEarlyRecheck` asks the KYC provider for one, with a reason; nothing changes in Homa until the KYC provider reports a pass or a lapse
- Payout account: `ProviderAddPayoutAccount` (opens Stripe's onboarding for the business bank account); Stripe reporting back: `StripeRecordPayoutAccountReady` → `PayoutAccountReadyIE`

**offering**
- `ProviderDraftListing`, `ProviderSetProfile`, `ProviderOfferJobs`, `ProviderBringTools`, `ProviderSetPricing`, `ProviderSetHours`, `ProviderSetHelpers`, `ProviderSetSupervisorRule`, `ProviderClearSupervisorRule`, `ProviderCoverCities`, `ProviderChooseCancellationTier` → `ListingChangedIE` when live
- `ProviderGoLive` → `ListingWentLiveIE`
- Handlers: the catalogue, policy and account events above.
  - A roster change can lead to `ListingWentOfflineIE`.
  - `JobRetiredIE` takes the job off every listing that offers it (`withdrawJob`) → `ListingChangedIE`, or `ListingWentOfflineIE` when no job is left; `ToolRetiredIE` does the same for a tool (`withdrawTool`) → `ListingChangedIE` (R11).
  - `ProviderVerificationLapsedIE` and `ProviderFlaggedIE` pause a live listing → `ListingPausedIE`; `ProviderReverifiedIE` resumes a paused one → `ListingResumedIE`.

**availability**
- `ProviderSetWindows`, `ProviderSetBuffer`, `CalendarSyncBusyBlocks` (scheduled)
- Handlers: `ReservationAcceptedIE` (hold), `ReservationCancelledIE` (release)
- Query port for U1: `AvailabilityCheck` (is a slot free, nearest free start)

**reservation**
- Customer: `CustomerCreateEnquiry` → `EnquiryCreatedIE`; `CustomerBookAgain`; `CustomerRequestReservation` → `ReservationRequestedIE` (and `EnquiryRequestedIE` when made from an enquiry); `CustomerEditRequest` → `ReservationEditedIE`; `CustomerViewCancellationTerms`; `CustomerCancelReservation` (a request → `RequestCancelledIE` only; a reserved booking → `ReservationCancelledIE` and `SettlementDecidedIE`; R12); `CustomerChallengeNoShow` → `NoShowChallengedIE`
- Provider: `ProviderViewRequests` (only requests whose payment is secured, R14); `ProviderAcceptRequest` → `ReservationAcceptedIE` (refused while the listing is paused, R2, and by the domain until the payment is secured, R14); `ProviderDeclineRequest`; `ProviderCancelReservation`; `ProviderNameTeam` → `TeamNamedIE`; `ProviderCheckIn` → `ProviderCheckedInIE`; `KycRecordFaceCheck` (the KYC provider's final result for each person) → `FaceCheckRecordedIE` (and `CheckInCompletedIE` on the first pass); `FaceCheckServiceUnavailable` → `CheckInCompletedIE`; `ProviderReportCustomerNoShow`; `ProviderSubmitCompletion` → `CompletionSubmittedIE`
- Either party: `PartyFileReport` → `ReportFiledIE`
- Scheduled: `HomaExpireRequests`, `HomaMarkProviderNoShows`, `HomaCloseNoShowReportWindows`, `HomaRemindTeams`, `HomaRemindCompletions`, `HomaMissCompletionDeadlines`, `HomaCloseDisputeWindows`, `HomaCloseEnquiries`
- Each ending after acceptance publishes `ReservationCancelledIE` or `ReservationCompletedIE`, with `SettlementDecidedIE` and `FineChargedIE` as the domain returns them. A request ends with one event: `ReservationDeclinedIE`, `ReservationExpiredIE` or `RequestCancelledIE`.
- Flagged providers: the `ProviderFlaggedIE` handler disputes each accepted reservation whose job isn't completed → `ReservationFlaggedIE`; `HomaCancelFlaggedReservation` (from the case queue) → `ReservationCancelledIE` and `SettlementDecidedIE`; `HomaRestoreFlaggedReservation` → `ReservationRestoredIE`; `HomaCancelUndecidedFlaggedReservations` (scheduled, at each booked start) → `ReservationCancelledIE` and `SettlementDecidedIE` (R10)
- Handlers: `CaseDecidedIE` (resolve), `PaymentSecuredIE` (records the amount with `Reservation.confirmPayment`; R14), `PaymentFailedIE` (ends the request with `Reservation.paymentFailed`, and publishes nothing more; R12), `HomaPolicyChangedIE`, the listing events, the verification events (R6, R10)

**payment**
- Handlers:
  - `ReservationRequestedIE` (authorise the card or take the FPX payment → `PaymentSecuredIE`; a failure → `PaymentFailedIE`; R12, R14)
  - `ReservationEditedIE` (adjust the authorisation, take the FPX increase or refund the decrease → `PaymentSecuredIE` with the new amount; a failure → `PaymentFailedIE`)
  - `ReservationAcceptedIE` (capture → `PaymentCapturedIE`)
  - `ReservationDeclinedIE`, `ReservationExpiredIE` and `RequestCancelledIE` (end before acceptance with `cancelBeforeAcceptance`)
  - `SettlementDecidedIE` (settle, then a payout with fine deductions → `PayoutCreatedIE`; held at once for a flagged business → `PayoutHeldIE`)
  - `FineChargedIE`
  - `ProviderFlaggedIE` (hold every payable payout of the business → `PayoutHeldIE`) and `ProviderReverifiedIE` (new payouts are no longer held; payouts already held wait for the founders) (R10)
- Homa: `HomaWaiveFine` → `FineWaivedIE`; `HomaReleasePayout` → `PayoutHoldReleasedIE`; `HomaWithholdPayout` → `PayoutWithheldIE`
- Stripe reporting back (webhooks, with the same signature and repeat checks as R9): `StripeRecordFpxPaid` → `PaymentSecuredIE`; `StripeRecordPaymentFailed` → `PaymentFailedIE`; `StripeRecordRefund` → `PaymentRefundedIE`; `StripeRecordPayoutFailed` (shown in admin tracking and retried in Stripe)
- Queries: `CustomerViewPayments` (payments and refunds with their status), `ProviderViewIncome` (earnings, payouts, fines being deducted)

**resolution**
- Handlers: `ReportFiledIE`, `NoShowChallengedIE`, `CompletionDeadlineMissedIE`. Each opens a case, or adds evidence when a case is already open for that reservation.
- `HomaDecideCase` → `CaseDecidedIE`
- Query: `HomaViewCaseQueue`

**conversation**
- `CustomerStartChat`, `PartyPostMessage`
- Handlers: `EnquiryRequestedIE` (link), `EnquiryClosedIE` (archive), `CustomerRegisteredIE`

**discovery**
- `CustomerSearchProviders`: the listing projection, the policy projection, and U1 for availability → `SearchPerformedIE` (R13)
- Handlers: the listing events, `HomaPolicyChangedIE`, the verification events (R6)

**feedback**
- `CustomerRateReservation` → `RatingSubmittedIE` (`Rating.submit`, then `RatingSummary` for the provider and each rated team member)
- `HomaHideRating` → `RatingHiddenIE` (`Rating.hide`, then `RatingSummary.withdraw`)
- Queries: `CustomerViewProviderRatings` (the averages, each category once it has the minimum ratings to show, and written feedback, newest first), `CustomerViewTeamRatings` (each named person's overall average)
- Handlers: `ReservationRequestedIE`, `TeamNamedIE`, `ReservationCompletedIE`, `HomaPolicyChangedIE`

**privacy** (no domain layer)
- `HomaRecordDataRequest`, `HomaCloseDataRequest`; query: `HomaViewDataRequests`

**tracking** (no domain layer)
- The admin console's views: see Admin console below.

## Required rules

These rules come from decisions made after the domain phase. Each one needs a usecase test.

1. **R1. One event per verification outcome.** The KYC usecases publish the event for the
   `VerificationOutcome` the domain returns, and nothing for `UNCHANGED`. A reported lapse publishes
   `TeamMemberLapsedIE` or `ProviderVerificationLapsedIE`:

   | Outcome | Team member | Business |
   |---|---|---|
   | `VERIFIED` | `TeamMemberAddedIE`, after `TeamMember.add` (R7) | `ProviderVerifiedIE` |
   | `REJECTED` | `MemberVerificationRejectedIE` | `ProviderVerificationRejectedIE` |
   | `REVERIFIED` | `TeamMemberReverifiedIE` | `ProviderReverifiedIE` |
   | `RECHECK_FAILED` | `RecheckFailedIE` | `RecheckFailedIE` |
   | `UNCHANGED` | none | none |

2. **R2. A paused provider can't accept.** `ProviderAcceptRequest` refuses with
   `ListingPausedException` while reservation's listing projection says the listing is paused, from
   `ListingPausedIE` until `ListingResumedIE`. This covers requests made before the pause, whose
   snapshots still say the listing takes requests.
   - Such a request stays Requested and expires normally (`HomaExpireRequests` is unchanged).
   - If the provider re-verifies before the expiry, they can accept it.
   - Accepted reservations go ahead: no handler of `ListingPausedIE` changes a reservation.
   - The domain stays as it is. The pause lives in offering, so only the usecase can see it at
     acceptance.
3. **R3. Going offline never touches accepted reservations.** `ListingWentOfflineIE` and
   `ListingPausedIE` update projections only. No handler cancels or changes a reservation because of
   them.
4. **R4. The roster holds team members only, by role.** offering builds a provider's `TeamRoster` from
   members where `isTeamMember()` is true, as helpers or supervisors. A business is never a helper
   itself. It rebuilds the roster on `TeamMemberAddedIE`, `TeamMemberReverifiedIE`,
   `TeamMemberLapsedIE` and `TeamMemberRemovedIE`.
5. **R5. A lapsed team member named on an upcoming reservation.** The provider must name someone else before
   the start, and the reservation's existing "team must be on the roster" rule then applies.
6. **R6. Verified for the whole job.**
   - reservation and discovery keep each business's verified-until date, from `ProviderVerifiedIE`
     and `ProviderReverifiedIE`.
   - `CustomerRequestReservation`, `CustomerEditRequest` and `ProviderAcceptRequest` pass the due date
     as it is now to the domain, which refuses a job ending after it.
   - `CustomerSearchProviders` puts it on each `ProviderCandidate`.

7. **R7. A verified person is added straight away.** When `MemberVerification.recordKycResult`
   returns `VERIFIED`, the same usecase calls `TeamMember.add` and publishes `TeamMemberAddedIE`; there
   is no separate step for the provider.
8. **R8. Only verified people work.** After the location check, the provider app opens the KYC
   provider's face check for every named person, and the KYC provider reports one final result each. The customer sees each person's result and time. Someone who cannot work
   (`Reservation.canWork` is false) is shown to the customer as not working on the job, and the
   customer can report a missing helper.

9. **R9. The KYC provider is an integration behind one port.**
   - **The port:** `IdentityVerificationService` (out port in account and reservation) starts KYB,
     member checks, re-verifications, early re-checks and face checks. Homa sets no time limits and
     works out no due dates; the KYC provider does both.
   - **The adapter** (infra) translates the KYC provider's statuses into `KybResult`, `KycResult`, a
     lapse, or a face check result. A timed-out or abandoned session is `rejected()`. It checks webhook
     signatures and skips a repeated webhook by its event id. Swapping the KYC provider changes only
     this adapter.
   - **A guard:** Homa never marks anyone verified without the KYC provider's result. A result with no
     registered name, SSM registration or verified-until date is refused by the domain.
   - **A safety net:** if a result never arrives, a scheduled job asks the KYC provider for the status of
     checks that have been waiting too long.

10. **R10. A seriously flagged provider's reservations.** When `ProviderFlaggedIE` arrives:
    - reservation disputes every accepted reservation of that provider whose job isn't completed
      (`disputeForProviderFlag`) and publishes `ReservationFlaggedIE`. The customer is told straight
      away.
    - Each one appears in the founders' queue. `HomaCancelFlaggedReservation` cancels it with a full
      refund (`cancelForProviderFlag`), with no fine and no cancellation fee. Or, once the KYC provider
      has cleared the flag (the business passed again), `HomaRestoreFlaggedReservation` restores it
      before its booked start (`restoreAfterFlagCleared`). The usecase refuses to restore while the
      business is still flagged.
    - `HomaCancelUndecidedFlaggedReservations` runs at each booked start and cancels any still undecided
      (`isFlagCancellationDue`), with a full refund.
    - payment holds the business's payouts on `ProviderFlaggedIE` (`Payout.hold`), including payouts
      created later, until the founders release or withhold each (`HomaReleasePayout`,
      `HomaWithholdPayout`). Only payable payouts are sent to Stripe.
    - The handler runs promptly: the provider app tells the team on site to stop and leave, and the
      customer is told at the same moment. A job under way is cancelled with a full refund, with nothing
      paid for the partial work.
    - offering pauses the listing, as on a lapse.
    - reservation and discovery also update the verified-until date on `ReverificationRequiredIE`, as on
      the other verification events (R6).

11. **R11. A retired job or tool comes off the listings.** On `JobRetiredIE`, offering calls
    `withdrawJob` on every listing that offers the job, and on `ToolRetiredIE`, `withdrawTool`.
    - A listing that was live and still is publishes `ListingChangedIE`; one left with no job
      publishes `ListingWentOfflineIE`. A listing that didn't have the job or tool publishes nothing.
    - So the provider is told, it also publishes what the domain returned: `JobWithdrawnFromListingIE`
      (`WITHDRAWN` on a live listing), `ListingWentOfflineForRetiredJobIE` (`WITHDRAWN_AND_OFFLINE`:
      offer another job and go live again), or `ToolWithdrawnFromListingIE`. A draft that loses a job
      publishes nothing; the provider sees it when they next edit the listing.
    - Requests and reservations already made are untouched: they keep the offering they were made with
      (R3).
12. **R12. A request ends with one event, and never waits unpaid.**
    - A customer cancelling a request publishes `RequestCancelledIE` only. payment handles it like a
      decline: the card authorisation is released, or the FPX payment is refunded in full.
    - When the card is declined, or the FPX payment fails or is abandoned, payment creates no `Payment`
      and publishes `PaymentFailedIE`. reservation ends the request with `Reservation.paymentFailed`
      and publishes nothing more. The customer is told from `PaymentFailedIE`.
    - When a payment fails after an edit, payment first ends the payment it already holds with
      `cancelBeforeAcceptance` (the authorisation is released, or the FPX payment refunded in full),
      then publishes `PaymentFailedIE`.
    - A `PaymentFailedIE` for a reservation that is no longer a request is refused by the domain; the
      handler records it and changes nothing.
13. **R13. Every search is recorded, without the customer.** `CustomerSearchProviders` publishes
    `SearchPerformedIE` with the jobs, start, hours, helpers, city and the numbers of exact and close
    matches. It never carries the customer's id, name or address.

14. **R14. A request is paid before the provider answers.**
    - payment publishes `PaymentSecuredIE` with the amount when the card is authorised or the FPX
      payment is paid, and again after an edit, with the new amount.
    - reservation records it with `Reservation.confirmPayment(amount)`. The domain refuses an
      acceptance until the secured amount covers the total (`PaymentNotSecuredException`), so an edit
      that raises the total waits for payment again.
    - The provider is told about a new request from `PaymentSecuredIE`, not `ReservationRequestedIE`,
      and `ProviderViewRequests` lists only requests whose payment is secured.
    - The request's expiry still runs from when it was requested.
    - A `PaymentSecuredIE` for a reservation that is no longer a request is refused by the domain; the
      handler records it and changes nothing.

## Admin console

The founders' console. The rule: **the Homa console handles only what Homa alone can decide or see, and
links out for the rest.** The external providers have their own consoles for their own part:

| Need | Where | Why |
|---|---|---|
| Identity review: documents, selfies, watchlist hits, KYB evidence, re-verification setup | **KYC provider console** | It holds the evidence and the personal data; copying it into Homa adds risk under the PDPA and no value |
| Moving money: refund execution, failed payouts, chargebacks, connected accounts, fees, reconciliation, tax reports | **Stripe console** | Stripe is the source of truth for money |
| Marketplace decisions: cases, flagged providers, held payouts, fines, ratings, team removals, catalogue, Homa-set values | **Homa console** | Only Homa's rules decide these |
| Growth, retention and marketplace health | **Homa console** | Only Homa has bookings, searches, cities, providers and outcomes together |

The Homa console shows each record's **status** (verified until, lapsed, flagged; payment held,
refunded) and a **deep link** to the external console, using its reference id. It never copies
identity documents, selfies or card details.

Every admin usecase follows the conventions above: the actor is `Homa`, and the usecase lives in the
context that owns the rule, for example `HomaDecideCase implements HomaCaseDecider` in
`resolution/usecase`. Only the read-only views get a module of their own: `tracking` (below).

### Growth dashboard

The founders' incentive is **customer growth × provider growth × retention growth × revenue growth**.
The console's home screen shows those four, with trends, defined so they can't be inflated by
sign-ups that never book:

| Metric | Defined as | Broken down by |
|---|---|---|
| **Customer growth** | **Active customers**: booked at least one completed job in the last 90 days. Also new customers, and first-booking conversion | City, how they found Homa (shelf or search), month |
| **Provider growth** | **Active live providers**: live and completed a job in the last 30 days. Also the onboarding funnel: registered, KYB passed, first team member added, live, first job | City, job, team size |
| **Retention** | **Customer repeat rate**: a second booking within 60 days of the first. Also monthly cohorts, and provider retention: active providers still active the next month | Cohort, city, provider |
| **Revenue** | **Net service fee**: the service fee Homa keeps after refunds. Also total booking value and the take rate | City, month, provider |

**Guardrails** sit on the same screen, so growth never costs safety or fairness:
- request acceptance rate and response time;
- expiry, cancellation and no-show rates;
- dispute rate and refunds;
- face check failures, lapses and serious flags;
- the **fine waiver rate**, per provider and in total. A provider with repeated waivers is a pattern,
  not bad luck.

### Usecases at launch: decisions only Homa can make

| Usecase | What it does | Context | Domain call | Event |
|---|---|---|---|---|
| `HomaViewCaseQueue` | Open cases, oldest first, with time since opened | resolution | (query) | — |
| `HomaViewCase` | One case: the reservation, check-in, face checks, evidence and chat. Chat and evidence open only for a case | resolution, through query ports | (query) | — |
| `HomaDecideCase` | Release, partial refund or full refund, with a reason | resolution | `Case.decide` | `CaseDecidedIE` |
| `HomaViewFlaggedProviders` | Each seriously flagged business, its disputed reservations and its held payouts | tracking | (query) | — |
| `HomaCancelFlaggedReservation` | Cancel with a full refund, no fine | reservation | `cancelForProviderFlag` | `ReservationCancelledIE`, `SettlementDecidedIE` |
| `HomaRestoreFlaggedReservation` | Restore before the booked start, only once the KYC provider cleared the flag | reservation | `restoreAfterFlagCleared` | `ReservationRestoredIE` |
| `HomaReleasePayout`, `HomaWithholdPayout` | Release a held payout, or keep it back with a reason | payment | `Payout.releaseHold`, `Payout.withhold` | `PayoutHoldReleasedIE`, `PayoutWithheldIE` |
| `HomaWaiveFine` | Waive what is left of a fine, once, with a **reason category** (customer-caused, emergency, Homa fault, other) and free text. The usecase keeps the category in the audit log and passes the text to the domain | payment | `Fine.waive` | `FineWaivedIE` |
| `HomaHideRating` | Hide a rating that breaks the rules, with a reason; the averages update | feedback | `Rating.hide`, `RatingSummary.withdraw` | `RatingHiddenIE` |
| `HomaRemoveTeamMember` | Remove a team member who may not work | account | `TeamMember.remove` | `TeamMemberRemovedIE` |
| `HomaRequestEarlyRecheck` | Ask the KYC provider for an early re-check, with a reason | account (KYC port) | (none until the KYC provider reports) | — |
| `HomaAddJob`, `HomaRenameJob`, `HomaRetireJob`, the same for tools, and `HomaCreateShelf` | The job list, tool list and homepage shelves | catalogue | `JobList`, `ToolList`, `Shelf` | `JobAddedIE`, `JobRetiredIE`, … |
| `HomaChangePolicyValue` | Change one Homa-set value; a new version for new reservations only | policy | `HomaPolicy.change` | `HomaPolicyChangedIE` |
| `HomaRecordDataRequest`, `HomaCloseDataRequest` | Record and track a PDPA request: see, correct, copy or delete | privacy (usecase only) | (none) | — |
| `HomaViewProvider` | A provider's page: listing, verified-until date, team, go-live status, fines and waivers, cancellation count, reservations, held payouts; links to the KYC provider and Stripe | tracking | (query) | — |
| `HomaViewCustomer` | A customer's page: reservations, payment states, reports; links to Stripe | tracking | (query) | — |
| `HomaViewReservation` | A reservation's timeline: every status change, payment state, check-in, face check and decision, with times | tracking | (query) | — |
| `HomaViewGrowth` | The growth dashboard and the guardrails | tracking | (query) | — |

### Usecases soon after launch: levers that move the metrics

| Usecase | Moves | What it shows or does |
|---|---|---|
| `HomaViewSearchGaps` | Providers, customers | Searches with no match or only close matches, by city, job, day and time: where to recruit providers. Fed by `SearchPerformedIE` |
| `HomaViewStuckOnboarding` | Providers | Businesses stuck at KYB, with no team member, or missing go-live requirements (`missingForGoLive`), to follow up |
| `HomaViewSlowResponders` | Retention | Providers with low acceptance or many expired requests, the main cause of a customer's bad first experience |
| `HomaViewOneTimeCustomers` | Retention | Customers who completed one job and didn't book again within 30 days, for a book-again prompt |
| `HomaViewShelfPerformance` | Customers, revenue | Bookings and revenue per homepage shelf, to curate better |
| `HomaViewCity` | All four | Supply against demand per city, before opening a new city |

Later, when there is a team: roles (founders; support agents who read and reply but don't decide cases
or release payouts), saved views, and exports for investors.

### The `tracking` module

A read-only module with no domain layer. It **listens** to the events already in `ie/` and builds its
own tables:

| View | Fed by |
|---|---|
| Growth dashboard | `CustomerRegisteredIE`, `ReservationRequestedIE`, `ReservationAcceptedIE`, `ReservationCompletedIE`, `ReservationCancelledIE`, `SettlementDecidedIE`, `PaymentRefundedIE`, `ListingWentLiveIE`, `ProviderVerifiedIE`, `TeamMemberAddedIE` |
| Guardrails | `ReservationExpiredIE`, `ReservationDeclinedIE`, `ReservationCancelledIE`, `ReportFiledIE`, `CaseDecidedIE`, `FaceCheckRecordedIE`, `ProviderVerificationLapsedIE`, `ProviderFlaggedIE`, `FineChargedIE`, `FineWaivedIE` |
| Provider, customer and reservation pages | Every event about them |
| Flagged providers | `ProviderFlaggedIE`, `ReservationFlaggedIE`, `ReservationRestoredIE`, `PayoutHeldIE`, `PayoutHoldReleasedIE`, `PayoutWithheldIE` |

Nothing depends on `tracking`. If it falls behind, no business rule is affected. Its tests feed events
in and check the views; there are no domain rules to test.

### Rules for the console (required, each with a usecase test)

1. **A1. Every decision records who and why.** Each admin action takes the founder's id from
   authentication (`decidedBy`) and a reason. The domain keeps the reason, and the usecase writes who,
   what, why and when to the `AdminAuditLog` out port. This matters more because the founders'
   incentives depend on outcomes they also decide.
2. **A2. Red-code rules can't be overridden.** A flagged reservation is cancelled with a full refund or
   restored only once the flag is cleared; the console has no "let it go ahead" action.
3. **A3. No editing.** Ratings, chats, evidence, check-in and face check records are never edited. A
   rating can only be hidden, with a reason.
4. **A4. Personal data only where needed.** Chats and evidence open only for a case. Identity data
   stays in the KYC provider's console.
5. **A5. Fine waivers stay exceptional.** A waiver needs a reason category and text, is given once and
   only for what is left. It covers only the money: the cancellation still counts. The waiver rate is on the growth
   dashboard's guardrails.
6. **A6. Money moves only through Homa's rules.** The founders never refund or pay out directly in
   Stripe for a reservation; they decide in the Homa console, and payment instructs Stripe.

## Build order

1. The same order as the domain: catalogue, policy, account, offering, availability, reservation, payment,
   resolution, conversation, discovery, feedback.
2. privacy, then tracking, which listens to every context's events.
3. `app/`, the composition root, with only the U1 adapters needed for usecase tests across contexts.

Each context passes its gates before the next starts.

## Progress

| Context | Usecases | Tests | Mutants killed | Worst CRAP | Status |
|---|---|---|---|---|---|
| catalogue | 16: add, rename and retire a job or a tool; regroup a job; create a shelf; publish the service area; seven views | 85 | 73 of 73 | 3.0 | Done |
| policy and the rest | — | — | — | — | Not started |

Each built context also has a `<Context>UsecaseArchitectureTest`, which checks that:
- the usecase layer depends only on its own domain, `kernel`, `ie` and Java;
- in ports are interfaces, DTOs are records, and neither carries a domain object or an event;
- every usecase implements its own in port, and usecase exceptions live in `usecase/exception`.

## Open questions

### Decided

1. **U1. Availability during a request, an acceptance and a search.** Reservation and discovery need a
   live "is this provider free?", but a context can't call another. *Decided:* reservation and
   discovery each declare an out port, such as `ProviderAvailability`. `app/`, the composition root
   that depends on every context, implements it by calling availability's `AvailabilityCheck`. Neither
   context imports the other. With in-process events in one deployable, an acceptance's hold is in
   place before the next check.
2. **U2. The service area for offering.** *Decided:* catalogue publishes
   `ServiceAreaPublishedIE(cities)` when the service area is set up, and offering keeps the cities.
3. **U3. Retiring a job that providers offer.** *Decided:* the job comes off those listings, and a live
   listing left with no job goes offline until the provider offers a job again
   (`ProviderListing.withdrawJob`). A retired tool comes off the same way (`withdrawTool`). See R11.
4. **U4. Cancelling a request.** *Decided:* reservation publishes `RequestCancelledIE` for this ending,
   and payment handles it like a decline, with `cancelBeforeAcceptance`. See R12.
5. **U5. A payment that fails at request.** *Decided:* payment publishes `PaymentFailedIE`, and
   reservation ends the request with `Reservation.paymentFailed`. See R12.
6. **U6. `JobAddedIE` and `ToolAddedIE`.** *Done:* both are in `ie/` and in the domain plan's event
   table.
7. **U7. Loading aggregates from storage.** Domain constructors are private and generate new ids, so
   the infra phase will need a way to rebuild an aggregate as it was saved. *Decided:* settle it in the
   infra phase. Usecase tests keep the objects in memory.
8. **U8. Recording searches.** *Decided:* discovery publishes `SearchPerformedIE`, with no customer
   details. See R13.
9. **U9. Who decided.** *Decided:* a usecase-level audit log, `AdminAuditLog`, keeps `decidedBy` for
   every admin action; the domain objects keep only the reason. See A1.
10. **U10. Removing a provider.** *Decided:* dropped from the spec. When Homa learns a team member may
    not work, it removes the team member with `HomaRemoveTeamMember` and records what happened.
    Removing a provider is provider suspension, which is not in the MVP.
11. **U11. Fine waiver reason categories.** *Decided:* the categories (customer-caused, emergency, Homa
    fault, other) are kept by the usecase with the free text; the domain keeps the text it has today.
    Promote them to the domain only if a rule ever depends on them.
12. **U12. A provider could accept a request before it is paid.** `Reservation.request` creates the
    request first and payment answers afterwards, and an FPX payment takes minutes at the bank.
    *Decided:* payment publishes `PaymentSecuredIE`; reservation records the amount
    (`Reservation.confirmPayment`), and the domain refuses an acceptance until it covers the total. The
    provider is told about a request only once it is paid. See R14.
13. **U13. A shelf's jobs aren't checked against the job list.** *Decided:* `Shelf.create` takes the
    jobs providers can offer and refuses any other; a retired job comes off every shelf's search, and a
    shelf left with no job is no longer shown. The founders see every shelf, hidden ones included
    (`HomaViewShelves`).
