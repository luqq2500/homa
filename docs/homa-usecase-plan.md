# Homa Usecase Development Plan

This plan covers the **usecase phase** (Backend Module Discipline §8, step 2): the `usecase` layer of each bounded context, with its in ports, out ports, DTOs, event handlers, exceptions and tests. Infra comes last.

**Sources:** `homa-product-description.md` (the product spec), `homa-domain-plan.md` (the domain as built), `homa-domain-stories.md` (the ubiquitous language) and `backend_module_discipline.md`. When this plan and the spec disagree, the spec wins; fix this plan. When it and the discipline disagree, the discipline wins.

**Status:** draft for review. Nothing in this plan is built. The domain prerequisites (D1 to D10) and the open questions (U1 to U6) need the product owner's answer before the contexts they touch are built.

## Contents

- Conventions for the usecase phase
- Actors
- How a context gets another context's data
- Domain prerequisites
- Event map
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
- notification
- privacy
- tracking
- Rules for the admin console
- Rules for the infra phase
- Build order
- Testing approach
- Open questions

## Conventions for the usecase phase

- **Layout** (Discipline §3), per context:

  ```text
  <context>/usecase/
  ├── port/in/        one interface per usecase, and one per integration event handler
  ├── port/out/       repositories, the context's IE publisher, projections, outbound clients
  ├── dto/            a request and a response record per usecase, never shared
  ├── exception/      orchestration failures, such as a record not found
  └── <Impl>.java     usecase and handler implementations
  ```

- **One usecase, one service** (Discipline §6). Each usecase has its own in-port interface and its own
  implementation, and implements no other port. A usecase never calls another usecase.
- **Naming** (Discipline §6): the implementation is named for the action and the interface for the
  actor of the action, with one intent-revealing method:
  `CustomerRequestReservation implements CustomerReservationRequester` with
  `CustomerReservationRequester.request(RequestReservationRequest) : RequestReservationResponse`.
  The actors are listed under **Actors**.
- **DTOs** (Discipline §4, §6): every in-port method takes its own request record and returns its own
  response record, even a query: a query's request carries at least who is asking. DTOs hold
  primitives, Strings, `kernel` types and JDK value types; never a domain object or an event.
  `void` usecases still return a response record, so a controller always has a contract for OpenAPI.
- **Event handlers** (Discipline §7): one handler per event in each consuming context,
  `Handle<Event> implements <Event>Handler` with `handle(<Event> event)`. A handler takes the event
  itself; it needs no DTO. The listener in `infra/event/` injects the handler interface.
- **One publisher per context** (Discipline §7): `<Context>IEPublisher.publish(<Context>IE event)` in
  `port/out`, taking the context's event interface from `ie/port`.
- **Dependencies** (Discipline §5): a usecase uses only its own context's domain, `kernel` and `ie`.
  It never imports another context, a framework or a runtime SDK. Infra is reached only through
  `port/out`.
- **Rules stay in the domain.** A usecase loads, asks the domain, saves, publishes and records. If a
  usecase starts deciding a business rule, the rule belongs in the domain, and the domain is changed
  first (Discipline §8: domain first). The domain prerequisites below are such changes.
- **Time:** usecases take a `java.time.Clock` and pass `Instant now` to the domain.
- **Order of a change:** validate everything first (who is acting, the reason, the domain call), then
  save, then publish, then record in the audit log. A refused change saves, publishes and records
  nothing.
- **Nothing changed, nothing done.** When the domain says a change changed nothing (a rename to the same
  name, a repeated result from the KYC provider, a role set to the role it has), the usecase saves,
  publishes and records nothing, and answers with things as they are.
- **Idempotent handlers.** A handler that receives an event it has already applied changes nothing. Each
  aggregate's own rules make most repeats harmless; where they don't, the handler skips an event whose
  id it has recorded (`ProcessedEvents` out port).
- **Errors:** domain exceptions pass through unchanged. Usecase exceptions cover orchestration only:
  a record not found (`ReservationNotFoundException`), an actor acting on something not theirs
  (`NotYourReservationException`), a missing admin or reason.
- **Who is acting** comes only from authentication: `infra/security/` puts the customer's, provider's or
  admin's id into the request. A usecase trusts the id it is given and checks only that the record
  belongs to that actor.
- **Scheduled work** (Discipline §10): a scheduler trigger lands in `infra/transport/messaging/` and
  calls ordinary usecase ports. Each context with scheduled work has one query, `HomaListDueWork`, which
  lists the ids due for one kind of work, and one usecase per kind that acts on a single id. One id per
  call keeps each change small, retryable and inside any request time limit.
- **Outbound services** (the KYC provider, the payment provider, the SMS and push services, the
  geoservice, the calendar services) sit behind `port/out` interfaces named for what Homa needs, never
  for the vendor: `IdentityVerificationService`, `PaymentGateway`, `Geocoder`. Swapping a vendor changes
  only its client in `infra/transport/client/`.
- **Money** is `kernel.Money` throughout; DTOs carry it as is.
- **Tests:** see **Testing approach**. The mutation gate is **at least 80%** for every `usecase` layer,
  with the ratchet (Discipline §9).

## Actors

The actor names the port and the usecase. Each is a word from the ubiquitous language.

| Actor | Who acts | Example |
|---|---|---|
| `Customer` | A homeowner, signed in or, for search and browse, a guest | `CustomerRequestReservation` |
| `Registrant` | Anyone verifying a phone number to register | `RegistrantConfirmPhone` |
| `Provider` | A business provider, through its owner or director in the provider app | `ProviderAcceptRequest` |
| `Admin` | Homa's admins, in the admin console: the founders, for now | `AdminDecideCase` |
| `Homa` | The platform acting on its own: scheduled steps and reminders | `HomaExpireRequest` |
| `KycProvider` | The KYC provider reporting a result | `KycProviderRecordKybResult` |
| `PaymentProvider` | The payment provider reporting a payment, refund, payout or chargeback | `PaymentProviderRecordPaymentSecured` |

`Admin` and `Homa` are different actors: `Admin…` usecases are people acting in the admin console, and
`Homa…` usecases are the platform acting on its own, as in the domain stories.

## How a context gets another context's data

Contexts communicate only through integration events (Discipline §4). A context that needs another's
data keeps its own **projection**, fed by its event handlers and stored behind its own out port:

| Consumer | Projection (out port) | Fed by |
|---|---|---|
| offering | `CatalogueView`: active jobs and tools | `JobAddedIE`, `JobRetiredIE`, `ToolAddedIE`, `ToolRetiredIE` |
| offering | `ServiceAreaView`: the cities Homa serves | `ServiceAreaPublishedIE` |
| offering | `HomaLimitsView`: Homa's duration range | `HomaPolicyChangedIE` |
| offering | `ProviderReadinessView`: verified, lapsed or flagged, payout account ready | `ProviderVerifiedIE`, `ProviderVerificationRejectedIE`, `ProviderReverifiedIE`, `ProviderVerificationLapsedIE`, `ProviderFlaggedIE`, `PayoutAccountReadyIE` |
| offering | `TeamRosterView`: team members by role and status | `TeamMemberAddedIE`, `TeamMemberRoleChangedIE`, `TeamMemberReverifiedIE`, `TeamMemberLapsedIE`, `TeamMemberRemovedIE` |
| discovery | `ListingSearchView`: each live listing's search fields, Instant book | the listing events |
| discovery | `FreeTimeView`: each provider's free time | `ProviderFreeTimeChangedIE` (D1) |
| discovery | `VerifiedUntilView` | `ProviderVerifiedIE`, `ProviderReverifiedIE`, `ReverificationRequiredIE` |
| discovery | `SearchLimitsView` | `HomaPolicyChangedIE` |
| discovery | `CatalogueNamesView`: job and tool names and groups | `JobAddedIE`, `JobRenamedIE`, `JobRegroupedIE`, `ToolAddedIE`, `ToolRenamedIE` |
| reservation | `OfferingView`: each live listing's `OfferingSnapshot` and whether it takes requests now | the listing events |
| reservation | `PolicyView`: the policy in force | `HomaPolicyChangedIE` |
| reservation | `ProviderStandingView`: verified-until date and whether seriously flagged | `ProviderVerifiedIE`, `ProviderReverifiedIE`, `ReverificationRequiredIE`, `ProviderFlaggedIE` |
| reservation | `TeamRosterView`: team members by role and status | the team member events |
| reservation | `CustomerView`: address and access notes | `CustomerRegisteredIE`, `CustomerAddressChangedIE` (D2), `CustomerAccessNotesChangedIE` |
| reservation | `FreeTimeView` | `ProviderFreeTimeChangedIE` (D1) |
| payment | `ReservationMoneyView`: customer, provider, method and total | `ReservationRequestedIE`, `ReservationEditedIE` |
| payment | `PayoutStandingView`: payout account ready, flagged | `PayoutAccountReadyIE`, `ProviderFlaggedIE`, `ProviderReverifiedIE` |
| payment | `FineCapView` | `HomaPolicyChangedIE` |
| resolution | — (cases carry what they need) | |
| conversation | `PeopleView`: customer names, provider display names | `CustomerRegisteredIE`, `CustomerRenamedIE`, the listing events |
| conversation | `OpenCaseView`: reservations with an open case | `CaseOpenedIE` (D6), `CaseDecidedIE`, `CaseClosedForProviderFlagIE` |
| feedback | `RateableReservationView` | `ReservationRequestedIE`, `TeamNamedIE`, `ReservationCompletedIE` |
| feedback | `FeedbackLimitsView` | `HomaPolicyChangedIE` |
| notification | `RecipientsView`: who a reservation's people are | `ReservationRequestedIE`, `TeamNamedIE` |
| tracking | its own views of everything | every event |

