# Homa Usecase Development Plan

This plan covers the **usecase phase** (Backend Module Discipline §8, step 2): the `usecase` layer of each bounded context. It sits on the finished domain phase (`homa-domain-plan.md`, `domain-closing-report.md`). Infra comes after.

**Status:** draft for review. Nothing is built. The open questions at the end need the product owner's decision before the contexts they touch are built.

**Sources:** `homa-product-description.md` (the product spec), `homa-domain-plan.md`, `homa-domain-stories.md` (the ubiquitous language) and `backend_module_discipline.md`. When this plan and the spec disagree, the spec wins; fix this plan.

**Numbering:** the rule and question numbers here are new. The numbers of the earlier plan (in git history up to `170c533`) no longer apply; what it decided is carried in below.

## Contents

- Conventions
- How a context gets another context's data
- Usecases per context
- Scheduled steps
- Required rules
- Admin console rules
- Rules for the infra phase
- Build order
- Testing approach
- Open questions

## Conventions

- **Layout** (Discipline §3), per context:

  ```text
  <context>/usecase/
  ├── port/in/        one interface per usecase, and one per integration event handler
  ├── port/out/       repositories, projections, the context's IE publisher, outbound services
  ├── dto/            a request and a response record per usecase, never shared
  ├── exception/      orchestration failures, such as a record not found
  └── <Impl>.java     usecase and handler implementations
  ```

- **Naming** (Discipline §6): the implementation is named for the action and the interface for the actor of the action: `CustomerRequestReservation implements CustomerReservationRequester`, with `request(RequestReservationRequest)`. The actors are:

  | Actor | Who |
  |---|---|
  | `Customer` | A customer, or a guest when searching |
  | `Provider` | The business, through the provider app |
  | `Registrant` | Anyone registering, before they have an account |
  | `Party` | The customer or the provider, where either can act |
  | `Homa` | The founders in the admin console, or Homa running a scheduled step |
  | `Kyc`, `Stripe`, `Calendar` | An outside service reporting back |

- **Event handlers** (Discipline §7): `HandleJobRetiredIE implements JobRetiredIEHandler`, one per event per context.
- **Each usecase implements exactly one in port**, and no other usecase implements it.
- **Dependencies:** a usecase uses only its own context's domain, `kernel/` and `ie/`. It never uses another context.
- **Rules stay in the domain.** A usecase loads, calls the domain, saves and publishes. If a usecase starts deciding something, that rule belongs in the domain, and the domain is changed first.
- **Time:** usecases take a `java.time.Clock` and pass `Instant now` to the domain.
- **Events:** a usecase maps the domain's result into the `ie/` event, with `occurredAt`, and publishes it through the context's single `<Context>IEPublisher` out port. One event per outcome.
- **DTOs** hold primitives, Strings, kernel and JDK value types, the same as events. Domain objects never leave the usecase layer.
- **Errors:** domain exceptions pass through unchanged. Usecase exceptions cover orchestration only, such as `ReservationNotFoundException`.
- **Who is acting** comes in the request DTO, filled in by infra from authentication (rule I2). A usecase checks that the actor owns what they act on, for example `NotYourReservationException`.
- **Nothing changed, nothing done.** When the domain says a change changed nothing, the usecase saves, publishes and records nothing, and answers with things as they are.
- **Order of a change:** check everything first, then save, then publish, then record. A refused change saves, publishes and records nothing.
- **Handlers can be delivered twice or late.** A handler whose event no longer applies (for example a payment secured for a reservation that is no longer a request) changes nothing and does not fail.
- **Queries with no input** have a response DTO only.
- **A screen can call several queries.** A context shows only its own data and what it has events for. A screen that needs more, such as the provider page, is put together by the app from several query usecases.
- **Admin actions:** every usecase whose actor is a founder and that changes something takes the founder's id (`decidedBy`) and records who, what, why and when through an `AdminAuditLog` out port (rule A1). Each context declares its own port; one adapter in `app/` serves them all.
- **Modules with no domain layer:** `notification`, `tracking` and `privacy` have no business rules, so they have `usecase` and `infra` only. They are the only exceptions to the three layers of the discipline.

## How a context gets another context's data

Contexts talk only through `ie/`. A context that needs another's data keeps its own **projection**: a small copy, fed by event handlers and stored behind its own out port.

