# Homa Usecase Development Plan

This plan covers the **usecase phase** (Backend Module Discipline §8, step 2): the `usecase` layer of each bounded context. It sits on the finished domain phase (`docs/homa-domain-plan.md`, `docs/domain-audit.md`). Infra comes after.

**Status:** draft, paused. Nothing in the usecase layer is built. The domain phase comes first; this plan waits for review, with the open questions at the end.

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
| offering | provider readiness, and the team roster by role | `ProviderVerifiedIE`, `ProviderVerificationRejectedIE`, `PayoutAccountReadyIE`, `TeamMemberAddedIE`, `TeamMemberReverifiedIE`, `TeamMemberLapsedIE`, `TeamMemberRemovedIE` |
| offering | whether the provider's re-verification has lapsed | `ProviderVerificationLapsedIE`, `ProviderReverifiedIE` |
| reservation | each live listing's `OfferingSnapshot`, and whether it is paused now | `ListingWentLiveIE`, `ListingChangedIE`, `ListingWentOfflineIE`, `ListingPausedIE`, `ListingResumedIE` |
| reservation, discovery | the policy in force | `HomaPolicyChangedIE` |
| reservation, discovery | each business's verified-until date | `ProviderVerifiedIE`, `ProviderReverifiedIE`, `ReverificationRequiredIE` |
| discovery | each live listing's search fields | the listing events |
| conversation | customer names | `CustomerRegisteredIE` |

Availability is the exception: a request needs a live answer (see U1).

## Usecases per context

`→` shows the event published.

**catalogue**
- `HomaAddJob`, `HomaRenameJob`, `HomaRetireJob` → `JobAddedIE`, `JobRetiredIE`
- `HomaAddTool`, `HomaRenameTool`, `HomaRetireTool` → `ToolAddedIE`, `ToolRetiredIE`
- `HomaCreateShelf`
- Queries: `ProviderViewJobList` (offered jobs by group), `ProviderViewToolList`, `CustomerViewShelves` (in order), `ProviderViewServiceArea`

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
- Stripe reporting back: `StripeRecordPayoutAccountReady` → `PayoutAccountReadyIE`

**offering**
- `ProviderDraftListing`, `ProviderSetProfile`, `ProviderOfferJobs`, `ProviderBringTools`, `ProviderSetPricing`, `ProviderSetHours`, `ProviderSetHelpers`, `ProviderSetSupervisorRule`, `ProviderClearSupervisorRule`, `ProviderCoverCities`, `ProviderChooseCancellationTier` → `ListingChangedIE` when live
- `ProviderGoLive` → `ListingWentLiveIE`
- Handlers: the catalogue, policy and account events above. A roster change can lead to `ListingWentOfflineIE`. `ProviderVerificationLapsedIE` pauses a live listing → `ListingPausedIE`; `ProviderReverifiedIE` resumes a paused one → `ListingResumedIE`.

**availability**
- `ProviderSetWindows`, `ProviderSetBuffer`, `CalendarSyncBusyBlocks` (scheduled)
- Handlers: `ReservationAcceptedIE` (hold), `ReservationCancelledIE` (release)
- Query port for U1: `AvailabilityCheck` (is a slot free, nearest free start)