**Screens compose queries; contexts don't.** A screen that shows data from several contexts calls each
context's own query, and the client puts them together. For example, the provider page is:
`CustomerViewListing` (offering), `CustomerViewProviderTeam` (account), `CustomerViewProviderRecord`
(reservation), `CustomerViewProviderRatings` and `CustomerViewTeamRatings` (feedback). No context
reads another's store.

## Domain prerequisites

Writing the usecases against the discipline found ten places where the domain or `ie/` must change
first. Discipline §8 puts domain work before usecase work, so each is built and gated before the
contexts that need it. **Each needs the product owner's confirmation.**

| # | Prerequisite | Why the usecases need it | Change |
|---|---|---|---|
| D1 | **Availability publishes free time** | Reservation and discovery need to know when a provider is free, and contexts talk only through events (Discipline §4), so they can't ask availability directly (see U1) | `availability`: `ProviderCalendar.freeTime(from, to)` returns the free stretches (windows, less busy blocks, less holds widened by the buffer). `discovery` and `reservation`: a `FreeTime` value with `isFree(slot)` and `nearestStart(wanted, within, earliest, latest)`, moved from `ProviderCalendar.nearestAvailableStart`. `ie/`: `ProviderFreeTimeChangedIE` |
| D2 | **Customer address reaches reservation** | A request needs the customer's address, coordinates and city, and check-in needs the coordinates. `CustomerRegisteredIE` carries only the city, and no event tells of a new address | `ie/`: `CustomerRegisteredIE` gains the street and coordinates; new `CustomerAddressChangedIE` |
| D3 | **Chargebacks reach resolution** | The payment provider reports a chargeback to payment, but cases live in resolution | `payment`: `Payment.recordChargeback(amount, now)`. `ie/`: `ChargebackOpenedIE` |
| D4 | **Reminders are sent once** | `isTeamReminderDue` and `isCompletionReminderDue` stay true, so a scheduled reminder would repeat | `reservation`: `recordTeamReminded(now)` and `recordCompletionReminded(now)`; a team that becomes incomplete again is due again. `ie/`: `TeamReminderDueIE`, `CompletionReminderDueIE` |
| D5 | **The provider record** | "Jobs done" and "cancellations" on the provider page are business definitions | `reservation`: a `ProviderRecord` read model rule: jobs done are Completed with a provider earning above RM0; cancellations are fined late cancellations and no-shows, waived or not |
| D6 | **A case says it opened** | Admins open chats and evidence only for a case, and the contexts that hold them must know a case is open | `ie/`: `CaseOpenedIE` |
| D7 | **A paused listing can't accept, in the domain** | Whether a paused listing can accept is a business rule, and rules stay in the domain, not the usecase | `reservation`: `accept` and `acceptInstantly` take whether the listing takes requests now, and refuse with `ListingPausedException` |
| D8 | **A `notification` context** | The spec tells people of dozens of moments, in their own language, by push or SMS. That is a business of its own: who is told what, in which language, on which channel | New context: `Notice`, `NoticeKind`, `Language`, `Channel`; rules: each person's language (Bahasa Melayu or English), the channel per kind (SMS as well as push for a job on hold, a provider no-show and a team change on the day) |
| D9 | **A `privacy` context** | Data requests have a rule: Homa answers within the PDPA's 21 days | New context: `DataRequest` (kind: see, correct, copy, delete; received, due, closed) |
| D10 | **A `tracking` context** | The goals and measures in the spec's Part 1 are business definitions | New context: the measure definitions (active customer, active live provider, repeat booking within 60 days, first reservation within 7 days, decided within 72 hours) |

The discipline requires every bounded context to have all three layers, so `notification`, `privacy`
and `tracking` each get a domain layer before their usecases.

## Event map

Who publishes each event, and which contexts handle it. Every handler is listed in its context's
section. Events marked (D) come from a domain prerequisite.

| Publisher | Events | Handled by |
|---|---|---|
| catalogue | `JobAddedIE`, `JobRetiredIE`, `ToolAddedIE`, `ToolRetiredIE` | offering, discovery (names), tracking |
| catalogue | `JobRenamedIE`, `JobRegroupedIE`, `ToolRenamedIE` | discovery, tracking |
| catalogue | `ServiceAreaPublishedIE` | offering |
| policy | `HomaPolicyChangedIE` | offering, discovery, reservation, payment, feedback, tracking |
| account | `CustomerRegisteredIE`, `CustomerAddressChangedIE` (D), `CustomerAccessNotesChangedIE`, `CustomerRenamedIE` | reservation, conversation, notification, tracking |
| account | `ProviderVerifiedIE`, `ProviderReverifiedIE`, `ReverificationRequiredIE` | offering, discovery, reservation, payment, tracking |
| account | `ProviderVerificationRejectedIE`, `ProviderVerificationLapsedIE`, `ProviderFlaggedIE` | offering, reservation, payment, notification, tracking |
| account | `PayoutAccountReadyIE` | offering, payment |
| account | `MemberVerificationStartedIE`, `MemberVerifiedIE`, `MemberVerificationRejectedIE`, `RecheckFailedIE` | notification, tracking |
| account | `TeamMemberAddedIE`, `TeamMemberRoleChangedIE`, `TeamMemberReverifiedIE`, `TeamMemberLapsedIE`, `TeamMemberRemovedIE` | offering, reservation, notification, tracking |
| offering | `ListingWentLiveIE`, `ListingChangedIE`, `ListingWentOfflineIE`, `ListingPausedIE`, `ListingResumedIE`, `ListingHoursNarrowedIE`, `ListingWentOfflineForHoursIE`, `ListingWentOfflineForRetiredJobIE` | discovery, reservation, conversation, notification, tracking |
| offering | `JobWithdrawnFromListingIE`, `ToolWithdrawnFromListingIE`, `ProviderInsuranceDeclaredIE`, `ProviderInsuranceClearedIE` | notification, tracking |
| availability | `ProviderFreeTimeChangedIE` (D) | discovery, reservation |
| discovery | `SearchPerformedIE` | tracking |
| reservation | `EnquiryCreatedIE`, `EnquiryRequestedIE`, `EnquiryClosedIE` | conversation, tracking |
| reservation | `ReservationRequestedIE`, `ReservationEditedIE` | payment, conversation, feedback, notification, tracking |
| reservation | `ReservationAcceptedIE` | availability, payment, notification, tracking |
| reservation | `ReservationDeclinedIE`, `ReservationExpiredIE`, `RequestCancelledIE`, `RequestDeclinedForTakenTimeIE` | payment, notification, tracking |
| reservation | `ReservationCancelledIE` | availability, notification, tracking |
| reservation | `ReservationCompletedIE` | feedback, notification, tracking |
| reservation | `SettlementDecidedIE`, `FineChargedIE` | payment, tracking |
| reservation | `TeamNamedIE` | feedback, notification, tracking |
| reservation | `TeamMemberDroppedIE`, `TeamMemberStoppedIE`, `TeamReminderDueIE` (D), `CompletionReminderDueIE` (D) | notification |
| reservation | `ProviderCheckedInIE`, `FaceCheckRecordedIE`, `CheckInCompletedIE`, `CompletionSubmittedIE`, `AccessNotesChangedIE`, `ReservationRestoredIE` | notification, tracking |
| reservation | `ReportFiledIE`, `NoShowChallengedIE`, `CompletionDeadlineMissedIE`, `ReservationFlaggedIE` | resolution, notification, tracking |
| payment | `PaymentSecuredIE`, `PaymentFailedIE` | reservation, notification, tracking |
| payment | `ChargebackOpenedIE` (D) | resolution |
| payment | `PaymentCapturedIE`, `PaymentRefundedIE`, `PayoutCreatedIE`, `PayoutHeldIE`, `PayoutHoldReleasedIE`, `PayoutWithheldIE`, `FineWaivedIE` | notification, tracking |
| resolution | `CaseOpenedIE` (D) | conversation, reservation, notification, tracking |
| resolution | `CaseDecidedIE` | reservation, conversation, notification, tracking |
| resolution | `ChargebackDecidedIE` | payment, notification, tracking |
| resolution | `CaseClosedForProviderFlagIE` | conversation, tracking |
| feedback | `RatingSubmittedIE`, `RatingHiddenIE` | notification, tracking |

---

## catalogue

**Purpose:** admins curate the job list, the tool list, the service area and the homepage
showcase; providers and customers read them.

**Out ports:** `JobListRepository`, `ToolListRepository`, `ShowcaseRepository`,
`ServiceAreaRepository`, `CatalogueIEPublisher`, `AdminAuditLog`.

| Usecase | In port · method | Domain | Publishes |
|---|---|---|---|
| `AdminAddJob` | `AdminJobAdder.add` | `JobList.add` | `JobAddedIE` |
| `AdminRenameJob` | `AdminJobRenamer.rename` | `JobList.rename` (nothing when unchanged) | `JobRenamedIE` |
| `AdminRegroupJob` | `AdminJobRegrouper.regroup` | `JobList.regroup` | `JobRegroupedIE` |
| `AdminRetireJob` | `AdminJobRetirer.retire` (with a reason) | `JobList.retire`, then `Showcase.withdrawJob` | `JobRetiredIE` |
| `AdminAddTool` | `AdminToolAdder.add` | `ToolList.add` | `ToolAddedIE` |
| `AdminRenameTool` | `AdminToolRenamer.rename` | `ToolList.rename` | `ToolRenamedIE` |
| `AdminRetireTool` | `AdminToolRetirer.retire` (with a reason) | `ToolList.retire` | `ToolRetiredIE` |
| `AdminFeatureSearch` | `AdminSearchFeaturer.feature` | `Showcase.feature` (jobs providers can offer only) | — |
| `AdminRenameFeaturedSearch` | `AdminFeaturedSearchRenamer.rename` | `Showcase.rename` | — |
| `AdminMoveFeaturedSearch` | `AdminFeaturedSearchMover.move` | `Showcase.move` | — |
| `AdminRemoveFeaturedSearch` | `AdminFeaturedSearchRemover.remove` | `Showcase.remove` | — |
| `AdminPublishServiceArea` | `AdminServiceAreaPublisher.publish` | `ServiceArea.launch` | `ServiceAreaPublishedIE` |
| `AdminViewJobList`, `AdminViewToolList`, `AdminViewShowcase` | `…Viewer.view` | (query: everything, retired and hidden included) | — |
| `ProviderViewJobList`, `ProviderViewToolList`, `ProviderViewServiceArea` | `…Viewer.view` | (query: what providers can choose) | — |
| `CustomerViewJobList`, `CustomerViewShowcase` | `…Viewer.view` | (query: offered jobs by group; the showcase in order) | — |