| Consumer | Keeps | Fed by |
|---|---|---|
| offering | The jobs and tools providers can pick | `JobAddedIE`, `JobRetiredIE`, `ToolAddedIE`, `ToolRetiredIE` |
| offering | The cities Homa serves | `ServiceAreaPublishedIE` |
| offering | Homa's duration range | `HomaPolicyChangedIE` |
| offering | Whether each business is ready for go-live, lapsed or flagged | `ProviderVerifiedIE`, `ProviderVerificationRejectedIE`, `ProviderReverifiedIE`, `ProviderVerificationLapsedIE`, `ProviderFlaggedIE`, `PayoutAccountReadyIE` |
| offering | Each business's team roster, by role | `TeamMemberAddedIE`, `TeamMemberRoleChangedIE`, `TeamMemberLapsedIE`, `TeamMemberReverifiedIE`, `TeamMemberRemovedIE` |
| reservation | Each live listing as an `OfferingSnapshot`, and whether it is paused now | `ListingWentLiveIE`, `ListingChangedIE`, `ListingWentOfflineIE`, `ListingPausedIE`, `ListingResumedIE` |
| reservation | Each business's current team roster, for naming the team | The same team events as offering |
| reservation | Each business's verified-until date, and whether it is flagged now | `ProviderVerifiedIE`, `ProviderReverifiedIE`, `ReverificationRequiredIE`, `ProviderFlaggedIE` |
| reservation | The policy in force, as a `PolicySnapshot` | `HomaPolicyChangedIE` |
| reservation | Each customer's access notes (and address: see Q1) | `CustomerRegisteredIE`, `CustomerAccessNotesChangedIE` |
| payment | Which businesses are flagged now, and which have a ready payout account | `ProviderFlaggedIE`, `ProviderReverifiedIE`, `PayoutAccountReadyIE` |
| payment | The fine deduction cap (see Q8) | `HomaPolicyChangedIE` |
| discovery | Each live listing's search fields, and whether it is paused | The listing events, `ListingHoursNarrowedIE`, the insurance events |
| discovery | Each business's verified-until date | `ProviderVerifiedIE`, `ProviderReverifiedIE`, `ReverificationRequiredIE` |
| discovery | Homa's search limits | `HomaPolicyChangedIE` |
| discovery | Job and tool names and job groups | `JobAddedIE`, `JobRenamedIE`, `JobRegroupedIE`, `ToolAddedIE`, `ToolRenamedIE` |
| discovery | Each provider's jobs done and cancellations | `ReservationCompletedIE`, `SettlementDecidedIE`, `FineChargedIE` |
| conversation | Who each enquiry and reservation belongs to, and customers' names | `EnquiryCreatedIE`, `ReservationRequestedIE`, `CustomerRegisteredIE`, `CustomerRenamedIE` |
| feedback | Each reservation's customer, provider, named team and when it became Completed | `ReservationRequestedIE`, `TeamNamedIE`, `TeamMemberDroppedIE`, `ReservationCompletedIE` |
| feedback | The rating window and minimum ratings to show (see Q8) | `HomaPolicyChangedIE` |
| resolution | Nothing beyond what the case events carry | — |
| notification | Who each reservation belongs to, and each person's language and device | The reservation, account and enquiry events |
| tracking | Its own views of everything | Every event |

**Availability is the exception** (decided earlier): a request, an acceptance and a search need a live answer. reservation and discovery each declare an out port, `ProviderAvailability` (is this slot free; the nearest free start). `app/`, the composition root, implements it by calling availability's query usecase. Neither context imports the other.

## Usecases per context

`→` shows the events published. A usecase with no arrow is a query, or publishes nothing.

### catalogue

| Usecase | Domain call | Publishes |
|---|---|---|
| `HomaAddJob`, `HomaRenameJob`, `HomaRegroupJob` | `JobList.add`, `rename`, `regroup` | `JobAddedIE`, `JobRenamedIE`, `JobRegroupedIE` |
| `HomaRetireJob` (with a reason) | `JobList.retire`, then `Showcase.withdrawJob` | `JobRetiredIE` |
| `HomaAddTool`, `HomaRenameTool`, `HomaRetireTool` (with a reason) | `ToolList.add`, `rename`, `retire` | `ToolAddedIE`, `ToolRenamedIE`, `ToolRetiredIE` |
| `HomaFeatureSearch`, `HomaRenameFeaturedSearch`, `HomaMoveFeaturedSearch`, `HomaRemoveFeaturedSearch` | `Showcase.feature`, `rename`, `move`, `remove` | — |
| `HomaPublishServiceArea` | `ServiceArea.launch` | `ServiceAreaPublishedIE` |

Queries: `HomaViewJobList`, `HomaViewToolList`, `HomaViewShowcase` (everything, retired and hidden included); `ProviderViewJobList` (jobs providers can pick, by group), `ProviderViewToolList`, `ProviderViewServiceArea`; `CustomerViewShowcase` (shown featured searches, in order), `CustomerViewJobList`.

### policy

| Usecase | Domain call | Publishes |
|---|---|---|
| `HomaLaunchPolicy` (version 1, once) | `HomaPolicy.initial` | `HomaPolicyChangedIE` |
| `HomaChangePolicyValue` (one value, with a reason) | `HomaPolicy.change` | `HomaPolicyChangedIE`, with the whole new version |

Query: `HomaViewPolicy`. The console names each value, such as `SERVICE_FEE`, and writes it by kind: a percentage (`15%`), an ISO-8601 period (`PT2H`), a count (`300`) or a range of hours (`1-6`). The usecase only translates; the domain checks each value and the rules across values.

### account