**reservation**
- Customer: `CustomerCreateEnquiry` → `EnquiryCreatedIE`; `CustomerBookAgain`; `CustomerRequestReservation` → `ReservationRequestedIE` (and `EnquiryRequestedIE` when made from an enquiry); `CustomerEditRequest` → `ReservationEditedIE`; `CustomerViewCancellationTerms`; `CustomerCancelReservation`; `CustomerChallengeNoShow` → `NoShowChallengedIE`
- Provider: `ProviderAcceptRequest` → `ReservationAcceptedIE` (refused while the listing is paused, R2); `ProviderDeclineRequest`; `ProviderCancelReservation`; `ProviderNameTeam` → `TeamNamedIE`; `ProviderCheckIn` → `ProviderCheckedInIE`; `KycRecordFaceCheck` (the KYC provider's final result for each person) → `FaceCheckRecordedIE` (and `CheckInCompletedIE` on the first pass); `FaceCheckServiceUnavailable` → `CheckInCompletedIE`; `ProviderReportCustomerNoShow`; `ProviderSubmitCompletion` → `CompletionSubmittedIE`
- Either party: `PartyFileReport` → `ReportFiledIE`
- Scheduled: `HomaExpireRequests`, `HomaMarkProviderNoShows`, `HomaCloseNoShowReportWindows`, `HomaRemindTeams`, `HomaRemindCompletions`, `HomaMissCompletionDeadlines`, `HomaCloseDisputeWindows`, `HomaCloseEnquiries`
- Each ending publishes `ReservationCancelledIE` or `ReservationCompletedIE`, with `SettlementDecidedIE` and `FineChargedIE` as the domain returns them.
- Flagged providers: the `ProviderFlaggedIE` handler disputes each accepted reservation whose job isn't completed → `ReservationFlaggedIE`; `HomaCancelFlaggedReservation` (from the case queue) → `ReservationCancelledIE` and `SettlementDecidedIE`; `HomaRestoreFlaggedReservation` → `ReservationRestoredIE`; `HomaCancelUndecidedFlaggedReservations` (scheduled, at each booked start) → `ReservationCancelledIE` and `SettlementDecidedIE` (R10)
- Handlers: `CaseDecidedIE` (resolve), `HomaPolicyChangedIE`, the listing events

**payment**
- Handlers: `ReservationRequestedIE` (authorise card or take FPX), `ReservationEditedIE`, `ReservationAcceptedIE` (capture), `ReservationDeclinedIE` and `ReservationExpiredIE` (end before acceptance), `SettlementDecidedIE` (settle, then a payout with fine deductions), `FineChargedIE`
- `HomaWaiveFine` → `FineWaivedIE`
- Publishes `PaymentCapturedIE`, `PaymentRefundedIE`, `PayoutCreatedIE`

**resolution**
- Handlers: `ReportFiledIE`, `NoShowChallengedIE`, `CompletionDeadlineMissedIE`. Each opens a case, or adds evidence when a case is already open for that reservation.
- `HomaDecideCase` → `CaseDecidedIE`
- Query: `HomaViewCaseQueue`

**conversation**
- `CustomerStartChat`, `PartyPostMessage`
- Handlers: `EnquiryRequestedIE` (link), `EnquiryClosedIE` (archive), `CustomerRegisteredIE`

**discovery**
- `CustomerSearchProviders`: the listing projection, the policy projection, and U1 for availability
- Handlers: the listing events, `HomaPolicyChangedIE`

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

## Build order

Same as the domain: catalogue, policy, account, offering, availability, reservation, payment, resolution,
conversation, discovery. Then `app/`, the composition root, with only the U1 adapters needed for usecase
tests across contexts.

## Open questions

1. **U1. Availability during a request, an acceptance and a search.** Reservation and discovery need a
   live "is this provider free?", but a context can't call another.
   *Recommendation:* reservation and discovery each declare an out port, such as
   `ProviderAvailability`. `app/`, the composition root that depends on every context, implements it
   by calling availability's `AvailabilityCheck`. Neither context imports the other. With in-process
   events in one deployable, an acceptance's hold is in place before the next check.
2. **U2. The service area for offering.** `offering` needs the launch cities, and no event carries them.
   *Recommendation:* add `ServiceAreaPublishedIE(cities)`, published by catalogue when the service
   area is set up.
3. **U3. Retiring a job that providers offer.** *Recommendation:* the job is removed from those
   listings. A live listing left with no jobs goes offline (`ListingWentOfflineIE`) until the provider
   offers a job again. This needs a new domain method, `ProviderListing.withdrawJob`.
4. **U4. Cancelling a request.** `cancelByCustomer` on a request returns a full-refund `Settlement`,
   but `payment` must end a request with `cancelBeforeAcceptance`, not `settle`. *Recommendation:*
   publish `RequestCancelledIE` for this ending, handled like a decline, instead of `SettlementDecidedIE`.
5. **U5. A payment that fails at request.** A declined card, or an FPX payment the customer abandons,
   should end the request. *Recommendation:* payment publishes `PaymentFailedIE`, and reservation ends
   the request with a new domain method, `Reservation.paymentFailed`, so it is not left waiting for the
   provider.
6. **U6. `JobAddedIE` and `ToolAddedIE`.** These are already in `ie/` and in the domain plan's event table.
   Offering can't know a job exists without them; the table had only the retired events.
7. **U7. Loading aggregates from storage.** Domain constructors are private and generate new ids. The
   infra phase will need a way to rebuild an aggregate as it was saved. *Recommendation:* decide in the
   infra phase. Usecase tests keep the objects in memory.
