# Homa Usecase Development Plan

This plan covers the **usecase phase** (Backend Module Discipline §8, step 2): the `usecase` layer of each bounded context. It sits on the finished domain phase (`docs/homa-domain-plan.md`, `docs/domain-audit.md`). Infra comes after.

**Status:** draft, paused. Nothing in the usecase layer is built. The domain phase comes first; this plan waits for review, with the open questions at the end.

## Conventions

- **Layout** (Discipline §3), per context:

  ```text
  <context>/usecase/
  ├── port/in/        one interface per usecase, and one per integration event handler
  ├── port/out/       repositories, the context's IE publisher, outbound clients (SMS, Kenal, Stripe, geoservice)
  ├── dto/            a request and a response record per usecase, never shared
  ├── exception/      orchestration failures, such as a record not found
  └── <Impl>.java     usecase and handler implementations
  ```

- **Naming** (Discipline §6): the implementation is named for the action and the interface for the actor
  of the action, as in `UserRegisterAccount implements UserAccountRegistrator`. Homa's actors are
  `Homa` (the founders, in the admin console), `Customer`, `Provider`, and the external services that
  report back (`Kenal`, `Stripe`). For example: `HomaAddJob implements HomaJobAdder`, with
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
| offering | provider readiness and the verified roster | `ProviderVerifiedIE`, `ProviderVerificationRejectedIE`, `PayoutAccountReadyIE`, `HelperVerifiedIE`, `HelperVerificationRejectedIE`, `HelperRemovedIE` |
| reservation | each live listing's `OfferingSnapshot` | `ListingWentLiveIE`, `ListingChangedIE`, `ListingWentOfflineIE` |
| reservation, discovery | the policy in force | `HomaPolicyChangedIE` |
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
- Provider: `ProviderRegisterAccount`, `ProviderStartEkyc` (Kenal out port), `ProviderIncludeSelfAsHelper`
- Helpers: `ProviderAddHelper`, `ProviderSendHelperEkycLink`, `ProviderRemoveHelper` → `HelperRemovedIE`
- Kenal reporting back: `KenalRecordProviderResult` → `ProviderVerifiedIE` or `ProviderVerificationRejectedIE`; `KenalRecordHelperResult` → `HelperVerifiedIE` or `HelperVerificationRejectedIE`
- Stripe reporting back: `StripeRecordPayoutAccountReady` → `PayoutAccountReadyIE`

**offering**
- `ProviderDraftListing`, `ProviderSetProfile`, `ProviderOfferJobs`, `ProviderBringTools`, `ProviderSetPricing`, `ProviderSetHours`, `ProviderSetHelpers`, `ProviderCoverCities`, `ProviderChooseCancellationTier` → `ListingChangedIE` when live
- `ProviderGoLive` → `ListingWentLiveIE`
- Handlers: the catalogue, policy and account events above. A roster change can lead to `ListingWentOfflineIE`.

**availability**
- `ProviderSetWindows`, `ProviderSetBuffer`, `CalendarSyncBusyBlocks` (scheduled)
- Handlers: `ReservationAcceptedIE` (hold), `ReservationCancelledIE` (release)
- Query port for U1: `AvailabilityCheck` (is a slot free, nearest free start)

**reservation**
- Customer: `CustomerCreateEnquiry` → `EnquiryCreatedIE`; `CustomerBookAgain`; `CustomerRequestReservation` → `ReservationRequestedIE` (and `EnquiryRequestedIE` when made from an enquiry); `CustomerEditRequest` → `ReservationEditedIE`; `CustomerViewCancellationTerms`; `CustomerCancelReservation`; `CustomerChallengeNoShow` → `NoShowChallengedIE`
- Provider: `ProviderAcceptRequest` → `ReservationAcceptedIE`; `ProviderDeclineRequest`; `ProviderCancelReservation`; `ProviderNameTeam` → `TeamNamedIE`; `ProviderCheckIn` → `ProviderCheckedInIE`; `ProviderReportCustomerNoShow`; `ProviderSubmitCompletion` → `CompletionSubmittedIE`
- Either party: `PartyFileReport` → `ReportFiledIE`
- Scheduled: `HomaExpireRequests`, `HomaMarkProviderNoShows`, `HomaCloseNoShowReportWindows`, `HomaRemindTeams`, `HomaRemindCompletions`, `HomaMissCompletionDeadlines`, `HomaCloseDisputeWindows`, `HomaCloseEnquiries`
- Each ending publishes `ReservationCancelledIE` or `ReservationCompletedIE`, with `SettlementDecidedIE` and `FineChargedIE` as the domain returns them.
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