| Usecase | Domain call | Publishes |
|---|---|---|
| `RegistrantStartPhoneVerification` (the code comes from a `VerificationCodes` port and is sent by an `SmsSender` port after the save) | `PhoneVerification.start` | — |
| `RegistrantConfirmPhone` | `PhoneVerification.confirm` | — |
| `CustomerRegisterAccount` (a `Geocoder` port gives the city; the customer's pin gives the coordinates) | `CustomerAccount.register` | `CustomerRegisteredIE` |
| `CustomerRename`, `CustomerChangeAddress`, `CustomerChangeAccessNotes` | `rename`, `changeAddress`, `changeAccessNotes` | `CustomerRenamedIE`, (see Q1), `CustomerAccessNotesChangedIE` |
| `ProviderRegisterAccount` | `ProviderAccount.register` | — |
| `ProviderStartKyb` (opens the KYC provider's KYB pages; the same pages re-verify) | — | — |
| `ProviderAddPayoutAccount` (opens Stripe's onboarding) | — | — |
| `StripeRecordPayoutAccountReady` | `recordPayoutAccountReady` | `PayoutAccountReadyIE`, once |
| `ProviderStartAddingTeamMember` (role and live photo; then opens the on-the-spot check) | `MemberVerification.start` | `MemberVerificationStartedIE` |
| `KycRecordMemberVerificationResult` (with the date of birth and the photo match) | `MemberVerification.recordKycResult` | `MemberVerifiedIE`, or `MemberVerificationRejectedIE` with the reason |
| `ProviderConfirmTeamMember` (the lawful-employment tick, with the terms version) | `confirmEmployment`, then `TeamMember.add` | `TeamMemberAddedIE` |
| `ProviderChangeTeamMemberRole` | `TeamMember.changeRole` | `TeamMemberRoleChangedIE` |
| `ProviderRemoveTeamMember`, `HomaRemoveTeamMember` (with a reason) | `TeamMember.remove` | `TeamMemberRemovedIE` |
| `ProviderStartMemberReverification` (with the person present) | — | — |
| `KycRecordBusinessResult` | `ProviderAccount.recordKybResult` | By outcome (rule R1) |
| `KycRecordTeamMemberResult` | `TeamMember.recordKycResult` | By outcome (rule R1) |
| `KycRecordBusinessLapse`, `KycRecordMemberLapse` | `lapse` | `ProviderVerificationLapsedIE`, `TeamMemberLapsedIE` |
| `KycRecordBusinessReverificationRequired`, `KycRecordMemberReverificationRequired` | `requireReverification` | `ReverificationRequiredIE` |
| `KycRecordBusinessFlag` | `recordSeriousFlag` | `ProviderFlaggedIE` |
| `KycRecordMemberFlag` | `TeamMember.lapse` | `TeamMemberLapsedIE` |
| `HomaRequestBusinessRecheck`, `HomaRequestMemberRecheck` (with a reason; asks the KYC provider, and nothing changes until it reports) | — | — |

Queries: `CustomerViewAccount`; `ProviderViewAccount` (verification, SSM registration, verified-until date, payout account, ready for go-live); `ProviderViewTeam` (members with their verified-until dates, and anyone waiting for verification or for the confirmation); `CustomerViewProviderTeam` (registered names, roles, photos and last verification dates of team members only, for the provider page and "who is coming").

- A phone number has at most one customer account and one business account (`PhoneAlreadyRegisteredException`). A phone verification is used once.
- A business acts only on its own team (`NotYourTeamMemberException`).
- Out ports: the repositories, `AccountIEPublisher`, `SmsSender`, `VerificationCodes`, `Geocoder`, `IdentityVerificationService` (rule R9), `PayoutAccountService`, `AdminAuditLog`.

### offering

| Usecase | Domain call | Publishes |
|---|---|---|
| `ProviderDraftListing` | `ProviderListing.draft` | — |
| `ProviderSetProfile`, `ProviderOfferJobs`, `ProviderBringTools`, `ProviderSetPricing`, `ProviderSetHours`, `ProviderSetHelpers`, `ProviderSetSupervisorRule`, `ProviderClearSupervisorRule`, `ProviderCoverCities`, `ProviderChooseCancellationTier`, `ProviderChooseAcceptancePolicy` | The matching setter | `ListingChangedIE`, when the listing is live |
| `ProviderDeclareInsurance`, `ProviderClearInsurance` | `declareInsurance`, `clearInsurance` | `ProviderInsuranceDeclaredIE`, `ProviderInsuranceClearedIE` |
| `ProviderGoLive` (readiness from the account projection) | `goLive` | `ListingWentLiveIE` |

Queries: `ProviderViewListing` (with everything still missing for go-live).

Handlers:

| Event | Does | Publishes |
|---|---|---|
| The catalogue, service area and business readiness events | Update the projections | — |
| `JobRetiredIE` | `withdrawJob` on every listing that offers it (rule R8) | `ListingChangedIE` and `JobWithdrawnFromListingIE`, or `ListingWentOfflineIE` and `ListingWentOfflineForRetiredJobIE` |
| `ToolRetiredIE` | `withdrawTool` (rule R8) | `ListingChangedIE` and `ToolWithdrawnFromListingIE` |
| `HomaPolicyChangedIE` | `fitHours` on every listing (rule R8) | `ListingChangedIE` and `ListingHoursNarrowedIE`, or `ListingWentOfflineIE` and `ListingWentOfflineForHoursIE` |
| The five team events | Rebuild the roster and `updateRoster` (rule R4) | `ListingChangedIE`, or `ListingWentOfflineIE` |
| `ProviderVerificationLapsedIE`, `ProviderFlaggedIE` | `pause` a live listing | `ListingPausedIE` |
| `ProviderReverifiedIE` | `resume` a paused listing | `ListingResumedIE` |

### availability

| Usecase | Domain call |
|---|---|
| `ProviderSetAvailabilityWindows` | `setWindows` |
| `ProviderSetBufferBetweenJobs` | `setBufferBetweenJobs` |
| `ProviderConnectCalendar` (opens Google's or Apple's consent) | — |
| `CalendarSyncBusyBlocks` (scheduled, per connected calendar, through a `CalendarService` port) | `replaceBusyBlocks` |

Queries: `ProviderViewCalendar`; `AvailabilityChecker` (is a slot free; the nearest free start within a distance and between the earliest and latest bookable starts), which `app/` uses to implement `ProviderAvailability`.

Handlers: `ReservationAcceptedIE` → `hold`; `ReservationCancelledIE` and `ReservationCompletedIE` → `release`. availability publishes no events.

### reservation

**The customer**

| Usecase | Domain call | Publishes |
|---|---|---|
| `CustomerCreateEnquiry` | `Enquiry.create` | `EnquiryCreatedIE` |
| `CustomerBookAgain` (from a past enquiry or reservation, at the current address) | `Enquiry.bookAgain` | `EnquiryCreatedIE` |
| `CustomerViewReviewPage` (price and cancellation terms for chosen details, before requesting) | pricing and `cancellationTerms` | — |
| `CustomerRequestReservation` (rule R2) | `Reservation.request`, `changeAccessNotes`, `Enquiry.markRequested` | `EnquiryRequestedIE` when from an enquiry, then `ReservationRequestedIE` |
| `CustomerEditRequest` | `edit` | `ReservationEditedIE` |
| `CustomerChangeReservationAccessNotes` | `changeAccessNotes` | `AccessNotesChangedIE` |
| `CustomerCancelReservation` | `cancelByCustomer` | A request: `RequestCancelledIE` only. A reserved booking: `ReservationCancelledIE` and `SettlementDecidedIE` |
| `CustomerChallengeNoShow` | `challengeNoShow` | `NoShowChallengedIE` |

**The provider**

| Usecase | Domain call | Publishes |
|---|---|---|
| `ProviderAcceptRequest` (rules R2, R3) | `accept` | `ReservationAcceptedIE`; then `RequestDeclinedForTakenTimeIE` for each clashing request (rule R5) |
| `ProviderDeclineRequest` | `decline` | `ReservationDeclinedIE` |
| `ProviderCancelReservation` | `cancelByProvider` | `ReservationCancelledIE`, `SettlementDecidedIE`, and `FineChargedIE` when fined |
| `ProviderNameTeam` (against the roster as it is now) | `nameTeam` | `TeamNamedIE` |
| `ProviderCheckIn` (a `Geoservice` port measures the distance from the device) | `checkIn` | `ProviderCheckedInIE` |
| `ProviderStartFaceCheck` (opens the KYC provider's face check for a named person; if the service is down, see the next row) | — | — |
| `KycRecordFaceCheckUnavailable` | `recordFaceCheckUnavailable` | `CheckInCompletedIE` |
| `KycRecordFaceCheck` (the final result for one person) | `recordFaceCheck` | `FaceCheckRecordedIE`, and `CheckInCompletedIE` on the first pass |
| `ProviderReportCustomerNoShow` (rule R6) | `reportCustomerNoShow` | `ReservationCancelledIE` |
| `ProviderSubmitCompletion` (evidence already stored through an `EvidenceStore` port) | `submitCompletion` | `CompletionSubmittedIE` |

**Either party, and the founders**

| Usecase | Domain call | Publishes |
|---|---|---|
| `PartyFileReport` | `fileReport` | `ReportFiledIE` |
| `HomaCancelFlaggedReservation` (with a reason) | `cancelForProviderFlag` | `ReservationCancelledIE`, `SettlementDecidedIE` |
| `HomaRestoreFlaggedReservation` (only once the flag is cleared; rule R7) | `restoreAfterFlagCleared` | `ReservationRestoredIE` |

Queries: `CustomerViewReservations`, `CustomerViewReservation` (status, price, cancellation terms, named team ids, check-in time, each face check result and who can work, the dispute window's end); `ProviderViewRequests` (only requests whose payment is secured); `ProviderViewReservations`, `ProviderViewReservation` (with the access notes from acceptance); `CustomerViewEnquiries`.

Handlers:

| Event | Does | Publishes |
|---|---|---|
| `PaymentSecuredIE` | `confirmPayment`; for Instant book, checks availability and `acceptInstantly`, or `declineForTakenTime` if the time is no longer free (rule R3) | `ReservationAcceptedIE`, or `RequestDeclinedForTakenTimeIE` |
| `PaymentFailedIE` | `paymentFailed` | — |
| `CaseDecidedIE` | `resolve` | `ReservationCompletedIE`, or `ReservationCancelledIE` for a challenged no-show; `SettlementDecidedIE` |
| `ProviderFlaggedIE` | `disputeForProviderFlag` on every accepted reservation with no completion (rule R7) | `ReservationFlaggedIE` each |
| `TeamMemberLapsedIE`, `TeamMemberRemovedIE` | `dropFromTeam` on every reservation that names them | `TeamMemberDroppedIE` or `TeamMemberStoppedIE` |
| The listing, policy, verification, team and customer events | Update the projections | — |

Scheduled steps are listed under **Scheduled steps**.

Out ports: the repositories and projections, `ReservationIEPublisher`, `ProviderAvailability`, `Geoservice`, `IdentityVerificationService` (face checks), `EvidenceStore`, `AdminAuditLog`.

### payment

Handlers:

| Event | Does | Publishes |
|---|---|---|
| `ReservationRequestedIE` | Starts the payment with Stripe for the total (see Q3) | — |
| `ReservationEditedIE` | `adjustTotal`: a new card authorisation replaces the old one; FPX collects an increase or refunds a decrease | `PaymentSecuredIE` with the new amount, or `PaymentFailedIE` |
| `ReservationAcceptedIE` | `capture` | `PaymentCapturedIE` |
| `ReservationDeclinedIE`, `ReservationExpiredIE`, `RequestCancelledIE`, `RequestDeclinedForTakenTimeIE` | `cancelBeforeAcceptance` | `PaymentRefundedIE` for an FPX refund |
| `SettlementDecidedIE` | `settle`; refunds the customer; when the provider earns, `Payout.create` and `FineDeductionPolicy.apply`; a flagged business's payout is held at once | `PaymentRefundedIE`, `PayoutCreatedIE`, `PayoutHeldIE` |
| `FineChargedIE` | `Fine.charge` | — |
| `ChargebackDecidedIE` | `Fine.charge` with `CHARGEBACK`, when the provider covers more than RM0 | — |
| `ProviderFlaggedIE` | `Payout.hold` on every payable payout not yet paid out | `PayoutHeldIE` each |
| `ProviderReverifiedIE`, `PayoutAccountReadyIE` | Update the projections | — |

| Usecase | Domain call | Publishes |
|---|---|---|
| `StripeRecordCardAuthorised` | `Payment.authoriseCard` | `PaymentSecuredIE` |
| `StripeRecordFpxPaid` | `Payment.payUpfrontByFpx` | `PaymentSecuredIE` |
| `StripeRecordPaymentFailed` (rule R2) | — (no `Payment` exists) | `PaymentFailedIE` |
| `StripeRecordRefund`, `StripeRecordPayoutPaid`, `StripeRecordPayoutFailed` | Record the status the customer and provider see | — |
| `HomaWaiveFine` (a reason category and text) | `Fine.waive` | `FineWaivedIE` |
| `HomaReleasePayout`, `HomaWithholdPayout` (with a reason) | `releaseHold`, `withhold` | `PayoutHoldReleasedIE`, `PayoutWithheldIE` |

Queries: `CustomerViewPayments` (payments and refunds with their status and expected timing; receipts), `ProviderViewIncome` (earnings, payouts with their statements, fines being deducted).

Out ports: the repositories and projections, `PaymentIEPublisher`, `PaymentGateway` (authorise, charge, release, refund), `PayoutGateway`, `AdminAuditLog`. Only a payable payout is sent to Stripe.

### resolution

| Usecase | Domain call | Publishes |
|---|---|---|
| `HomaDecideCase` (with a reason) | `Case.decide` | `CaseDecidedIE`; for a chargeback case, `ChargebackDecidedIE` instead |
| `StripeRecordChargeback` (see Q4) | `Case.open` with `CHARGEBACK` | — |

Queries: `HomaViewCaseQueue` (open cases in `QUEUE_ORDER`, each with its age and decision target date; see Q9), `HomaViewCase`, `PartyViewCaseStatus` (the expected decision date and the outcome).

Handlers:

| Event | Does | Publishes |
|---|---|---|
| `ReportFiledIE`, `NoShowChallengedIE`, `CompletionDeadlineMissedIE` | `Case.open`, or `addEvidence` when a case is already open for that reservation; `markSafety` for a safety, damage or loss report | — |
| `CompletionSubmittedIE` | `addEvidence`, when a case is open for that reservation | — |
| `ReservationFlaggedIE` | `closeForProviderFlag` on the open case for that reservation | `CaseClosedForProviderFlagIE` |

### conversation

| Usecase | Domain call |
|---|---|
| `CustomerStartChat` (the first message on an enquiry) | `Conversation.startByCustomer` |
| `PartyPostMessage` (only the chat's customer or provider) | `post` |

Queries: `PartyViewChat`, `PartyViewInbox` (chats grouped by provider for a customer), `HomaViewChatForCase` (rule A4).

Handlers: `EnquiryCreatedIE` (remember who the enquiry belongs to, so the customer's first message starts its chat); `EnquiryRequestedIE` → `linkReservation`; `ReservationRequestedIE` → `openForReservation`, unless that reservation already has a chat; `EnquiryClosedIE` → `archive`; the customer name events. conversation publishes no events.

### discovery

| Usecase | Does | Publishes |
|---|---|---|
| `CustomerSearchProviders` (a guest gives a city) | `SearchCriteria.of`; builds a `ProviderCandidate` per listing in the city that takes requests, asking `ProviderAvailability` for the nearest free start; `MatchClassifier.classify` | `SearchPerformedIE` (rule R10) |

Queries: `CustomerViewProviderPage` (the listing, jobs by group, tools brought and not brought, the cancellation tier, Instant book, Insured, jobs done and cancellations).

Handlers: the projections listed above.

### feedback

| Usecase | Domain call | Publishes |
|---|---|---|
| `CustomerRateReservation` | `Rating.submit`, then `RatingSummary.record` for the provider and each rated team member | `RatingSubmittedIE` |
| `HomaHideRating` (with a reason) | `Rating.hide`, then `RatingSummary.withdraw` | `RatingHiddenIE` |

Queries: `CustomerViewProviderRatings` (the overall and category averages once shown, the count with "New" until then, and written feedback, newest first), `CustomerViewTeamRatings` (each team member's overall average), `HomaViewRatings`.

Handlers: the projections listed above. Only `ReservationCompletedIE` makes a reservation rateable.

### notification (no domain layer)

One handler per event that someone must be told about, each sending through a `PushSender` out port in the person's language. See Q6. Examples:

| Told | Events |
|---|---|
| The provider | `PaymentSecuredIE` (a new request to answer), `ReservationEditedIE`, `RequestCancelledIE`, `ReservationCancelledIE`, the team and completion reminders (Q5), `ReportFiledIE`, `CaseDecidedIE`, `FineChargedIE`, `FineWaivedIE`, `PayoutCreatedIE`, the payout hold events, the verification events, `JobWithdrawnFromListingIE` and the other listing notices |
| The customer | `ReservationAcceptedIE`, `ReservationDeclinedIE`, `RequestDeclinedForTakenTimeIE`, `ReservationExpiredIE`, `PaymentFailedIE`, `PaymentRefundedIE`, `TeamNamedIE`, `TeamMemberDroppedIE`, `TeamMemberStoppedIE`, `CheckInCompletedIE`, `FaceCheckRecordedIE`, `CompletionSubmittedIE`, `ReservationCancelledIE`, `ReservationFlaggedIE`, `ReservationRestoredIE`, `CaseDecidedIE`, `ReservationCompletedIE` (asked to rate) |

### privacy (no domain layer)

`HomaRecordDataRequest`, `HomaCloseDataRequest`; query: `HomaViewDataRequests`.

### tracking (no domain layer)

Read-only views for the admin console, built by listening to every event. Nothing depends on it.

- At launch: `HomaViewOverview` (the measures in the spec's Goals and Measures, with reservations, booking value, service fee earned, cancellation and no-show rates, the Instant book share and response times), `HomaViewReservation` (the full history), `HomaViewProvider`, `HomaViewCustomer`, `HomaViewPayments`, `HomaViewFines`, `HomaViewFlaggedProviders`.
- A request declined for taken time (`RequestDeclinedForTakenTimeIE`) is left out of a provider's response and acceptance rates.
- The console links out to the KYC provider's and Stripe's consoles for identity evidence and money movement; it copies neither.

## Scheduled steps

A scheduler calls each of these in ports every minute (the trigger lands in `infra/transport/messaging/`). Each takes no input, loads what is due at `now`, and handles each item on its own, so one failure doesn't stop the rest.

| Usecase | Context | Due when | Domain call | Publishes |
|---|---|---|---|---|
| `HomaExpireRequests` | reservation | `requestExpiresAt` has come | `expire` | `ReservationExpiredIE` |
| `HomaCloseEnquiries` | reservation | The enquiry's date has passed with no request | `isClosed` | `EnquiryClosedIE`, once |
| `HomaRemindTeams` | reservation | `isTeamReminderDue` | — | A reminder, once per time the team becomes incomplete (Q5) |
| `HomaMarkProviderNoShows` | reservation | The check-in period ended with no check-in | `markProviderNoShow` | `ReservationCancelledIE`, `SettlementDecidedIE`, `FineChargedIE` |
| `HomaCancelUndecidedFlaggedReservations` | reservation | `isFlagCancellationDue` | `cancelForProviderFlag` | `ReservationCancelledIE`, `SettlementDecidedIE` |
| `HomaReleaseNoShowFees` | reservation | The no-show report window ended with no challenge (rule R6) | — | `SettlementDecidedIE` |
| `HomaRemindCompletions` | reservation | `isCompletionReminderDue` | — | A reminder, once (Q5) |
| `HomaMissCompletionDeadlines` | reservation | The completion deadline passed with no completion | `missCompletionDeadline` | `CompletionDeadlineMissedIE` |
| `HomaCloseDisputeWindows` | reservation | `disputeWindowEndsAt` has come with no report | `closeDisputeWindow` | `ReservationCompletedIE`, `SettlementDecidedIE` |
| `CalendarSyncBusyBlocks` | availability | Every sync interval | `replaceBusyBlocks` | — |
| `HomaChaseWaitingVerifications` | account | A check has waited too long for its result (rule R9) | Asks the KYC provider for its status | As the result reports |

- **Done once.** Where the domain changes the status, the status stops a repeat. Where it doesn't (reminders, enquiry closing, the no-show fee), the usecase records that the step was done, through an out port, before it publishes.
- **Order within a minute** follows the table: expiry before anything else, and a provider no-show before a customer cancellation can be tried.

## Required rules

Each rule needs a usecase test.

1. **R1. One event per verification outcome.** The KYC usecases publish the event for the `VerificationOutcome` the domain returns, and nothing for `UNCHANGED`.

   | Outcome | Adding a team member | Team member | Business |
   |---|---|---|---|
   | `VERIFIED` | `MemberVerifiedIE`; the provider's confirmation then adds the person with `TeamMemberAddedIE` | — | `ProviderVerifiedIE` |
   | `REJECTED` | `MemberVerificationRejectedIE`, with the reason | — | `ProviderVerificationRejectedIE` |
   | `REVERIFIED` | — | `TeamMemberReverifiedIE` | `ProviderReverifiedIE` |
   | `RECHECK_FAILED` | — | `RecheckFailedIE` | `RecheckFailedIE` |
   | `UNCHANGED` | none | none | none |

2. **R2. A request is paid before the provider answers, and never waits unpaid.**
   - payment publishes `PaymentSecuredIE` with the amount once the card is authorised or the FPX payment is paid, and again after an edit. reservation records it with `confirmPayment`; the domain refuses an acceptance until it covers the total.
   - The provider is told about a request from `PaymentSecuredIE`, and `ProviderViewRequests` lists only requests whose payment is secured.
   - When the payment fails or is abandoned, payment publishes `PaymentFailedIE`; reservation ends the request with `paymentFailed` and publishes nothing more. If payment already holds a payment from before an edit, it ends that first with `cancelBeforeAcceptance`.
   - A request ends with one event: `ReservationDeclinedIE`, `ReservationExpiredIE`, `RequestCancelledIE` or `RequestDeclinedForTakenTimeIE`. payment handles all four the same way.
   - The request's expiry still runs from when it was requested.
3. **R3. Availability is checked at request, at edit and at acceptance.**
   - `CustomerRequestReservation` and `CustomerEditRequest` refuse a time the provider isn't free (`ProviderNotAvailableException`).
   - `ProviderAcceptRequest` refuses the same way, and the request stays Requested.
   - Instant book checks again when the payment is secured: if the time is no longer free, the request ends with `declineForTakenTime` and costs the customer nothing.
   - `ProviderAcceptRequest` also refuses while the listing is paused (`ListingPausedException`), from `ListingPausedIE` until `ListingResumedIE`, including a request made before the pause. That request stays Requested and expires normally unless the provider re-verifies in time.
4. **R4. The roster holds team members only, by role.** offering and reservation build a business's `TeamRoster` from members on the team: added, and not lapsed or removed. A lapsed member who passes rejoins it. A role change moves the person between helpers and supervisors.
5. **R5. Accepting a request declines the requests it makes impossible.** After an acceptance, by the provider or by Instant book, reservation asks `ProviderAvailability` about each of that provider's other open requests, and ends each one that is no longer free with `declineForTakenTime`. This runs once the accepted booking's hold is in place (see Q2).
6. **R6. A customer no-show fee is sent to payment only when its report window ends unchallenged.**
   - `ProviderReportCustomerNoShow` publishes `ReservationCancelledIE` and keeps the settlement the domain returned.
   - `HomaReleaseNoShowFees` publishes `SettlementDecidedIE` once the window has ended and the reservation was not challenged.
   - If the customer challenges, the kept settlement is dropped, and Homa's decision gives the only settlement sent.
   - A customer who cancels after check-in is different: that settlement is sent at once.
7. **R7. A seriously flagged business.** When `ProviderFlaggedIE` arrives:
   - reservation disputes every accepted reservation of that business whose completion hasn't been submitted, including one already disputed over the job, and publishes `ReservationFlaggedIE`. The handler runs promptly: the team on site and the customer are told at the same moment.
   - resolution closes the open case of a reservation the flag took over.
   - offering pauses the listing. payment holds the business's payable payouts, and every payout created while it is flagged.
   - The founders cancel each flagged reservation with a full refund, or restore it before its booked start. `HomaRestoreFlaggedReservation` refuses while the business is still flagged (`ProviderStillFlaggedException`); a pass (`ProviderReverifiedIE`) clears the flag.
   - `HomaCancelUndecidedFlaggedReservations` cancels any still undecided at its booked start.
   - A payout already held stays held after the flag is cleared, until the founders release or withhold it.
8. **R8. Homa's changes reach the listings, and the provider is told why.**
   - A retired job or tool comes off every listing that has it. A narrower duration range shrinks every listing's hours.
   - A listing that was live and still is publishes `ListingChangedIE`, so the projections follow, plus the notice for the provider (`JobWithdrawnFromListingIE`, `ToolWithdrawnFromListingIE`, `ListingHoursNarrowedIE`).
   - A live listing that goes offline publishes `ListingWentOfflineIE` plus its notice (`ListingWentOfflineForRetiredJobIE`, `ListingWentOfflineForHoursIE`).
   - A draft publishes nothing. A listing the change doesn't touch publishes nothing.
   - Going offline or pausing never touches a request or reservation already made: those handlers update projections only.
9. **R9. The KYC provider is an integration behind one port.**
   - `IdentityVerificationService` (an out port in account and in reservation) starts KYB, member checks, re-verifications, early re-checks and face checks. Homa sets no time limits and works out no dates.
   - The adapter translates the KYC provider's statuses into `KybResult`, `KycResult`, a lapse, a flag or a face check result. A timed-out or abandoned session is a rejection.
   - Homa never marks anyone verified without the KYC provider's result.
   - If a result never arrives, `HomaChaseWaitingVerifications` asks for it.
10. **R10. Every search is recorded, without the customer.** `SearchPerformedIE` carries the jobs, start, hours, helpers, city and the numbers of exact and close matches, and nothing about who searched. A paused listing is left out of search.
11. **R11. Only people who can work are shown as working.** After the location check, the provider app opens a face check for every named person. The customer sees each person's result and time, and anyone for whom `canWork` is false is shown as not working on the job.
12. **R12. Requesting from an enquiry keeps one chat.** `CustomerRequestReservation` publishes `EnquiryRequestedIE` before `ReservationRequestedIE`, so conversation links the enquiry's chat and does not open a second one.
13. **R13. A person who lapses or is removed comes off every upcoming job.** On `TeamMemberLapsedIE` and `TeamMemberRemovedIE`, reservation calls `dropFromTeam` on every reservation that names them and publishes `TeamMemberDroppedIE` (with whether the team is still complete) or `TeamMemberStoppedIE`. `NOT_AFFECTED` publishes nothing.

## Admin console rules

Each rule needs a usecase test.

1. **A1. Every decision records who and why.** A reason is required for a decision about a person, money or providers' listings. Plain curation, such as renaming a job, records who, what and when.
2. **A2. Red-code rules can't be overridden.** A flagged reservation is cancelled with a full refund, or restored once the flag is cleared; there is no "let it go ahead".
3. **A3. No editing.** Ratings, chats, evidence, check-in and face check records are never edited. A rating can only be hidden.
4. **A4. Personal data only where needed.** A chat and its evidence open only for a reservation that has a case, and each opening is recorded. Identity data stays with the KYC provider.
5. **A5. Fine waivers stay exceptional.** A waiver needs a reason category (customer-caused, emergency, Homa fault, other) and text. The usecase keeps the category in the audit log and passes the text to the domain. The cancellation still counts.
6. **A6. Money moves only through Homa's rules.** The founders decide in the Homa console, and payment instructs Stripe. They never refund or pay out directly in Stripe for a reservation.

## Rules for the infra phase

Decided earlier. They shape the out ports now, and each gets an infra test later.

1. **I1. A save never overwrites a change made meanwhile.** Every repository port refuses a save made on an out-of-date copy with the context's `ChangedMeanwhileException`. The Homa-set values are stricter: a save is refused unless its version is one more than the version in force.
2. **I2. Only authentication says who is acting.** `infra/security` sets `decidedBy`, the customer's id or the provider's id from the authenticated user, never from the request body, and lets only founders reach the `Homa*` in ports.
3. **I3. A change is all or nothing.** The save, the events and the audit entry of one usecase happen in one transaction.
4. **I4. Rebuilding aggregates from storage** is settled in the infra phase: the domain constructors are private and generate new ids. Usecase tests keep the objects in memory.

## Build order

The same order as the domain, so each context's handlers have events to listen to. Each context passes its gates before the next starts.

1. `catalogue`
2. `policy`
3. `account`: phone, customer and business first; then the team and the KYC provider's reports
4. `offering`
5. `availability`
6. `reservation`, in slices:
   1. projections, enquiries, the review page and requesting
   2. payment secured and failed, accept, Instant book, decline, taken time and expiry
   3. editing, access notes and cancelling
   4. naming the team, people coming off it, check-in and face checks
   5. no-shows
   6. completion, reports, the dispute window and decisions
   7. flagged businesses
7. `payment`
8. `resolution`
9. `conversation`
10. `discovery`
11. `feedback`
12. `notification`, `privacy`, then `tracking`

`app/`, with the `ProviderAvailability` adapter and the wiring, belongs to the infra phase.

## Testing approach

- **Framework-free**, with one fixed clock per test and in-memory fakes of the out ports, kept in test-support files per context.
- **A usecase test proves the orchestration:** what is loaded, what is passed to the domain, what is saved, which events are published in which order, and what is recorded. It does not test the domain's rules again; one refused case per usecase shows that a refusal saves and publishes nothing.
- **Naming** follows the domain plan: one class per usecase or feature, methods as camelCase sentences in the ubiquitous language, such as `theProviderIsToldAboutARequestOnlyOnceItIsPaid`.
- **Each required rule above** has a test named for it.
- **Gates per context:** unit tests; CRAP below 6; mutation at least 80% for `usecase`, with the ratchet in `quality/ratchet.json`; and a `<Context>UsecaseArchitectureTest` that checks:
  - the usecase layer depends only on its own domain, `kernel`, `ie` and Java;
  - in ports are interfaces, DTOs are records, and neither carries a domain object or an event;
  - every usecase implements exactly one in port, and usecase exceptions live in `usecase/exception`.
- **When a context is added,** its `usecase` package is added to PIT's `targetClasses` and `targetTests` in `pom.xml`.

## Open questions

Found while checking the domain code and the 82 events against the usecases above. Each has a recommendation. Q1 to Q5 change `ie/` or the domain, so they are settled before the context they touch is built.

1. **Q1. The customer's address never leaves `account`.** `CustomerRegisteredIE` carries the name and city only, and no event says an address changed. But a request, an enquiry and book again need the customer's current address, and check-in needs its map pin: reservation's `Address` is a street and a city, with no coordinates.
   *Recommended:* add the street and coordinates to `CustomerRegisteredIE`; add `CustomerAddressChangedIE`; reservation keeps the customer's current address in a projection, as it already does for access notes; and reservation's `Address` gains the coordinates, so each reservation keeps the pin it was made with. A guest's search still takes a city only.
2. **Q2. The hold must be in place before availability is asked again.** Rule R5 and two acceptances at the same moment both depend on it. availability holds the slot when it handles `ReservationAcceptedIE`.
   *Recommended:* in the default single deployable, availability handles `ReservationAcceptedIE` synchronously inside the accepting transaction. A hold that fails (`SlotNotAvailableException`) rolls the acceptance back, and the taken-time check runs after the event is published. If contexts are ever split, availability would publish its own "slot held" event and R5 would move to its handler.
3. **Q3. How the customer pays.** A card or FPX payment is completed by the customer in the app, with Stripe, after the request exists; Stripe then reports the result. The domain creates a `Payment` only once it is authorised or paid.
   *Recommended:* on `ReservationRequestedIE`, payment starts the payment through `PaymentGateway` and keeps a pending record (reservation, method, total, Stripe's reference) behind an out port. `CustomerViewPaymentToComplete` gives the app what it needs to finish paying. `StripeRecordCardAuthorised` and `StripeRecordFpxPaid` create the `Payment`; `StripeRecordPaymentFailed` ends it. A request that ends while still unpaid cancels the pending payment with Stripe.
4. **Q4. No event tells resolution that a chargeback arrived.** `ChargebackDecidedIE` exists for the decision, but nothing opens the case.
   *Recommended:* the chargeback webhook lands in resolution: `StripeRecordChargeback` opens the case with the amount the bank took back. No new event. Answering the bank with the evidence is done in Stripe's console.
5. **Q5. Reminders have no event.** The team reminder and the completion reminder are due by the domain (`isTeamReminderDue`, `isCompletionReminderDue`), but nothing carries them to whoever tells the provider.
   *Recommended:* two new events, `TeamReminderDueIE` and `CompletionReminderDueIE`, so everything a person is told goes one way. The alternative is a reminder out port in reservation.
6. **Q6. Who sends notifications.** The domain plan lists "(notifications)" as a listener but names no module.
   *Recommended:* a `notification` module with no domain layer, as above: one handler per event, a `PushSender` port, and each person's language.
7. **Q7. A named person whose role changes.** The provider can change a team member's role at any time, but a reservation may already name them as a helper or a supervisor.
   *Recommended:* before the booked start, they come off that job's team (`dropFromTeam`), and the provider names the team again. From the booked start, nothing changes. If agreed, the spec and the domain plan say so.
8. **Q8. Which version of a Homa-set value payment and feedback use.** Each reservation keeps the values in force when it was requested, but `SettlementDecidedIE` and `ReservationCompletedIE` carry none.
   *Recommended:* payment and feedback note the fine deduction cap and the rating window in force when they handle `ReservationRequestedIE`. The minimum ratings to show is a display value, so the current one applies.
9. **Q9. The decision target date.** "Within 3 working days" is shown to both parties and in the queue.
   *Recommended:* a domain query, `Case.decisionDueAt()`, counting Monday to Friday in Malaysian time, with no public holidays in the MVP.
10. **Q10. The provider page is put together by the app.** No single context has the listing, the team's photos and the ratings.
    *Recommended:* confirm the convention above: the app calls `CustomerViewProviderPage` (discovery), `CustomerViewProviderTeam` (account) and `CustomerViewProviderRatings` (feedback). "Who is coming" works the same way.