**Rules**
- **C1.** Retiring a job or tool needs a reason, because it changes providers' listings; plain curation
  records who, what and when, with no reason (A1).
- **C2.** Retiring a job takes it off every featured search in the same change, and a featured search
  left with no job is no longer shown.

```gherkin
Feature: Curating the catalogue

  Scenario: Retiring a job is told to the other contexts and recorded with its reason
    Given the job list offers KITCHEN_GREASE, featured in "Weekend quick refresh"
    When admin Aisyah retires KITCHEN_GREASE because "Replaced by kitchen care"
    Then the job list is saved with KITCHEN_GREASE retired
    And the showcase is saved without it
    And JobRetiredIE is published for KITCHEN_GREASE
    And the audit log records Aisyah, the retirement and the reason

  Scenario: A retirement without a reason changes nothing
    When admin Aisyah retires KITCHEN_GREASE with no reason
    Then it is refused with AdminReasonRequiredException
    And nothing is saved, published or recorded

  Scenario: Renaming a job to its own name changes nothing
    When admin Aisyah renames KITCHEN_GREASE to its own name
    Then nothing is saved, published or recorded
```

---

## policy

**Purpose:** admins launch and change the Homa-set values; the other contexts keep a copy.

**Out ports:** `PolicyRepository`, `PolicyIEPublisher`, `AdminAuditLog`.

| Usecase | In port · method | Domain | Publishes |
|---|---|---|---|
| `AdminLaunchPolicy` | `AdminPolicyLauncher.launch` | `HomaPolicy.initial` (version 1, once) | `HomaPolicyChangedIE` |
| `AdminChangePolicyValue` | `AdminPolicyValueChanger.change` (one value, with a reason) | `HomaPolicy.change` | `HomaPolicyChangedIE` |
| `AdminViewPolicy` | `AdminPolicyViewer.view` | (query: the 26 values in force) | — |

**Rules**
- **P1.** Each value has a name (`SERVICE_FEE`, `RESPONSE_DEADLINE`, …) and is written by kind: a
  percentage (`17.5%`), an ISO-8601 period in whole minutes (`PT2H`, `P30D`), a count (`300`) or a range
  of hours (`1-6`). The view shows each value by the same name and in the same form, so the console can
  send it back changed. The usecase only translates; the domain checks every value and every rule
  across values.
- **P2.** `HomaPolicyChangedIE` carries every value of the new version, so listeners keep a full copy.
- **P3.** The policy is saved only as the next version; a save on an out-of-date version is refused
  (`ChangedMeanwhileException`).

```gherkin
Feature: Changing a Homa-set value

  Scenario: A change makes a new version for new reservations
    Given policy version 1 is in force
    When admin Aisyah changes RESPONSE_DEADLINE to PT30M because "More time to answer"
    Then version 2 is saved with a response deadline of 30 minutes
    And HomaPolicyChangedIE is published with version 2 and every value
    And the audit log records Aisyah, the change and the reason

  Scenario: A value that breaks a rule across values is refused
    When admin Aisyah changes RESPONSE_DEADLINE to PT2H while the minimum notice is 2 hours
    Then it is refused with ResponseDeadlineNotBeforeMinimumNoticeException
    And nothing is saved, published or recorded
```

---

## account

**Purpose:** registration, business verification through the KYC provider, the team, and the payout
account.

**Out ports:** `PhoneVerificationRepository`, `CustomerAccountRepository`,
`ProviderAccountRepository`, `MemberVerificationRepository`, `TeamMemberRepository`,
`AccountIEPublisher`, `AdminAuditLog`, `VerificationCodes` (makes the SMS code), `SmsSender`,
`Geocoder` (an address or a pin to a city and coordinates), `IdentityVerificationService` (opens the
KYC provider's KYB, member checks, re-verifications and early re-checks, and asks a check's status),
`PayoutAccountService` (opens the payment provider's onboarding).

**Customers and registration**

| Usecase | In port · method | Domain | Publishes |
|---|---|---|---|
| `RegistrantStartPhoneVerification` | `RegistrantPhoneVerificationStarter.start` | `PhoneVerification.start`; the code is sent only after it is saved | — |
| `RegistrantConfirmPhone` | `RegistrantPhoneConfirmer.confirm` | `PhoneVerification.confirm` | — |
| `CustomerRegisterAccount` | `CustomerAccountRegistrator.register` | `CustomerAccount.register` (the geocoder places the pin) | `CustomerRegisteredIE` |
| `CustomerChangeAddress` | `CustomerAddressChanger.change` | `CustomerAccount.changeAddress` | `CustomerAddressChangedIE` (D2) |
| `CustomerRename` | `CustomerRenamer.rename` | `CustomerAccount.rename` | `CustomerRenamedIE` |
| `CustomerChangeAccessNotes` | `CustomerAccessNotesChanger.change` | `CustomerAccount.changeAccessNotes` | `CustomerAccessNotesChangedIE` |
| `CustomerViewAccount` | `CustomerAccountViewer.view` | (query) | — |

**Business providers**

| Usecase | In port · method | Domain | Publishes |
|---|---|---|---|
| `ProviderRegisterAccount` | `ProviderAccountRegistrator.register` | `ProviderAccount.register` | — |
| `ProviderStartKyb` | `ProviderKybStarter.start` | (opens the KYC provider's KYB pages) | — |
| `ProviderAddPayoutAccount` | `ProviderPayoutAccountAdder.add` | (opens the payment provider's onboarding) | — |
| `ProviderViewAccount` | `ProviderAccountViewer.view` | (query: verification, SSM registration, verified-until, payout account, ready to go live) | — |

**The team**

| Usecase | In port · method | Domain | Publishes |
|---|---|---|---|
| `ProviderStartAddingTeamMember` | `ProviderTeamMemberAdder.start` (role and live photo) | `MemberVerification.start`; saved before the check opens | `MemberVerificationStartedIE` |
| `ProviderConfirmTeamMember` | `ProviderTeamMemberConfirmer.confirm` | `MemberVerification.confirmEmployment`, `TeamMember.add` | `TeamMemberAddedIE` |
| `ProviderChangeTeamMemberRole` | `ProviderTeamMemberRoleChanger.change` | `TeamMember.changeRole` | `TeamMemberRoleChangedIE` |
| `ProviderStartMemberReverification` | `ProviderMemberReverificationStarter.start` | (opens the KYC provider's check, with the person present) | — |
| `ProviderRemoveTeamMember` | `ProviderTeamMemberRemover.remove` | `TeamMember.remove` | `TeamMemberRemovedIE` |
| `ProviderViewTeam` | `ProviderTeamViewer.view` | (query: members with their next due date; people waiting for verification or for the confirmation) | — |
| `CustomerViewProviderTeam` | `CustomerProviderTeamViewer.view` | (query: for the provider page, each team member's registered name, role, photo and last verification) | — |

**The KYC provider and the payment provider reporting back** (webhooks; signature and repeat checks
in infra)

| Usecase | In port · method | Domain | Publishes, one per outcome (A2) |
|---|---|---|---|
| `KycProviderRecordKybResult` | `KycProviderKybResultRecorder.record` | `ProviderAccount.recordKybResult` | `ProviderVerifiedIE`, `ProviderVerificationRejectedIE`, `ProviderReverifiedIE` or `RecheckFailedIE`; nothing for `UNCHANGED` |
| `KycProviderRecordMemberVerificationResult` | `KycProviderMemberVerificationResultRecorder.record` (with the date of birth and the photo match) | `MemberVerification.recordKycResult` | `MemberVerifiedIE` or `MemberVerificationRejectedIE` with the reason |
| `KycProviderRecordTeamMemberResult` | `KycProviderTeamMemberResultRecorder.record` | `TeamMember.recordKycResult` | `TeamMemberReverifiedIE` or `RecheckFailedIE` |
| `KycProviderRecordBusinessLapse` | `KycProviderBusinessLapseRecorder.record` | `ProviderAccount.lapse` | `ProviderVerificationLapsedIE` |
| `KycProviderRecordMemberLapse` | `KycProviderMemberLapseRecorder.record` | `TeamMember.lapse` | `TeamMemberLapsedIE` |
| `KycProviderRecordBusinessReverificationRequired` | `KycProviderBusinessReverificationRequirer.record` | `ProviderAccount.requireReverification` | `ReverificationRequiredIE` |
| `KycProviderRecordMemberReverificationRequired` | `KycProviderMemberReverificationRequirer.record` | `TeamMember.requireReverification` | `ReverificationRequiredIE` |
| `KycProviderRecordBusinessFlag` | `KycProviderBusinessFlagRecorder.record` | `ProviderAccount.recordSeriousFlag` | `ProviderFlaggedIE` |
| `KycProviderRecordMemberFlag` | `KycProviderMemberFlagRecorder.record` | `TeamMember.lapse` | `TeamMemberLapsedIE` |
| `PaymentProviderRecordPayoutAccountReady` | `PaymentProviderPayoutAccountReadyRecorder.record` | `ProviderAccount.recordPayoutAccountReady` (once) | `PayoutAccountReadyIE` |

**Admins and Homa**

| Usecase | In port · method | Domain | Publishes |
|---|---|---|---|
| `AdminRemoveTeamMember` | `AdminTeamMemberRemover.remove` (with a reason) | `TeamMember.remove` | `TeamMemberRemovedIE` |
| `AdminRequestBusinessRecheck` | `AdminBusinessRecheckRequester.request` (with a reason) | (asks the KYC provider; nothing changes until it reports) | — |
| `AdminRequestMemberRecheck` | `AdminMemberRecheckRequester.request` (with a reason) | (same) | — |
| `HomaListDueWork` | `HomaDueWorkLister.list` | (query: checks waiting too long for a result) | — |
| `HomaChaseCheck` | `HomaCheckChaser.chase` | (asks the KYC provider for the check's status; its answer arrives as a usual result) | — |

**Rules**
- **AC1. One event per verification outcome** (A2): the KYC usecases publish the event for the
  `VerificationOutcome` or `MemberRejection` the domain returns, and nothing for `UNCHANGED`.
- **AC2. A business acts only on its own team** (`NotYourTeamMemberException`), and a removed member is
  treated as gone.
- **AC3. Homa never marks anyone verified without the KYC provider's result.** A result with no
  registered name, SSM registration, verified-until date or (for a person being added) date of birth is
  refused by the domain.
- **AC4. The code is sent after the verification is saved**, so a code is never sent for a
  verification that doesn't exist.

```gherkin
Feature: Adding a team member

  Background:
    Given Kemas, a verified business, started adding Aina as a helper with a live photo

  Scenario: A pass waits for the provider and is told
    When the KYC provider reports Aina verified, born 1 May 1998, with her photo matching
    Then the verification is saved as verified
    And MemberVerifiedIE is published with her registered name
    And nobody is added to the team yet

  Scenario: The provider's confirmation adds the person
    Given the KYC provider verified Aina
    When Nur Aina confirms Aina is lawfully employed by Kemas
    Then Aina is saved as a team member, with the confirmation
    And TeamMemberAddedIE is published

  Scenario: A rejection says why
    When the KYC provider reports Aina verified, born 2 March 2009, on 1 March 2027
    Then the verification is saved as rejected
    And MemberVerificationRejectedIE is published with the reason UNDER_18

  Scenario: A repeated result tells nothing
    Given the KYC provider verified Aina
    When it reports the same result again
    Then nothing is saved or published

  Scenario: A business confirms only its own people
    Given the KYC provider verified Aina for Kemas
    When another business confirms her
    Then it is refused with MemberVerificationNotFoundException
```

---

## offering

**Purpose:** a provider builds its listing and goes live; the listing follows the catalogue, Homa's
values, the business's verification and the team.

**Out ports:** `ProviderListingRepository`, `OfferingIEPublisher`, and the projections `CatalogueView`,
`ServiceAreaView`, `HomaLimitsView`, `ProviderReadinessView` and `TeamRosterView`.

| Usecase | In port · method | Domain | Publishes |
|---|---|---|---|
| `ProviderDraftListing` | `ProviderListingDrafter.draft` | `ProviderListing.draft` | — |
| `ProviderSetProfile` | `ProviderProfileSetter.set` | `setProfile` | `ListingChangedIE` when live |
| `ProviderOfferJobs` | `ProviderJobOfferer.offer` | `offerJobs` (active catalogue jobs) | `ListingChangedIE` when live |
| `ProviderBringTools` | `ProviderToolBringer.bring` | `bringTools` | `ListingChangedIE` when live |
| `ProviderSetPricing` | `ProviderPricingSetter.set` | `setPricing` | `ListingChangedIE` when live |
| `ProviderSetHours` | `ProviderHoursSetter.set` | `setHours` (Homa's range) | `ListingChangedIE` when live |
| `ProviderSetHelpers` | `ProviderHelpersSetter.set` | `setHelpers` (at most the helpers on the team) | `ListingChangedIE` when live |
| `ProviderSetSupervisorRule`, `ProviderClearSupervisorRule` | `ProviderSupervisorRuleSetter.set`, `ProviderSupervisorRuleClearer.clear` | `setSupervisorRule`, `clearSupervisorRule` | `ListingChangedIE` when live |
| `ProviderCoverCities` | `ProviderCityCoverer.cover` | `coverCities` | `ListingChangedIE` when live |
| `ProviderChooseCancellationTier` | `ProviderCancellationTierChooser.choose` | `chooseCancellationTier` | `ListingChangedIE` when live |
| `ProviderChooseAcceptancePolicy` | `ProviderAcceptancePolicyChooser.choose` | `chooseAcceptancePolicy` | `ListingChangedIE` when live |
| `ProviderDeclareInsurance` | `ProviderInsuranceDeclarer.declare` | `declareInsurance` | `ProviderInsuranceDeclaredIE` |
| `ProviderClearInsurance` | `ProviderInsuranceClearer.clear` | `clearInsurance` | `ProviderInsuranceClearedIE` |
| `ProviderGoLive` | `ProviderGoLiver.goLive` | `goLive` (the business's readiness from `ProviderReadinessView`) | `ListingWentLiveIE` |
| `ProviderViewListing` | `ProviderListingViewer.view` | (query, with `missingForGoLive`) | — |
| `CustomerViewListing` | `CustomerListingViewer.view` | (query: the provider page's listing part, including Instant book and Insured) | — |

**Event handlers**

| Handler | Event | Does | Publishes |
|---|---|---|---|
| `HandleJobAddedIE`, `HandleToolAddedIE` | `JobAddedIE`, `ToolAddedIE` | adds to `CatalogueView` | — |
| `HandleJobRetiredIE` | `JobRetiredIE` | `CatalogueView`; `withdrawJob` on every listing that offers it | `ListingChangedIE`, `JobWithdrawnFromListingIE`, `ListingWentOfflineIE` and `ListingWentOfflineForRetiredJobIE` as the domain returns |
| `HandleToolRetiredIE` | `ToolRetiredIE` | `CatalogueView`; `withdrawTool` on every listing | `ListingChangedIE`, `ToolWithdrawnFromListingIE` |
| `HandleServiceAreaPublishedIE` | `ServiceAreaPublishedIE` | `ServiceAreaView` | — |
| `HandleHomaPolicyChangedIE` | `HomaPolicyChangedIE` | `HomaLimitsView`; `fitHours` on every listing | `ListingHoursNarrowedIE` with `ListingChangedIE`, or `ListingWentOfflineForHoursIE` with `ListingWentOfflineIE`, on a live listing |
| `HandleProviderVerifiedIE`, `HandleProviderVerificationRejectedIE`, `HandlePayoutAccountReadyIE` | those events | `ProviderReadinessView` | — |
| `HandleProviderVerificationLapsedIE`, `HandleProviderFlaggedIE` | those events | `ProviderReadinessView`; `pause` a live listing | `ListingPausedIE` |
| `HandleProviderReverifiedIE` | `ProviderReverifiedIE` | `ProviderReadinessView`; `resume` a paused listing | `ListingResumedIE` |
| `HandleTeamMemberAddedIE`, `HandleTeamMemberRoleChangedIE`, `HandleTeamMemberReverifiedIE`, `HandleTeamMemberLapsedIE`, `HandleTeamMemberRemovedIE` | those events | `TeamRosterView`; `updateRoster` | `ListingChangedIE`, or `ListingWentOfflineIE` |

**Rules**
- **O1. A live listing's change is told with the whole listing**, so discovery and reservation never
  read offering.
- **O2. Going offline or pausing never touches accepted reservations.** No handler anywhere cancels a
  reservation for `ListingWentOfflineIE` or `ListingPausedIE`.
- **O3. The roster holds team members only, by role**, rebuilt from the team member events.

```gherkin
Feature: The listing follows Homa's values and the catalogue

  Scenario: Homa narrows its duration range and a listing's hours shrink
    Given Kemas is live with hours 2 to 6
    When HomaPolicyChangedIE arrives with a duration range of 1 to 4
    Then Kemas's listing is saved with hours 2 to 4
    And ListingHoursNarrowedIE and ListingChangedIE are published

  Scenario: A retired job takes a listing offline when it was the last
    Given Kemas is live offering only KITCHEN
    When JobRetiredIE arrives for KITCHEN
    Then Kemas's listing is saved offline
    And ListingWentOfflineIE and ListingWentOfflineForRetiredJobIE are published

  Scenario: A flagged business's listing is paused, and its reservations are left alone
    Given Kemas is live
    When ProviderFlaggedIE arrives for Kemas
    Then Kemas's listing is saved paused
    And ListingPausedIE is published
```

---

## availability

**Purpose:** a provider's working time: windows, synced busy blocks, holds for accepted jobs and the
buffer between jobs. It tells the other contexts the provider's free time (D1).

**Out ports:** `ProviderCalendarRepository`, `AvailabilityIEPublisher`, `CalendarSyncClient` (reads a
provider's Google or Apple calendar), `CalendarConnections` (the provider's connection, kept by infra).

| Usecase | In port · method | Domain | Publishes |
|---|---|---|---|
| `ProviderSetWindows` | `ProviderWindowsSetter.set` | `ProviderCalendar.setWindows` (a new calendar on the first call) | `ProviderFreeTimeChangedIE` |
| `ProviderSetBuffer` | `ProviderBufferSetter.set` | `setBufferBetweenJobs` | `ProviderFreeTimeChangedIE` |
| `ProviderConnectCalendar` | `ProviderCalendarConnector.connect` | (stores the connection through `CalendarConnections`) | — |
| `ProviderViewCalendar` | `ProviderCalendarViewer.view` | (query) | — |
| `HomaListDueWork` | `HomaDueWorkLister.list` | (query: calendars due a sync) | — |
| `HomaSyncCalendar` | `HomaCalendarSyncer.sync` | `replaceBusyBlocks` | `ProviderFreeTimeChangedIE` when the free time changed |

**Event handlers**

| Handler | Event | Does | Publishes |
|---|---|---|---|
| `HandleReservationAcceptedIE` | `ReservationAcceptedIE` | `hold` the slot (A3) | `ProviderFreeTimeChangedIE` |
| `HandleReservationCancelledIE` | `ReservationCancelledIE` | `release` the slot | `ProviderFreeTimeChangedIE` |

**Rules**
- **AV1. The slot hold is part of the acceptance.** `HandleReservationAcceptedIE` runs inside the
  acceptance's transaction (I3), so a slot that can't be held refuses the acceptance itself, and two
  acceptances never take the same time.
- **AV2. Free time is told after every change** that changes it, for the booking horizon plus the
  longest booking.

---

## discovery

**Purpose:** a customer, or a guest, searches; Homa sorts providers into exact, close and no matches.

**Out ports:** `DiscoveryIEPublisher`, `CityLocator` (a pin to a city), and the projections
`ListingSearchView`, `FreeTimeView`, `VerifiedUntilView`, `SearchLimitsView` and `CatalogueNamesView`.

| Usecase | In port · method | Domain | Publishes |
|---|---|---|---|
| `CustomerSearchProviders` | `CustomerProviderSearcher.search` (a signed-in customer's city, or a guest's city or pin) | `SearchCriteria.of`, `FreeTime.nearestStart` (D1), `MatchClassifier.classify` | `SearchPerformedIE` |

**Event handlers:** the listing events (`ListingWentLiveIE`, `ListingChangedIE`,
`ListingWentOfflineIE`, `ListingPausedIE`, `ListingResumedIE`, `ListingHoursNarrowedIE`,
`ListingWentOfflineForHoursIE`, `ListingWentOfflineForRetiredJobIE`) keep `ListingSearchView`;
`ProviderFreeTimeChangedIE` keeps `FreeTimeView`; `ProviderVerifiedIE`, `ProviderReverifiedIE` and
`ReverificationRequiredIE` keep `VerifiedUntilView`; `HomaPolicyChangedIE` keeps `SearchLimitsView`;
`JobAddedIE`, `JobRenamedIE`, `JobRegroupedIE`, `ToolAddedIE` and `ToolRenamedIE` keep
`CatalogueNamesView`. Each handler only updates its projection; none publishes.

**Rules**
- **DI1. Every search is recorded, without the customer.** `SearchPerformedIE` carries the jobs, start,
  hours, helpers, city and the numbers of exact and close matches, never the customer's id, name or
  address, and never a guest's pin.
- **DI2. A paused or offline listing is no match.**

```gherkin
Feature: Searching

  Scenario: A guest searches by city and the search is recorded without them
    Given Kemas is live in Cyberjaya and free on Saturday from 10:00 to 13:00
    When a guest searches Cyberjaya for KITCHEN and LAUNDRY_FOLD on Saturday 10:00 for 3 hours and 2 helpers
    Then Kemas is an exact match
    And SearchPerformedIE is published with 1 exact match and no customer

  Scenario: A close match carries its bookable suggested start
    Given Kemas is free on Saturday only from 11:00
    When Sarah searches for Saturday 10:00
    Then Kemas is a close match, NEARBY_TIME, at Saturday 11:00
```

---

## reservation

**Purpose:** enquiries and the reservation lifecycle, from the request to its ending, with every time
and money rule.

**Out ports:** `EnquiryRepository`, `ReservationRepository`, `ReservationIEPublisher`, `AdminAuditLog`,
`DistanceMeter` (the geoservice: the device's distance from the customer's location), and the
projections `OfferingView`, `PolicyView`, `ProviderStandingView`, `TeamRosterView`, `CustomerView` and
`FreeTimeView`. Photos and videos are stored by infra; a usecase receives their references only.

This is the biggest context. Build it in the slices listed under **Build order**.

**The customer**

| Usecase | In port · method | Domain | Publishes |
|---|---|---|---|
| `CustomerCreateEnquiry` | `CustomerEnquiryCreator.create` | `Enquiry.create` | `EnquiryCreatedIE` |
| `CustomerBookAgain` | `CustomerBookingRepeater.bookAgain` (a past enquiry or reservation) | `Enquiry.bookAgain` with the current address | `EnquiryCreatedIE` |
| `CustomerRequestReservation` | `CustomerReservationRequester.request` | `FreeTime.isFree` (D1), `Reservation.request`, `changeAccessNotes`; `Enquiry.markRequested` from an enquiry | `ReservationRequestedIE`; `EnquiryRequestedIE` from an enquiry |
| `CustomerEditRequest` | `CustomerRequestEditor.edit` | `Reservation.edit` | `ReservationEditedIE` |
| `CustomerChangeAccessNotes` | `CustomerReservationAccessNotesChanger.change` | `changeAccessNotes` | `AccessNotesChangedIE` |
| `CustomerCancelReservation` | `CustomerReservationCanceller.cancel` | `cancelByCustomer` | a request: `RequestCancelledIE`; reserved: `ReservationCancelledIE`, `SettlementDecidedIE` |
| `CustomerChallengeNoShow` | `CustomerNoShowChallenger.challenge` | `challengeNoShow` | `NoShowChallengedIE` |
| `CustomerFileReport` | `CustomerReportFiler.file` | `fileReport` (the customer) | `ReportFiledIE` |
| `CustomerViewCancellationTerms` | `CustomerCancellationTermsViewer.view` | `cancellationTerms` | — |
| `CustomerViewReservation`, `CustomerViewReservations` | `…Viewer.view` | (query: status, team, check-in and face check results, completion) | — |
| `CustomerViewProviderRecord` | `CustomerProviderRecordViewer.view` | `ProviderRecord` (D5): jobs done and cancellations | — |

**The provider**

| Usecase | In port · method | Domain | Publishes |
|---|---|---|---|
| `ProviderViewRequests` | `ProviderRequestsViewer.view` | (query: only requests whose payment is secured) | — |
| `ProviderAcceptRequest` | `ProviderRequestAccepter.accept` | `FreeTime.isFree`, `Reservation.accept` (D7) | `ReservationAcceptedIE` |
| `ProviderDeclineRequest` | `ProviderRequestDecliner.decline` | `decline` (refused for Instant book) | `ReservationDeclinedIE` |
| `ProviderCancelReservation` | `ProviderReservationCanceller.cancel` | `cancelByProvider` | `ReservationCancelledIE`, `SettlementDecidedIE`, `FineChargedIE` when fined |
| `ProviderNameTeam` | `ProviderTeamNamer.name` | `nameTeam` with the roster from `TeamRosterView` | `TeamNamedIE` |
| `ProviderCheckIn` | `ProviderCheckInRecorder.checkIn` (the device's location) | `DistanceMeter`, `checkIn` | `ProviderCheckedInIE` |
| `ProviderReportCustomerNoShow` | `ProviderCustomerNoShowReporter.report` | `reportCustomerNoShow` | `ReservationCancelledIE` (the settlement waits, R6) |
| `ProviderSubmitCompletion` | `ProviderCompletionSubmitter.submit` | `submitCompletion` | `CompletionSubmittedIE` |
| `ProviderFileReport` | `ProviderReportFiler.file` | `fileReport` (the provider) | `ReportFiledIE` |
| `ProviderViewReservation` | `ProviderReservationViewer.view` | (query: access notes only once accepted) | — |

**The KYC provider, admins and Homa**

| Usecase | In port · method | Domain | Publishes |
|---|---|---|---|
| `KycProviderRecordFaceCheck` | `KycProviderFaceCheckRecorder.record` | `recordFaceCheck` | `FaceCheckRecordedIE`; `CheckInCompletedIE` on the first pass |
| `HomaRecordFaceCheckOutage` | `HomaFaceCheckOutageRecorder.record` (infra found the KYC provider unreachable) | `recordFaceCheckUnavailable` | `CheckInCompletedIE` |
| `AdminCancelFlaggedReservation` | `AdminFlaggedReservationCanceller.cancel` (with a reason) | `cancelForProviderFlag` | `ReservationCancelledIE`, `SettlementDecidedIE` |
| `AdminRestoreFlaggedReservation` | `AdminFlaggedReservationRestorer.restore` (with a reason; only once the flag is cleared) | `restoreAfterFlagCleared` | `ReservationRestoredIE` |
| `AdminViewReservation` | `AdminReservationViewer.view` | (query: the full history) | — |
| `HomaListDueWork` | `HomaDueWorkLister.list` | (query: by kind, the ids due now) | — |
| `HomaExpireRequest` | `HomaRequestExpirer.expire` | `expire` | `ReservationExpiredIE` |
| `HomaRemindTeam` | `HomaTeamReminder.remind` | `isTeamReminderDue`, `recordTeamReminded` (D4) | `TeamReminderDueIE` |
| `HomaMarkProviderNoShow` | `HomaProviderNoShowMarker.mark` | `markProviderNoShow` | `ReservationCancelledIE`, `SettlementDecidedIE`, `FineChargedIE` |
| `HomaCloseNoShowReportWindow` | `HomaNoShowReportWindowCloser.close` | (the unchallenged no-show's settlement, R6) | `SettlementDecidedIE` |
| `HomaRemindCompletion` | `HomaCompletionReminder.remind` | `isCompletionReminderDue`, `recordCompletionReminded` (D4) | `CompletionReminderDueIE` |
| `HomaMissCompletionDeadline` | `HomaCompletionDeadlineMisser.miss` | `missCompletionDeadline` | `CompletionDeadlineMissedIE` |
| `HomaCloseDisputeWindow` | `HomaDisputeWindowCloser.close` | `closeDisputeWindow` | `ReservationCompletedIE`, `SettlementDecidedIE` |
| `HomaCloseEnquiry` | `HomaEnquiryCloser.close` | `Enquiry.isClosed` | `EnquiryClosedIE` |
| `HomaCancelUndecidedFlaggedReservation` | `HomaUndecidedFlaggedReservationCanceller.cancel` | `isFlagCancellationDue`, `cancelForProviderFlag` | `ReservationCancelledIE`, `SettlementDecidedIE` |

**Event handlers**

| Handler | Event | Does | Publishes |
|---|---|---|---|
| `HandlePaymentSecuredIE` | `PaymentSecuredIE` | `confirmPayment`; for Instant book, `acceptInstantly` (R7) | `ReservationAcceptedIE` for Instant book |
| `HandlePaymentFailedIE` | `PaymentFailedIE` | `paymentFailed` | — |
| `HandleCaseDecidedIE` | `CaseDecidedIE` | `resolve` | `ReservationCompletedIE` or `ReservationCancelledIE`, and `SettlementDecidedIE` |
| `HandleProviderFlaggedIE` | `ProviderFlaggedIE` | `ProviderStandingView`; `disputeForProviderFlag` on every accepted reservation with no completion | `ReservationFlaggedIE` for each |
| `HandleProviderVerifiedIE`, `HandleProviderReverifiedIE`, `HandleReverificationRequiredIE` | those events | `ProviderStandingView` (a pass clears the flag) | — |
| `HandleTeamMemberLapsedIE`, `HandleTeamMemberRemovedIE` | those events | `TeamRosterView`; `dropFromTeam` on every upcoming or ongoing reservation naming them (R9) | `TeamMemberDroppedIE` or `TeamMemberStoppedIE` for each |
| `HandleTeamMemberAddedIE`, `HandleTeamMemberRoleChangedIE`, `HandleTeamMemberReverifiedIE` | those events | `TeamRosterView` | — |
| `HandleProviderFreeTimeChangedIE` | `ProviderFreeTimeChangedIE` | `FreeTimeView`; `declineForTakenTime` on the provider's open requests that are no longer free (R8) | `RequestDeclinedForTakenTimeIE` for each |
| `HandleCaseOpenedIE` | `CaseOpenedIE` (D6) | records the reservation as having an open case, so admins can see its evidence | — |
| the listing events, `HandleHomaPolicyChangedIE`, `HandleCustomerRegisteredIE`, `HandleCustomerAddressChangedIE`, `HandleCustomerAccessNotesChangedIE` | those events | `OfferingView`, `PolicyView`, `CustomerView` | — |

**Rules**
- **R1. A request is paid before the provider answers.** The provider sees a request, and can accept it,
  only once `PaymentSecuredIE` has recorded a secured amount that covers the total
  (`ProviderViewRequests` lists only those). An edit that raises the total waits for payment again. The
  request's expiry still runs from when it was made.
- **R2. A request ends with one event, and never waits unpaid.** A customer cancelling a request
  publishes `RequestCancelledIE` only. `PaymentFailedIE` ends the request with `paymentFailed` and
  publishes nothing more. A late `PaymentSecuredIE` or `PaymentFailedIE` for a reservation that is no
  longer a request is refused by the domain; the handler records it and changes nothing.
- **R3. The provider's standing is as it is now.** `request`, `edit`, `accept` and `acceptInstantly` take
  the verified-until date from `ProviderStandingView` and the listing's current state from
  `OfferingView` (D7). A paused listing's request stays Requested and expires as usual unless the
  business re-verifies in time.
- **R4. A request is checked against free time, and the acceptance holds it.** `CustomerRequestReservation`
  and `ProviderAcceptRequest` refuse a slot `FreeTimeView` says is taken
  (`SlotNotFreeException`, a usecase exception). The authoritative check is the hold in the
  acceptance's transaction (AV1).
- **R5. A seriously flagged provider's reservations:** `HandleProviderFlaggedIE` disputes every accepted
  reservation with no completion; the admins cancel each with a full refund or restore it once the
  flag is cleared, before the start; `HomaCancelUndecidedFlaggedReservation` cancels any undecided at
  the booked start. There is no "let it go ahead" (A3).
- **R6. A customer no-show's settlement waits for the window.** `ProviderReportCustomerNoShow` publishes
  `ReservationCancelledIE`; `HomaCloseNoShowReportWindow` sends the no-show settlement to payment when
  the report window ends unchallenged. If it is challenged, Homa's decision is the only settlement sent.
  A customer's cancellation after check-in is settled straight away.
- **R7. Instant book accepts on payment.** `HandlePaymentSecuredIE` calls `acceptInstantly` for a request
  whose snapshot says Instant book, with the same standing and free-time checks as R3 and R4. A refused
  instant acceptance leaves the request Requested; if its time is no longer free, R8 declines it.
- **R8. Taken time declines open requests.** When a provider's free time changes, each of their open
  requests that is no longer free is declined at once with `declineForTakenTime` (see U2).
- **R9. A person who comes off the team comes off their jobs.** On `TeamMemberLapsedIE` and
  `TeamMemberRemovedIE`, every reservation naming them, reserved or disputed over the job, gets
  `dropFromTeam`; the customer is told.
- **R10. Reminders are sent once** (D4).
- **R11. A party acts only on their own reservation** (`NotYourReservationException`); the provider sees
  access notes only from acceptance.

```gherkin
Feature: Requesting and accepting

  Background:
    Given Kemas is live in Cyberjaya, RM30 per helper per hour, travel RM20, Standard, Request and accept
    And Sarah lives at "12 Jalan Teknokrat 3", Cyberjaya, with access notes "Unit 12-3"

  Scenario: A request is saved with the customer's address and notes, and told to payment
    When Sarah requests KITCHEN and LAUNDRY_FOLD on Saturday 10:00 for 3 hours and 2 helpers
    Then the reservation is saved as Requested at Sarah's address, with her access notes
    And ReservationRequestedIE is published with the total RM230.00

  Scenario: The provider can't accept until the request is paid
    Given Sarah's request is not paid yet
    When Nur Aina accepts it
    Then it is refused with PaymentNotSecuredException
    And nothing is saved or published

  Scenario: Accepting holds the slot and declines a clashing request
    Given Sarah's request is paid, and Kemas has another paid request for Saturday 11:00
    When Nur Aina accepts Sarah's request
    Then Sarah's reservation is saved as Reserved
    And ReservationAcceptedIE is published
    And the other request is declined for taken time, with RequestDeclinedForTakenTimeIE

  Scenario: Instant book accepts the moment the payment is secured
    Given Kemas uses Instant book
    When PaymentSecuredIE arrives for Sarah's request with RM230.00
    Then the reservation is saved as Reserved
    And ReservationAcceptedIE is published

Feature: Job day

  Scenario: A named person who lapses comes off the team before the start
    Given Sarah's reservation names Aina and Ben for Saturday 10:00
    When TeamMemberLapsedIE arrives for Ben on Friday 18:00
    Then the reservation is saved with Aina only and the team incomplete
    And TeamMemberDroppedIE is published, saying the team is not complete

  Scenario: The team reminder is sent once
    Given Sarah's reservation has no team named at Saturday 04:00
    When Homa reminds the team twice
    Then TeamReminderDueIE is published once

  Scenario: An unchallenged no-show is settled when the window ends
    Given Nur Aina reported Sarah a no-show at 10:15
    When Homa closes the no-show report window at 11:15
    Then SettlementDecidedIE is published with Kemas earning RM74.00 and Sarah refunded RM144.90
```

---

## payment

**Purpose:** money: authorising, charging and holding the customer's payment, settling it, paying out
with fine deductions, refunds, fines, holds and chargebacks.

**Out ports:** `PaymentRepository`, `PayoutRepository`, `FineRepository`, `PaymentIEPublisher`,
`AdminAuditLog`, `PaymentGateway` (the payment provider: start a card authorisation or an FPX payment,
replace an authorisation, capture, release, refund, transfer a payout), and the projections
`ReservationMoneyView`, `PayoutStandingView` and `FineCapView`.

| Usecase | In port · method | Domain | Publishes |
|---|---|---|---|
| `CustomerPayForRequest` | `CustomerRequestPayer.pay` (card or FPX; what is owed now) | (starts the payment with `PaymentGateway`; the result arrives by webhook) | — |
| `PaymentProviderRecordPaymentSecured` | `PaymentProviderPaymentSecuredRecorder.record` | `Payment.authoriseCard`, `payUpfrontByFpx` or `adjustTotal` | `PaymentSecuredIE` |
| `PaymentProviderRecordPaymentFailed` | `PaymentProviderPaymentFailedRecorder.record` | (no `Payment` for a first payment; `cancelBeforeAcceptance` after an edit) | `PaymentFailedIE` |
| `PaymentProviderRecordRefund` | `PaymentProviderRefundRecorder.record` | (marks the refund done) | `PaymentRefundedIE` |
| `PaymentProviderRecordPayoutFailed` | `PaymentProviderPayoutFailedRecorder.record` | (marks it for a retry and for the admins) | — |
| `PaymentProviderRecordChargeback` | `PaymentProviderChargebackRecorder.record` | `Payment.recordChargeback` (D3) | `ChargebackOpenedIE` |
| `AdminWaiveFine` | `AdminFineWaiver.waive` (a reason category and text) | `Fine.waive` | `FineWaivedIE` |
| `AdminReleasePayout` | `AdminPayoutReleaser.release` (with a reason) | `Payout.releaseHold` | `PayoutHoldReleasedIE` |
| `AdminWithholdPayout` | `AdminPayoutWithholder.withhold` (with a reason) | `Payout.withhold` | `PayoutWithheldIE` |
| `HomaListDueWork` | `HomaDueWorkLister.list` | (query: payouts and releases due) | — |
| `HomaPayOut` | `HomaPayouter.payOut` | (transfers a payable payout whose release time has come) | — |
| `CustomerViewPayments` | `CustomerPaymentsViewer.view` | (query: payments and refunds with their status and expected timing) | — |
| `ProviderViewIncome` | `ProviderIncomeViewer.view` | (query: earnings, payouts, fines deducted) | — |
| `AdminViewPayments` | `AdminPaymentsViewer.view` | (query; links to the payment provider's console) | — |

**Event handlers**

| Handler | Event | Does | Publishes |
|---|---|---|---|
| `HandleReservationRequestedIE`, `HandleReservationEditedIE` | those events | `ReservationMoneyView` (the total now owed); for FPX, a decrease is refunded with `adjustTotal` | `PaymentRefundedIE` for a decrease |
| `HandleReservationAcceptedIE` | `ReservationAcceptedIE` | `capture` | `PaymentCapturedIE` |
| `HandleReservationDeclinedIE`, `HandleReservationExpiredIE`, `HandleRequestCancelledIE`, `HandleRequestDeclinedForTakenTimeIE` | those events | `cancelBeforeAcceptance` | `PaymentRefundedIE` for FPX |
| `HandleSettlementDecidedIE` | `SettlementDecidedIE` | `settle`; `Payout.create` and `FineDeductionPolicy.apply`; `hold` for a flagged business | `PayoutCreatedIE`; `PayoutHeldIE` |
| `HandleFineChargedIE` | `FineChargedIE` | `Fine.charge` | — |
| `HandleChargebackDecidedIE` | `ChargebackDecidedIE` | `Fine.charge` with `CHARGEBACK` for what the provider covers | — |
| `HandleProviderFlaggedIE` | `ProviderFlaggedIE` | `PayoutStandingView`; `hold` every payable payout of the business | `PayoutHeldIE` for each |
| `HandleProviderReverifiedIE`, `HandlePayoutAccountReadyIE`, `HandleHomaPolicyChangedIE` | those events | `PayoutStandingView`, `FineCapView` | — |

**Rules**
- **PA1. Money moves only through Homa's rules** (A6). No usecase refunds or pays out except from a
  domain instruction.
- **PA2. Instructions wait for their time.** A refund goes out at once; a release to the provider and
  Homa's fee go out from the settlement's release time, through `HomaPayOut`.
- **PA3. A withheld payout stays with Homa** as owed to the business, never revenue.
- **PA4. Processing fees are Homa's.** No usecase passes a payment provider's fee to a customer or a
  provider.

```gherkin
Feature: Settling and paying out

  Scenario: A completed job is paid out, less a fine within the cap
    Given Kemas owes a RM54.00 fine
    When SettlementDecidedIE arrives for Sarah's job with Kemas earning RM200.00
    Then the payment is settled
    And a payout of RM200.00 is saved with RM54.00 deducted
    And PayoutCreatedIE is published with RM146.00 net

  Scenario: A flagged business's payout is held
    Given Kemas is seriously flagged
    When SettlementDecidedIE arrives for a completed job
    Then the payout is saved held
    And PayoutHeldIE is published

  Scenario: A chargeback reaches the admins
    When the payment provider reports a RM230.00 chargeback on Sarah's payment
    Then the payment is saved with the chargeback
    And ChargebackOpenedIE is published
```

---

## resolution

**Purpose:** the admins' case queue and their decisions.

**Out ports:** `CaseRepository`, `ResolutionIEPublisher`, `AdminAuditLog`.

| Usecase | In port · method | Domain | Publishes |
|---|---|---|---|
| `AdminDecideCase` | `AdminCaseDecider.decide` (release, partial or full refund, with a reason) | `Case.decide` | `CaseDecidedIE`; `ChargebackDecidedIE` for a chargeback case |
| `AdminViewCaseQueue` | `AdminCaseQueueViewer.view` | (query: `Case.QUEUE_ORDER`, with the time left of the 72-hour target) | — |
| `AdminViewCase` | `AdminCaseViewer.view` | (query: the case and its evidence; the reservation, chat and payment come from their own contexts' admin views) | — |
| `CustomerViewCase`, `ProviderViewCase` | `…Viewer.view` | (query: status and when the decision is due) | — |

**Event handlers**

| Handler | Event | Does | Publishes |
|---|---|---|---|
| `HandleReportFiledIE` | `ReportFiledIE` | `Case.open` or `addEvidence`; `markSafety` for a safety, damage or loss report | `CaseOpenedIE` (D6) for a new case |
| `HandleNoShowChallengedIE` | `NoShowChallengedIE` | `Case.open` | `CaseOpenedIE` |
| `HandleCompletionDeadlineMissedIE` | `CompletionDeadlineMissedIE` | `Case.open` | `CaseOpenedIE` |
| `HandleChargebackOpenedIE` | `ChargebackOpenedIE` (D3) | `Case.open` with `CHARGEBACK` | `CaseOpenedIE` |
| `HandleReservationFlaggedIE` | `ReservationFlaggedIE` | `closeForProviderFlag` on the reservation's open case | `CaseClosedForProviderFlagIE` |

**Rules**
- **RE1. One decision event per outcome.** A chargeback case publishes `ChargebackDecidedIE` with what
  the provider covers; every other case publishes `CaseDecidedIE`.
- **RE2. The decision target is 72 hours**, every day counted; the queue and both parties see when each
  case is due. It is a service promise: nothing happens automatically when it passes.

```gherkin
Feature: The admins' queue

  Scenario: A safety report opens a case at the top of the queue
    Given an open case from Friday about finished work
    When ReportFiledIE arrives on Saturday with a safety, damage or loss report
    Then a safety case is saved
    And CaseOpenedIE is published
    And the queue lists the safety case first

  Scenario: The other party's report joins the open case
    Given an open case for Sarah's reservation
    When ReportFiledIE arrives from Kemas for the same reservation
    Then the case is saved with Kemas's evidence added
    And no new case is opened
```

---

## conversation

**Purpose:** the chats for enquiries and reservations.

**Out ports:** `ConversationRepository`, and the projections `PeopleView` and `OpenCaseView`.

| Usecase | In port · method | Domain | Publishes |
|---|---|---|---|
| `CustomerStartChat` | `CustomerChatStarter.start` (an enquiry, with the first message) | `Conversation.startByCustomer` | — |
| `CustomerPostMessage` | `CustomerMessagePoster.post` | `post` | — |
| `ProviderPostMessage` | `ProviderMessagePoster.post` | `post` | — |
| `CustomerViewInbox`, `ProviderViewInbox` | `…Viewer.view` | (query: chats grouped by provider, or by customer) | — |
| `CustomerViewChat`, `ProviderViewChat` | `…Viewer.view` | (query) | — |
| `AdminViewChat` | `AdminChatViewer.view` | (query: only while the reservation has an open case, `OpenCaseView`) | — |

**Event handlers**

| Handler | Event | Does |
|---|---|---|
| `HandleEnquiryRequestedIE` | `EnquiryRequestedIE` | `linkReservation` |
| `HandleEnquiryClosedIE` | `EnquiryClosedIE` | `archive` |
| `HandleReservationRequestedIE` | `ReservationRequestedIE` | `openForReservation` when not from an enquiry |
| `HandleCustomerRegisteredIE`, `HandleCustomerRenamedIE`, the listing events | those events | `PeopleView` |
| `HandleCaseOpenedIE`, `HandleCaseDecidedIE`, `HandleCaseClosedForProviderFlagIE` | those events | `OpenCaseView` |

**Rules**
- **CO1. A party writes only in their own chats** (`NotYourChatException`).
- **CO2. Admins open a chat only for a case** (A4).

---

## feedback

**Purpose:** customers rate completed reservations; the averages are shown.

**Out ports:** `RatingRepository`, `RatingSummaryRepository`, `FeedbackIEPublisher`, `AdminAuditLog`,
and the projections `RateableReservationView` and `FeedbackLimitsView`.

| Usecase | In port · method | Domain | Publishes |
|---|---|---|---|
| `CustomerRateReservation` | `CustomerReservationRater.rate` | `Rating.submit`; `RatingSummary.record` for the provider and each rated team member | `RatingSubmittedIE` |
| `AdminHideRating` | `AdminRatingHider.hide` (with a reason) | `Rating.hide`; `RatingSummary.withdraw` | `RatingHiddenIE` |
| `CustomerViewProviderRatings` | `CustomerProviderRatingsViewer.view` | (query: averages shown once there are enough, written feedback newest first) | — |
| `CustomerViewTeamRatings` | `CustomerTeamRatingsViewer.view` | (query: each named person's overall average) | — |
| `AdminViewRatings` | `AdminRatingsViewer.view` | (query: every rating, hidden included) | — |

**Event handlers:** `HandleReservationRequestedIE` (customer and provider), `HandleTeamNamedIE` (who
was named), `HandleReservationCompletedIE` (rateable from then) keep `RateableReservationView`;
`HandleHomaPolicyChangedIE` keeps `FeedbackLimitsView`.

**Rules**
- **FE1. A reservation is rated once**: the rating and the reservation's rated mark are saved together.
- **FE2. A rating never changes money.** No handler in any context listens to the rating events for
  payouts, fines or decisions.

---

## notification

**Purpose:** telling people what happened and what to do, in their language, on the right channel.
This context needs its domain first (D8).

**Out ports:** `NoticeRepository`, `RecipientsView` (each person's language and devices, and who a
reservation's people are), `PushSender`, `SmsSender`.

| Usecase | In port · method | Domain | Publishes |
|---|---|---|---|
| `CustomerChooseLanguage`, `ProviderChooseLanguage` | `…LanguageChooser.choose` | (D8) | — |
| `CustomerRegisterDevice`, `ProviderRegisterDevice` | `…DeviceRegistrar.register` | (D8) | — |
| `CustomerViewNotices`, `ProviderViewNotices` | `…NoticesViewer.view` | (query) | — |

**Event handlers:** one handler per event that tells someone, listed in the event map (for example
`HandlePaymentSecuredIE` tells the provider of a new request; `HandleReservationFlaggedIE` tells the
customer the job is on hold; `HandleTeamReminderDueIE` reminds the provider). Each builds a `Notice`
with the domain and sends it through `PushSender`, and also `SmsSender` where the domain's channel rule
says so. Handlers of `ReservationRequestedIE` and `TeamNamedIE` also keep `RecipientsView`.

**Rules**
- **NO1. Notifying never blocks a change.** Notices are sent after the change that caused them is
  committed (I3); a failed send is retried and never undoes the change.

---

## privacy

**Purpose:** admins record and answer data requests under the PDPA. This context needs its
domain first (D9).

**Out ports:** `DataRequestRepository`, `AdminAuditLog`.

| Usecase | In port · method | Domain | Publishes |
|---|---|---|---|
| `AdminRecordDataRequest` | `AdminDataRequestRecorder.record` | `DataRequest.receive` (D9) | — |
| `AdminCloseDataRequest` | `AdminDataRequestCloser.close` (with what was done) | `DataRequest.close` | — |
| `AdminViewDataRequests` | `AdminDataRequestsViewer.view` | (query: open ones by due date) | — |

---

## tracking

**Purpose:** the admins' views of everything, and the goals and measures from the spec's Part 1.
This context needs its domain first (D10). It listens to every event and is read-only: nothing
depends on it, and if it falls behind no rule is affected.

**Out ports:** one view store per view.

| Usecase | In port · method | Shows |
|---|---|---|
| `AdminViewGoals` | `AdminGoalsViewer.view` | the measures in Goals and Measures, with trends, by city and month |
| `AdminViewGuardrails` | `AdminGuardrailsViewer.view` | acceptance rate and response time, expiry, cancellation and no-show rates, disputes and refunds, face check failures, lapses and flags, the fine waiver rate |
| `AdminViewProvider` | `AdminProviderViewer.view` | a provider's listing, standing, team, fines and waivers, cancellation count, reservations, held payouts, and links to the KYC provider's and the payment provider's consoles |
| `AdminViewCustomer` | `AdminCustomerViewer.view` | a customer's reservations, payments and reports |
| `AdminViewFlaggedProviders` | `AdminFlaggedProvidersViewer.view` | each flagged business, its disputed reservations and held payouts |
| `AdminViewSearchGaps` | `AdminSearchGapsViewer.view` | searches with no or only close matches, by city, job, day and time |

**Event handlers:** one per event, each updating the views it feeds.

---

## Rules for the admin console

Every admin usecase follows these. Each has a usecase test.

1. **A1. Every decision records who and why.** An admin usecase takes the admin's id
   (`decidedBy`, from authentication) and, for a decision about a person, money or providers'
   listings, a reason. It writes who, what, why and when to its context's `AdminAuditLog`. Plain
   curation records who, what and when. A missing admin or reason is refused before anything else
   (`DecidedByRequiredException`, `AdminReasonRequiredException`).
2. **A2. One event per outcome.** An admin's or a provider's reported result publishes the event of the
   outcome the domain returned, never a generic event with a status.
3. **A3. Red-code rules can't be overridden.** A flagged reservation is cancelled with a full refund or
   restored only once the flag is cleared; there is no "let it go ahead".
4. **A4. Personal data only where needed.** Chats and evidence open only for a case. Identity documents
   and selfies stay with the KYC provider; card details stay with the payment provider.
5. **A5. Fine waivers stay exceptional.** A waiver has a reason category (customer-caused, emergency,
   Homa fault, other) and text, is given once and only for what is left, and covers only the money.
6. **A6. Money moves only through Homa's rules.** Admins never refund or pay out directly in the
   payment provider's console for a reservation.

## Rules for the infra phase

The usecase ports promise these; infra keeps them. Each has an infra test.

1. **I1. A save never overwrites a change made meanwhile.** Every repository refuses a save on an
   out-of-date copy with the context's `ChangedMeanwhileException` (optimistic locking). The Homa-set
   values save only as the next version.
2. **I2. Only authentication says who is acting.** `infra/security/` sets the customer's, provider's or
   admin's id from the authenticated user; a controller never takes it from the request body. Only
   admins reach `Admin…` ports; only the KYC provider's and payment provider's signed webhooks reach
   theirs; only the scheduler reaches `Homa…` ports.
3. **I3. A change and its events are all or nothing.** The save, the events and the audit entry of one
   usecase happen in one transaction. Events go out through an outbox written in that transaction
   (Discipline §10), so a change is never saved without its event, and an event is never sent for a
   change that was rolled back. **One exception:** the slot hold (AV1) runs inside the acceptance's
   transaction, because it guards an invariant across two contexts.
4. **I4. Webhooks are checked and deduplicated.** The KYC provider's and payment provider's webhooks are
   verified by signature, and a repeated delivery is skipped by its id before any usecase runs.
5. **I5. Aggregates are rebuilt from storage by infra.** Domain constructors are private and generate
   new ids, so each repository rebuilds an aggregate as it was saved; usecase tests keep aggregates in
   memory.

## Build order

Each step passes its gates before the next starts (Discipline §9). A context's domain prerequisites
come first in its step.

1. **Test support and conventions:** the `AdminAuditLog` and `ProcessedEvents` fakes, a fixed clock, and
   each context's `<Context>UsecaseArchitectureTest`.
2. `catalogue`
3. `policy`
4. `account` (needs D2's event changes)
5. `offering`
6. `availability` (D1)
7. `discovery` (D1)
8. `reservation` (D1, D4, D5, D7), in slices:
   1. Enquiries, requests and edits (with the customer and offering projections)
   2. Payment results, acceptance, decline, expiry, Instant book and taken time
   3. Cancellations and fines
   4. The team, reminders and coming off the team
   5. Check-in, face checks and no-shows
   6. Completion, reports, the dispute window and decisions
   7. Flagged providers
   8. Queries, including the provider record
9. `payment` (D3)
10. `resolution` (D6)
11. `conversation`
12. `feedback`
13. `notification` (D8)
14. `privacy` (D9)
15. `tracking` (D10)
16. `app/`: the composition root wiring, with in-memory adapters, and one end-to-end test of the
    running example: Sarah books Kemas, the team checks in, completes, Kemas is paid and Sarah rates.

## Testing approach

- **What a usecase test proves:** the orchestration, never the domain's rules again: what is loaded,
  what is passed to the domain, what is saved, what is published, what is recorded, and that a refused
  change saves, publishes and records nothing.
- **Framework-free,** against in-memory fakes of every out port, kept in each context's
  `<Context>UsecaseFixtures` (Discipline §9, shared fixtures). Each fake can be told to refuse a save
  (`somethingElseSavesFirst`), so the order of a change is tested.
- **Names in the ubiquitous language,** as in the domain plan: one test class per feature, such as
  `RequestingAReservationTest`, and one method per scenario, such as
  `theProviderCannotAcceptUntilTheRequestIsPaid`. Builders read as domain sentences.
- **Each Gherkin scenario above becomes a test**, and the domain audit gains a usecase section mapping
  them.
- **Architecture test per context** (`<Context>UsecaseArchitectureTest`, ArchUnit):
  - the usecase layer depends only on its own domain, `kernel`, `ie` and Java;
  - in ports and out ports are interfaces; DTOs are records and hold no domain object or event;
  - every usecase implements exactly one in port, named for its actor, and no other usecase implements
    it;
  - every handler is named `Handle<Event>` and implements `<Event>Handler`;
  - usecase exceptions live in `usecase/exception`.
- **Gates per context:** unit tests, CRAP below 6, mutation at least 80% for `usecase` with the ratchet,
  and the dependency rule.

## Open questions

1. **U1. Availability across contexts.** Reservation and discovery need availability's answer. Asking
   availability directly, for example through an adapter in `app/`, is one context calling another
   without an event, which Discipline §4 does not allow. *Recommendation:* D1 (availability publishes free time; the hold
   stays authoritative inside the acceptance's transaction). The cost is a small domain change in three
   contexts and a slightly stale view for search, which the hold covers.
2. **U2. What declines a request for taken time.** The spec declines open requests whose time clashes
   with an accepted booking or its buffer. With D1, any change that takes the time, such as a synced
   calendar event, would decline them too. *Recommendation:* widen the spec to "any open request the
   provider can no longer take", in line with "nobody waits on something that can't happen".
3. **U3. Paying for an edit that raises the total.** *Recommendation:* the customer pays the difference
   with `CustomerPayForRequest`, as for the first payment, since a card may need the customer to
   confirm again.
4. **U4. Deleting personal data across contexts.** *Recommendation:* in the MVP the admins carry out a
   deletion by hand, contexts by context, and record it with `AdminCloseDataRequest`; automated
   deletion events come after launch.
5. **U5. The new contexts.** D8 to D10 add three bounded contexts. *Recommendation:* agree their domains
   (`notification`, `privacy`, `tracking`) before step 13, since the discipline requires all three
   layers.
6. **U6. The events from the domain prerequisites.** D1 to D6 add or change seven events
   (`ProviderFreeTimeChangedIE`, `CustomerAddressChangedIE`, `ChargebackOpenedIE`, `TeamReminderDueIE`,
   `CompletionReminderDueIE`, `CaseOpenedIE`, and `CustomerRegisteredIE` with the address).
   *Recommendation:* add them with their domain prerequisite, in the same step, with contract tests.
