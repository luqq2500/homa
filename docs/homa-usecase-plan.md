# Homa Usecase Development Plan

This plan covers the **usecase phase** (Backend Module Discipline §8, step 2): the `usecase` layer of every bounded context, with its in ports, out ports, DTOs, event handlers, exceptions and tests, and the changes to the integration event contracts in `ie/` that the usecases need. Infra comes last.

**Sources:** `homa-product-description.md` (the product spec), `backend_module_discipline.md` (the discipline), and, for the domain as built, `homa-domain-plan.md` and `homa-domain-stories.md` (the ubiquitous language). When this plan and the spec disagree, the spec wins; when it and the discipline disagree, the discipline wins. Fix this plan.

**Status:** draft for review. Nothing in it is built. The four domain prerequisites (DP1 to DP4) and the open questions (U1 to U5) need the product owner's answer before the steps that depend on them.

## Contents

- Conventions for the usecase phase
- Actors
- Bounded contexts in the usecase phase
- Context map
- How a context gets another context's data
- Prerequisites in the domain
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
- Integration events (`ie/`)
- Rules for the admin console
- Rules for the infra phase
- Build order
- Testing approach
- Open questions

## Conventions for the usecase phase

- **Layout** (Discipline §3). Each context's `usecase` layer has this shape, and nothing else:

  ```text
  <context>/usecase/
  ├── port/in/        one interface per usecase, and one per integration event handler
  ├── port/out/       repositories, the context's IE publisher, projections, outbound clients
  ├── dto/            one request and one response record per usecase, never shared
  ├── exception/      usecase-level exceptions: orchestration failures
  └── <Impl>.java     usecase and handler implementations
  ```

- **One usecase, one service** (Discipline §6). Every usecase has its own interface and its own
  implementation. An implementation implements one in port only, and no usecase calls another.
- **Naming** (Discipline §6): the implementation is named for the action and the interface for the
  actor of the action, with one intent-revealing method:
  `CustomerRequestReservation implements CustomerReservationRequester`, with
  `CustomerReservationRequester.request(RequestReservationRequest) : RequestReservationResponse`.
- **Dedicated DTOs** (Discipline §4, §6). Every in-port method takes its own request record and returns
  its own response record, queries and commands alike. A request carries at least who is acting, so no
  request is empty. DTOs hold primitives, Strings, `kernel` types and JDK value types; never a domain
  object or an event. A command that has nothing to answer still returns a response record, so a
  controller always has a contract for OpenAPI.
- **Event handlers** (Discipline §7): one handler per event in each context that consumes it,
  `Handle<Event> implements <Event>Handler`, with `handle(<Event> event)`. A handler takes the event
  itself, from `ie/events`. The listener in `infra/event/` injects the handler interface, never the
  implementation.
- **One publisher per context** (Discipline §7): `<Context>IEPublisher.publish(<Context>IE event)` in
  `port/out`, taking the context's event interface from `ie/port`.
- **Dependencies** (Discipline §5): a usecase imports only its own context's domain, `kernel` and `ie`.
  It never imports another context, a framework, or a runtime or cloud SDK. Infra is reached only
  through `port/out`.
- **Rules stay in the domain** (Discipline §6). A usecase loads, asks the domain, saves, publishes and
  records. When writing a usecase needs a business rule the domain doesn't have, the rule is added to
  the domain first (Discipline §8: domain first). The four found so far are under **Prerequisites in
  the domain**.
- **Time:** usecases take a `java.time.Clock` and pass `Instant now` to the domain.
- **Order of a change:** validate everything first (who is acting, a required reason, the domain call),
  then save, then publish, then record in the audit log. A refused change saves, publishes and records
  nothing.
- **Nothing changed, nothing done.** When the domain says a change changed nothing (a rename to the same
  name, a repeated result from the KYC provider, a role set to the role it has), the usecase saves,
  publishes and records nothing, and answers with things as they are.
- **Idempotent handlers.** A handler that receives an event it has already applied changes nothing.
  Where the domain doesn't make a repeat harmless, the handler skips an event whose id its context's
  `ProcessedEvents` out port has recorded.
- **Exceptions** (Discipline §6). Domain exceptions pass through unchanged. Usecase exceptions, in
  `usecase/exception`, cover orchestration only: a record not found (`ReservationNotFoundException`),
  an actor acting on something not theirs (`NotYourReservationException`), a missing admin or reason,
  a save on an out-of-date copy (`ChangedMeanwhileException`).
- **Who is acting** comes only from authentication (I2). `infra/security/` puts the customer's,
  provider's or admin's id into the request; a usecase trusts the id it is given and checks only that
  the record belongs to that actor.
- **Scheduled work** (Discipline §10). A scheduler trigger lands in `infra/transport/messaging/` and
  calls ordinary usecase ports. A context with scheduled work has one query, `HomaListDueWork`, which
  lists the ids due for one kind of work, and one usecase per kind that acts on a single id. One id per
  call keeps each change small, retryable and within any request time limit.
- **Outbound services** (the KYC provider, the payment provider, the SMS and push services, the
  geoservice and the calendar services) sit behind `port/out` interfaces named for what Homa needs,
  never for a vendor: `IdentityVerificationService`, `PaymentGateway`, `Geocoder`. Their clients live in
  `infra/transport/client/` (Discipline §3), so a vendor change touches only its client.
- **Money** is `kernel.Money` throughout.

## Actors

The actor names the in port and the usecase. Each is a word from the ubiquitous language.

| Actor | Who acts | Example |
|---|---|---|
| `Customer` | A homeowner: signed in, or a guest for search and browse | `CustomerRequestReservation` |
| `Registrant` | Anyone verifying a phone number to register | `RegistrantConfirmPhone` |
| `Provider` | A business provider, through its owner or director in the provider app | `ProviderAcceptRequest` |
| `Admin` | Homa's admins, in the admin console: the founders, for now | `AdminDecideCase` |
| `Homa` | The platform acting on its own: scheduled steps and reminders | `HomaExpireRequest` |
| `KycProvider` | The KYC provider reporting a result | `KycProviderRecordKybResult` |
| `PaymentProvider` | The payment provider reporting a payment, refund, payout or chargeback | `PaymentProviderRecordPaymentSecured` |

`Admin` and `Homa` are different actors: `Admin…` usecases are people acting in the admin console;
`Homa…` usecases are the platform acting on its own.

## Bounded contexts in the usecase phase

| Context | What its usecases do | Publishes (`ie/port`) |
|---|---|---|
| `catalogue` | Admins curate the job list, tool list, service area and showcase | `CatalogueIE` |
| `policy` | Admins launch and change the Homa-set values | `PolicyIE` |
| `account` | Registration, KYB, the team, the payout account, and the KYC provider's reports | `AccountIE` |
| `offering` | Providers build their listing and go live; the listing follows the catalogue, values, verification and team | `OfferingIE` |
| `availability` | Providers' working time, synced calendars, holds; free time told to others | `AvailabilityIE` (new) |
| `discovery` | Customers and guests search | `DiscoveryIE` |
| `reservation` | Enquiries and the reservation lifecycle | `ReservationIE` |
| `payment` | Payments, settlements, payouts, fines and chargebacks | `PaymentIE` |
| `resolution` | The admins' case queue and decisions | `ResolutionIE` |
| `conversation` | Chats | — |
| `feedback` | Ratings | `FeedbackIE` |
| `notification` | Telling people, in their language (U5) | — |
| `privacy` | Recording data requests (U5) | — |
| `tracking` | The admins' goals and guardrails (U5) | — |

## Context map

```text
catalogue ──(jobs, tools, cities)──► offering, discovery
policy ────(Homa-set values)──────► offering, availability, discovery, reservation, payment, feedback
account ───(customers, verification, team)──► offering, discovery, reservation, payment, conversation
offering ──(listings)─────────────► discovery, reservation, conversation
availability ──(free time)────────► discovery, reservation
reservation ──(lifecycle, settlements, fines)──► availability, payment, resolution, conversation, feedback
payment ───(payment secured or failed; chargebacks)──► reservation, resolution
resolution ─(case opened, decided)──► reservation, payment, conversation
every context ──(events)──► notification, tracking
```

## How a context gets another context's data

Contexts communicate only through integration events (Discipline §4). A context that needs another's
data keeps its own **projection**: a store behind its own out port, kept by its own event handlers. No
context reads another's store, and no context calls another's in port.

| Consumer | Projection (out port) | Kept by |
|---|---|---|
| offering | `CatalogueView`: active jobs and tools with their names and groups | `JobAddedIE`, `JobRenamedIE`, `JobRegroupedIE`, `JobRetiredIE`, `ToolAddedIE`, `ToolRenamedIE`, `ToolRetiredIE` |
| offering | `ServiceAreaView` | `ServiceAreaPublishedIE` |
| offering | `HomaLimitsView`: Homa's duration range | `HomaPolicyChangedIE` |
| offering | `ProviderReadinessView`: verified, lapsed or flagged; payout account ready | `ProviderVerifiedIE`, `ProviderVerificationRejectedIE`, `ProviderReverifiedIE`, `ProviderVerificationLapsedIE`, `ProviderFlaggedIE`, `PayoutAccountReadyIE` |
| offering, reservation | `TeamRosterView`: team members by role and status | `TeamMemberAddedIE`, `TeamMemberRoleChangedIE`, `TeamMemberReverifiedIE`, `TeamMemberLapsedIE`, `TeamMemberRemovedIE` |
| availability | `HorizonView`: the booking horizon | `HomaPolicyChangedIE` |
| discovery | `ListingSearchView`: each live listing's search fields, name, picture, price, Instant book, Insured | the listing events, `ProviderInsuranceDeclaredIE`, `ProviderInsuranceClearedIE` |
| discovery, reservation | `FreeTimeView`: each provider's free time | `ProviderFreeTimeChangedIE` (DP1) |
| discovery | `VerifiedUntilView` | `ProviderVerifiedIE`, `ProviderReverifiedIE`, `ReverificationRequiredIE` |
| discovery | `SearchLimitsView` | `HomaPolicyChangedIE` |
| discovery | `CatalogueNamesView` | `JobAddedIE`, `JobRenamedIE`, `JobRegroupedIE`, `ToolAddedIE`, `ToolRenamedIE` |
| reservation | `OfferingView`: each live listing's offering snapshot, name, and whether it takes requests now | the listing events |
| reservation | `PolicyView`: the policy in force | `HomaPolicyChangedIE` |
| reservation | `ProviderStandingView`: verified-until date, seriously flagged or not | `ProviderVerifiedIE`, `ProviderReverifiedIE`, `ReverificationRequiredIE`, `ProviderFlaggedIE` |
| reservation | `CustomerView`: address, coordinates, access notes | `CustomerRegisteredIE`, `CustomerAddressChangedIE`, `CustomerAccessNotesChangedIE` |
| reservation | `OpenCaseView` | `CaseOpenedIE`, `CaseDecidedIE`, `CaseClosedForProviderFlagIE` |
| payment | `ReservationMoneyView`: customer, provider, method and total | `ReservationRequestedIE`, `ReservationEditedIE` |
| payment | `PayoutStandingView`: payout account ready, flagged | `PayoutAccountReadyIE`, `ProviderFlaggedIE`, `ProviderReverifiedIE` |
| payment | `FineCapView` | `HomaPolicyChangedIE` |
| conversation | `PeopleView`: customer names, provider display names | `CustomerRegisteredIE`, `CustomerRenamedIE`, `ListingWentLiveIE`, `ListingChangedIE` |
| conversation | `OpenCaseView` | `CaseOpenedIE`, `CaseDecidedIE`, `CaseClosedForProviderFlagIE` |
| feedback | `RateableReservationView` | `ReservationRequestedIE`, `TeamNamedIE`, `ReservationCompletedIE` |
| feedback | `FeedbackLimitsView` | `HomaPolicyChangedIE` |
| notification | `RecipientsView`: a reservation's customer and provider; people's names | `ReservationRequestedIE`, `CustomerRegisteredIE`, `CustomerRenamedIE`, the listing events |
| tracking | its own views | every event |

**Screens compose queries; contexts don't.** A screen that shows several contexts' data calls each
context's own query, and the client puts the answers together. The provider page is
`CustomerViewListing` (offering), `CustomerViewProviderTeam` (account), `CustomerViewProviderRecord`
(payment: jobs done and cancellations), `CustomerViewProviderRatings` and `CustomerViewTeamRatings`
(feedback).

## Prerequisites in the domain

Writing the usecases against the discipline found four rules the domain doesn't hold yet. Discipline
§6 keeps rules in the domain and §8 builds the domain first, so each is added to the domain, with its
tests and gates, before the usecases that need it. **Each needs the product owner's confirmation.**

| # | Prerequisite | Why | Change |
|---|---|---|---|
| DP1 | **Free time** | Reservation and discovery need to know when a provider is free, and contexts talk only through events (Discipline §4), so neither can ask availability. Availability must tell its answer instead (U1) | `availability`: `ProviderCalendar.freeTime(from, until)` returns the free stretches: windows, less busy blocks, less holds widened by the buffer. `discovery`: a `FreeTime` value with `nearestStart(wanted, within, earliest, latest)`, replacing the nearest start given to `ProviderCandidate`. `reservation`: a `FreeTime` value with `isFree(slot)`; `request`, `edit`, `accept` and `acceptInstantly` take it and refuse a slot that isn't free (`SlotNotFreeException`) |
| DP2 | **What an earning is for** | Payment deducts fines only from payouts for completed jobs, and must know whether an earning pays for a job, a cancellation fee or a customer no-show fee. A job decided by Homa and a challenged no-show decided by Homa end the same way today, so payment can't tell them apart | `reservation`: `Settlement` gains `earningFor`: `COMPLETED_JOB`, `CANCELLATION_FEE` or `CUSTOMER_NO_SHOW_FEE`. `payment`: a payout's `PayoutKind` comes from it |
| DP3 | **A reminder is sent once** | `isTeamReminderDue` and `isCompletionReminderDue` stay true, so a scheduled reminder would repeat. The spec reminds again only when the team becomes incomplete | `reservation`: `recordTeamReminded(now)` and `recordCompletionReminded(now)`; a team that becomes incomplete after a reminder is due again |
| DP4 | **A paused listing can't accept, in the domain** | Whether a paused listing can accept is a rule, and rules stay in the domain (Discipline §6) | `reservation`: `accept` and `acceptInstantly` take whether the listing takes requests now, and refuse with `ListingPausedException` |

Each prerequisite also changes `ie/`: see **Integration events**. The domain plan, the domain stories and
the domain audit are updated with each one.

---

## catalogue

**Purpose:** admins curate the job list, the tool list, the service area and the homepage showcase;
providers and customers read them.

| Out port | Kind |
|---|---|
| `JobListRepository`, `ToolListRepository`, `ShowcaseRepository`, `ServiceAreaRepository` | Repository |
| `CatalogueIEPublisher` | IE publisher |
| `AdminAuditLog` | Audit log (A1) |

```java
// Admin
AdminJobAdder.add(AddJobRequest) : AddJobResponse                                   // AdminAddJob → JobAddedIE
AdminJobRenamer.rename(RenameJobRequest) : RenameJobResponse                        // AdminRenameJob → JobRenamedIE
AdminJobRegrouper.regroup(RegroupJobRequest) : RegroupJobResponse                   // AdminRegroupJob → JobRegroupedIE
AdminJobRetirer.retire(RetireJobRequest) : RetireJobResponse                        // AdminRetireJob → JobRetiredIE (with a reason)
AdminToolAdder.add(AddToolRequest) : AddToolResponse                                // AdminAddTool → ToolAddedIE
AdminToolRenamer.rename(RenameToolRequest) : RenameToolResponse                     // AdminRenameTool → ToolRenamedIE
AdminToolRetirer.retire(RetireToolRequest) : RetireToolResponse                     // AdminRetireTool → ToolRetiredIE (with a reason)
AdminSearchFeaturer.feature(FeatureSearchRequest) : FeatureSearchResponse           // AdminFeatureSearch
AdminFeaturedSearchRenamer.rename(RenameFeaturedSearchRequest) : RenameFeaturedSearchResponse  // AdminRenameFeaturedSearch
AdminFeaturedSearchMover.move(MoveFeaturedSearchRequest) : MoveFeaturedSearchResponse          // AdminMoveFeaturedSearch
AdminFeaturedSearchRemover.remove(RemoveFeaturedSearchRequest) : RemoveFeaturedSearchResponse  // AdminRemoveFeaturedSearch
AdminServiceAreaPublisher.publish(PublishServiceAreaRequest) : PublishServiceAreaResponse      // AdminPublishServiceArea → ServiceAreaPublishedIE
AdminJobListViewer.view(…) , AdminToolListViewer.view(…), AdminShowcaseViewer.view(…)  // everything, retired and hidden included

// Provider and Customer
ProviderJobListViewer.view(…), ProviderToolListViewer.view(…), ProviderServiceAreaViewer.view(…)  // what providers can choose
CustomerJobListViewer.view(…)                                                       // the offered jobs, by group
CustomerShowcaseViewer.view(…)                                                      // the shown featured searches, in order
```

The domain calls are `JobList.add`, `rename`, `regroup`, `retire`; `ToolList.add`, `rename`, `retire`;
`Showcase.feature`, `rename`, `move`, `remove`, `withdrawJob`; `ServiceArea.launch`.

**Rules**
- **CA1.** Retiring a job or tool needs a reason, because it changes providers' listings. Other curation
  records who, what and when, with no reason (A1).
- **CA2.** `AdminRetireJob` also takes the job off every featured search (`Showcase.withdrawJob`), in the
  same change; a featured search left with no job is no longer shown.
- **CA3.** A featured search names only jobs providers can offer; the showcase owns the order.

```gherkin
Feature: Curating the catalogue

  Scenario: A retired job is told, comes off the showcase and is recorded with its reason
    Given the job list offers KITCHEN_GREASE, which "Weekend quick refresh" features
    When admin Aisyah retires KITCHEN_GREASE because "Replaced by kitchen care"
    Then the job list is saved with KITCHEN_GREASE retired
    And the showcase is saved without it
    And JobRetiredIE is published for KITCHEN_GREASE
    And the audit log records Aisyah, the retirement and the reason

  Scenario: A retirement without a reason changes nothing
    When admin Aisyah retires KITCHEN_GREASE with no reason
    Then it is refused with AdminReasonRequiredException
    And nothing is saved, published or recorded

  Scenario: A rename to the same name changes nothing
    When admin Aisyah renames KITCHEN_GREASE to its own name
    Then nothing is saved, published or recorded
```

---

## policy

**Purpose:** admins launch and change the Homa-set values; the other contexts keep a copy.

| Out port | Kind |
|---|---|
| `PolicyRepository` | Repository; saves only the next version (I1) |
| `PolicyIEPublisher` | IE publisher |
| `AdminAuditLog` | Audit log |

```java
AdminPolicyLauncher.launch(LaunchPolicyRequest) : LaunchPolicyResponse              // AdminLaunchPolicy → HomaPolicyChangedIE (version 1, once)
AdminPolicyValueChanger.change(ChangePolicyValueRequest) : ChangePolicyValueResponse // AdminChangePolicyValue → HomaPolicyChangedIE (with a reason)
AdminPolicyViewer.view(ViewPolicyRequest) : ViewPolicyResponse                      // the 26 values in force
```

**Rules**
- **PO1.** Each value is named (`SERVICE_FEE`, `RESPONSE_DEADLINE`, …) and written by its kind: a
  percentage (`17.5%`), an ISO-8601 period in whole minutes (`PT2H`, `P30D`), a count (`300`) or a range
  of hours (`1-6`). The view shows each value by the same name and in the same form, so the console can
  send it back changed. The usecase only translates; the domain checks each value and every rule across
  values.
- **PO2.** `HomaPolicyChangedIE` carries every value of the new version.

```gherkin
Feature: Changing a Homa-set value

  Scenario: A change makes a new version for new reservations
    Given policy version 1 is in force
    When admin Aisyah changes RESPONSE_DEADLINE to PT30M because "More time to answer"
    Then version 2 is saved with a 30-minute response deadline
    And HomaPolicyChangedIE is published with version 2 and every value

  Scenario: A value that breaks a rule across values changes nothing
    When admin Aisyah changes RESPONSE_DEADLINE to PT2H while the minimum notice is 2 hours
    Then it is refused with ResponseDeadlineNotBeforeMinimumNoticeException
    And nothing is saved, published or recorded
```

---

## account

**Purpose:** registration; the business's verification through the KYC provider; the team; the payout
account; the KYC provider's and payment provider's reports.

| Out port | Kind |
|---|---|
| `PhoneVerificationRepository`, `CustomerAccountRepository`, `ProviderAccountRepository`, `MemberVerificationRepository`, `TeamMemberRepository` | Repository |
| `AccountIEPublisher` | IE publisher |
| `AdminAuditLog` | Audit log |
| `VerificationCodes` | Makes the 6-digit code |
| `SmsSender` | Client: sends the code |
| `Geocoder` | Client: an address or a pin to a city and coordinates |
| `IdentityVerificationService` | Client: opens the KYC provider's KYB, member checks, re-verifications and early re-checks; asks a check's status |
| `PayoutAccountService` | Client: opens the payment provider's payout onboarding |

```java
// Registrant and Customer
RegistrantPhoneVerificationStarter.start(StartPhoneVerificationRequest) : …Response  // RegistrantStartPhoneVerification (the code is sent after the verification is saved)
RegistrantPhoneConfirmer.confirm(ConfirmPhoneRequest) : ConfirmPhoneResponse          // RegistrantConfirmPhone
CustomerAccountRegistrator.register(RegisterCustomerRequest) : RegisterCustomerResponse  // CustomerRegisterAccount → CustomerRegisteredIE
CustomerAddressChanger.change(ChangeAddressRequest) : ChangeAddressResponse           // CustomerChangeAddress → CustomerAddressChangedIE
CustomerRenamer.rename(RenameCustomerRequest) : RenameCustomerResponse                // CustomerRename → CustomerRenamedIE
CustomerAccessNotesChanger.change(ChangeAccessNotesRequest) : …Response               // CustomerChangeAccessNotes → CustomerAccessNotesChangedIE
CustomerAccountViewer.view(ViewCustomerAccountRequest) : …Response                    // CustomerViewAccount
CustomerProviderTeamViewer.view(ViewProviderTeamRequest) : …Response                  // CustomerViewProviderTeam: the provider page's team, with names, roles, photos, last verification

// Provider
ProviderAccountRegistrator.register(RegisterProviderRequest) : …Response              // ProviderRegisterAccount (no event until KYB passes)
ProviderKybStarter.start(StartKybRequest) : StartKybResponse                          // ProviderStartKyb: opens the KYC provider's KYB pages
ProviderPayoutAccountAdder.add(AddPayoutAccountRequest) : …Response                   // ProviderAddPayoutAccount: opens the payment provider's onboarding
ProviderAccountViewer.view(ViewProviderAccountRequest) : …Response                    // ProviderViewAccount
ProviderTeamMemberAdder.start(StartAddingTeamMemberRequest) : …Response               // ProviderStartAddingTeamMember → MemberVerificationStartedIE (role and live photo; saved before the check opens)
ProviderTeamMemberConfirmer.confirm(ConfirmTeamMemberRequest) : …Response             // ProviderConfirmTeamMember → TeamMemberAddedIE
ProviderTeamMemberRoleChanger.change(ChangeTeamMemberRoleRequest) : …Response         // ProviderChangeTeamMemberRole → TeamMemberRoleChangedIE
ProviderMemberReverificationStarter.start(StartMemberReverificationRequest) : …Response  // ProviderStartMemberReverification
ProviderTeamMemberRemover.remove(RemoveTeamMemberRequest) : …Response                 // ProviderRemoveTeamMember → TeamMemberRemovedIE
ProviderTeamViewer.view(ViewTeamRequest) : ViewTeamResponse                           // ProviderViewTeam: members with next due dates; people waiting for verification or for the confirmation

// KycProvider and PaymentProvider (webhooks; one event per outcome, A2)
KycProviderKybResultRecorder.record(…)                    // KycProviderRecordKybResult → ProviderVerifiedIE | ProviderVerificationRejectedIE | ProviderReverifiedIE | RecheckFailedIE | nothing
KycProviderMemberVerificationResultRecorder.record(…)     // KycProviderRecordMemberVerificationResult → MemberVerifiedIE | MemberVerificationRejectedIE (with the reason)
KycProviderTeamMemberResultRecorder.record(…)             // KycProviderRecordTeamMemberResult → TeamMemberReverifiedIE | RecheckFailedIE
KycProviderBusinessLapseRecorder.record(…)                // KycProviderRecordBusinessLapse → ProviderVerificationLapsedIE
KycProviderMemberLapseRecorder.record(…)                  // KycProviderRecordMemberLapse → TeamMemberLapsedIE
KycProviderBusinessReverificationRequirer.record(…)       // KycProviderRecordBusinessReverificationRequired → ReverificationRequiredIE
KycProviderMemberReverificationRequirer.record(…)         // KycProviderRecordMemberReverificationRequired → ReverificationRequiredIE
KycProviderBusinessFlagRecorder.record(…)                 // KycProviderRecordBusinessFlag → ProviderFlaggedIE
KycProviderMemberFlagRecorder.record(…)                   // KycProviderRecordMemberFlag → TeamMemberLapsedIE
PaymentProviderPayoutAccountReadyRecorder.record(…)       // PaymentProviderRecordPayoutAccountReady → PayoutAccountReadyIE (once)

// Admin and Homa
AdminTeamMemberRemover.remove(…)                          // AdminRemoveTeamMember → TeamMemberRemovedIE (with a reason)
AdminBusinessRecheckRequester.request(…)                  // AdminRequestBusinessRecheck (with a reason; nothing changes until the KYC provider reports)
AdminMemberRecheckRequester.request(…)                    // AdminRequestMemberRecheck (with a reason)
AdminProviderViewer.view(…), AdminCustomerViewer.view(…)  // a provider's or customer's account, with links to the KYC provider's console
HomaDueWorkLister.list(ListDueWorkRequest) : …Response    // HomaListDueWork: checks waiting too long for a result
HomaCheckChaser.chase(ChaseCheckRequest) : …Response      // HomaChaseCheck: asks the KYC provider for the check's status
```

**Rules**
- **AC1. One event per verification outcome** (A2). The KYC usecases publish the event of the
  `VerificationOutcome` or `MemberRejection` the domain returns, and nothing for `UNCHANGED`.
- **AC2. A phone number has at most one customer account and one business account.** Registering a
  second one is refused (`PhoneAlreadyRegisteredException`).
- **AC3. A business acts only on its own team** (`NotYourTeamMemberException`); a removed member is
  treated as gone.
- **AC4. Homa marks nobody verified without the KYC provider's result.** The domain refuses a result
  missing its registered name, SSM registration, verified-until date or, for a person being added, the
  date of birth.
- **AC5. The code is sent only after the verification is saved.**

```gherkin
Feature: Adding a team member

  Background:
    Given Kemas, a verified business, started adding Aina as a helper with a live photo

  Scenario: A pass waits for the provider's confirmation and is told
    When the KYC provider reports Aina verified, born 1 May 1998, with her photo matching
    Then the verification is saved as verified
    And MemberVerifiedIE is published with her registered name
    And nobody is added to the team yet

  Scenario: The provider's confirmation adds the person
    Given the KYC provider verified Aina
    When Nur Aina confirms that Aina is lawfully employed by Kemas
    Then Aina is saved as a team member, with the confirmation
    And TeamMemberAddedIE is published

  Scenario: A rejection says why
    When the KYC provider reports Aina verified on 1 March 2027, born 2 March 2009
    Then the verification is saved as rejected
    And MemberVerificationRejectedIE is published with the reason UNDER_18

  Scenario: A repeated result tells nothing
    Given the KYC provider verified Aina
    When it reports the same result again
    Then nothing is saved or published
```

---

## offering

**Purpose:** a provider builds its listing and goes live; the listing follows the catalogue, Homa's
values, the business's verification and the team.

| Out port | Kind |
|---|---|
| `ProviderListingRepository` | Repository |
| `OfferingIEPublisher` | IE publisher |
| `CatalogueView`, `ServiceAreaView`, `HomaLimitsView`, `ProviderReadinessView`, `TeamRosterView` | Projections |

```java
// Provider (each setter publishes ListingChangedIE when the listing is live)
ProviderListingDrafter.draft(…)                           // ProviderDraftListing
ProviderProfileSetter.set(…)                              // ProviderSetProfile
ProviderJobOfferer.offer(…)                               // ProviderOfferJobs (active catalogue jobs)
ProviderToolBringer.bring(…)                              // ProviderBringTools
ProviderPricingSetter.set(…)                              // ProviderSetPricing
ProviderHoursSetter.set(…)                                // ProviderSetHours (Homa's range)
ProviderHelpersSetter.set(…)                              // ProviderSetHelpers (at most the helpers on the team)
ProviderSupervisorRuleSetter.set(…)                       // ProviderSetSupervisorRule
ProviderSupervisorRuleClearer.clear(…)                    // ProviderClearSupervisorRule
ProviderCityCoverer.cover(…)                              // ProviderCoverCities
ProviderCancellationTierChooser.choose(…)                 // ProviderChooseCancellationTier
ProviderAcceptancePolicyChooser.choose(…)                 // ProviderChooseAcceptancePolicy
ProviderInsuranceDeclarer.declare(…)                      // ProviderDeclareInsurance → ProviderInsuranceDeclaredIE
ProviderInsuranceClearer.clear(…)                         // ProviderClearInsurance → ProviderInsuranceClearedIE
ProviderGoLiver.goLive(…)                                 // ProviderGoLive → ListingWentLiveIE (readiness from ProviderReadinessView)
ProviderListingViewer.view(…)                             // ProviderViewListing, with what is still missing to go live

// Customer and Admin
CustomerListingViewer.view(…)                             // CustomerViewListing: the provider page's listing part
AdminListingViewer.view(…)                                // AdminViewListing

// Event handlers
JobAddedIEHandler, JobRenamedIEHandler, JobRegroupedIEHandler, ToolAddedIEHandler, ToolRenamedIEHandler  // CatalogueView
JobRetiredIEHandler          // withdrawJob on every listing → ListingChangedIE, JobWithdrawnFromListingIE, ListingWentOfflineIE, ListingWentOfflineForRetiredJobIE, as returned
ToolRetiredIEHandler         // withdrawTool on every listing → ListingChangedIE, ToolWithdrawnFromListingIE
ServiceAreaPublishedIEHandler                                                       // ServiceAreaView
HomaPolicyChangedIEHandler   // HomaLimitsView; fitHours on every listing → ListingHoursNarrowedIE and ListingChangedIE, or ListingWentOfflineForHoursIE and ListingWentOfflineIE
ProviderVerifiedIEHandler, ProviderVerificationRejectedIEHandler, PayoutAccountReadyIEHandler   // ProviderReadinessView
ProviderVerificationLapsedIEHandler, ProviderFlaggedIEHandler  // ProviderReadinessView; pause a live listing → ListingPausedIE
ProviderReverifiedIEHandler  // ProviderReadinessView; resume a paused listing → ListingResumedIE
TeamMemberAddedIEHandler, TeamMemberRoleChangedIEHandler, TeamMemberReverifiedIEHandler,
TeamMemberLapsedIEHandler, TeamMemberRemovedIEHandler  // TeamRosterView; updateRoster → ListingChangedIE, or ListingWentOfflineIE
```

**Rules**
- **OF1. A live listing's change is told with the whole listing**, so discovery and reservation never
  read offering.
- **OF2. Going offline or pausing never touches an accepted reservation.** No handler in any context
  cancels a reservation for `ListingWentOfflineIE` or `ListingPausedIE`.
- **OF3. The roster holds team members only, by role**, and is rebuilt from the team member events.
- **OF4. A provider acts only on its own listing** (`NotYourListingException`).

```gherkin
Feature: The listing follows Homa's values, the catalogue and verification

  Scenario: Homa narrows its duration range and a listing's hours shrink
    Given Kemas is live with hours 2 to 6
    When HomaPolicyChangedIE arrives with a duration range of 1 to 4
    Then Kemas's listing is saved with hours 2 to 4
    And ListingHoursNarrowedIE and ListingChangedIE are published

  Scenario: A retired last job takes a listing offline
    Given Kemas is live offering only KITCHEN
    When JobRetiredIE arrives for KITCHEN
    Then Kemas's listing is saved offline
    And ListingWentOfflineIE and ListingWentOfflineForRetiredJobIE are published

  Scenario: A flagged business's listing is paused
    Given Kemas is live
    When ProviderFlaggedIE arrives for Kemas
    Then Kemas's listing is saved paused
    And ListingPausedIE is published
```

---

## availability

**Purpose:** a provider's working time: windows, synced busy blocks, holds for accepted jobs and the
buffer between jobs. It tells the other contexts the provider's free time (DP1).

| Out port | Kind |
|---|---|
| `ProviderCalendarRepository` | Repository |
| `AvailabilityIEPublisher` | IE publisher (new, with `AvailabilityIE`) |
| `HorizonView` | Projection |
| `CalendarSyncClient` | Client: reads a provider's Google or Apple calendar |
| `CalendarConnections` | Client: the provider's calendar connection, kept by infra |

```java
ProviderWindowsSetter.set(…)               // ProviderSetWindows → ProviderFreeTimeChangedIE (a calendar is made on the first call)
ProviderBufferSetter.set(…)                // ProviderSetBuffer → ProviderFreeTimeChangedIE
ProviderCalendarConnector.connect(…)       // ProviderConnectCalendar
ProviderCalendarViewer.view(…)             // ProviderViewCalendar
HomaDueWorkLister.list(…)                  // HomaListDueWork: calendars due a sync, or due to tell their free time again
HomaCalendarSyncer.sync(…)                 // HomaSyncCalendar: replaceBusyBlocks → ProviderFreeTimeChangedIE when it changed
HomaFreeTimeTeller.tell(…)                 // HomaTellFreeTime: tells the free time again as the horizon moves on each day

// Event handlers
ReservationAcceptedIEHandler               // hold the slot (AV1) → ProviderFreeTimeChangedIE
ReservationCancelledIEHandler              // release the slot → ProviderFreeTimeChangedIE
HomaPolicyChangedIEHandler                 // HorizonView
```

**Rules**
- **AV1. The slot hold is part of the acceptance.** `HandleReservationAcceptedIE` runs inside the
  acceptance's transaction (I3), so a slot that can't be held refuses the acceptance itself, and two
  acceptances never take the same time.
- **AV2. Free time is told after every change that changes it**, from now to the booking horizon plus
  the longest booking.

---

## discovery

**Purpose:** a customer, signed in or a guest, searches; Homa sorts providers into exact, close and no
matches.

| Out port | Kind |
|---|---|
| `DiscoveryIEPublisher` | IE publisher |
| `ListingSearchView`, `FreeTimeView`, `VerifiedUntilView`, `SearchLimitsView`, `CatalogueNamesView` | Projections |
| `CityLocator` | Client: a guest's pin to a city |

```java
CustomerProviderSearcher.search(SearchProvidersRequest) : SearchProvidersResponse
    // CustomerSearchProviders → SearchPerformedIE
    // request: the jobs, start, hours, helpers, and a signed-in customer's id, or a guest's city or pin
    // domain: SearchCriteria.of, FreeTime.nearestStart (DP1), MatchClassifier.classify
    // response: exact and close matches with their reasons, suggested start, name, picture, price, Instant book, Insured

// Event handlers: each updates its projection only
ListingWentLiveIEHandler, ListingChangedIEHandler, ListingWentOfflineIEHandler, ListingPausedIEHandler,
ListingResumedIEHandler, ListingHoursNarrowedIEHandler, ListingWentOfflineForHoursIEHandler,
ListingWentOfflineForRetiredJobIEHandler, ProviderInsuranceDeclaredIEHandler, ProviderInsuranceClearedIEHandler  // ListingSearchView
ProviderFreeTimeChangedIEHandler                                                     // FreeTimeView
ProviderVerifiedIEHandler, ProviderReverifiedIEHandler, ReverificationRequiredIEHandler  // VerifiedUntilView
HomaPolicyChangedIEHandler                                                           // SearchLimitsView
JobAddedIEHandler, JobRenamedIEHandler, JobRegroupedIEHandler, ToolAddedIEHandler, ToolRenamedIEHandler  // CatalogueNamesView
```

**Rules**
- **DI1. Every search is recorded without the customer.** `SearchPerformedIE` carries the jobs, start,
  hours, helpers, city and the numbers of exact and close matches; never a customer's id, name or
  address, nor a guest's pin.
- **DI2. A paused or offline listing is no match.**

```gherkin
Feature: Searching

  Scenario: A guest searches by city and the search is recorded without them
    Given Kemas is live in Cyberjaya and free on Saturday from 10:00 to 13:00
    When a guest searches Cyberjaya for KITCHEN and LAUNDRY_FOLD, Saturday 10:00, 3 hours, 2 helpers
    Then Kemas is an exact match
    And SearchPerformedIE is published with 1 exact match and no customer

  Scenario: A close match comes with a bookable start
    Given Kemas is free on Saturday only from 11:00
    When Sarah searches for Saturday 10:00
    Then Kemas is a close match, NEARBY_TIME, at Saturday 11:00
```

---

## reservation

**Purpose:** enquiries and the reservation lifecycle, from the request to its ending, with every time
and money rule.

| Out port | Kind |
|---|---|
| `EnquiryRepository`, `ReservationRepository` | Repository |
| `ReservationIEPublisher` | IE publisher |
| `AdminAuditLog` | Audit log |
| `OfferingView`, `PolicyView`, `ProviderStandingView`, `TeamRosterView`, `CustomerView`, `FreeTimeView`, `OpenCaseView` | Projections |
| `DistanceMeter` | Client: the geoservice's distance from the device to the customer's location |
| `IdentityVerificationService` | Client: opens the KYC provider's face check for a named person |

Photos and videos are stored by infra; usecases receive their references only. This is the biggest
context; build it in the slices under **Build order**.

```java
// Customer
CustomerEnquiryCreator.create(CreateEnquiryRequest) : …Response          // CustomerCreateEnquiry → EnquiryCreatedIE
CustomerBookingRepeater.bookAgain(BookAgainRequest) : …Response          // CustomerBookAgain → EnquiryCreatedIE (a past enquiry or reservation; the current address)
CustomerReservationRequester.request(RequestReservationRequest) : …Response
    // CustomerRequestReservation → ReservationRequestedIE, and EnquiryRequestedIE from an enquiry
    // request: customer id, provider id, jobs, start, hours, helpers, payment method, optional enquiry id
    // domain: Reservation.request (OfferingView, PolicyView, ProviderStandingView, FreeTime), then changeAccessNotes with the account's notes
CustomerRequestEditor.edit(EditRequestRequest) : …Response               // CustomerEditRequest → ReservationEditedIE
CustomerReservationAccessNotesChanger.change(…) : …Response              // CustomerChangeAccessNotes → AccessNotesChangedIE
CustomerReservationCanceller.cancel(…) : …Response                       // CustomerCancelReservation → RequestCancelledIE, or ReservationCancelledIE and SettlementDecidedIE
CustomerNoShowChallenger.challenge(…) : …Response                        // CustomerChallengeNoShow → NoShowChallengedIE
CustomerReportFiler.file(…) : …Response                                  // CustomerFileReport → ReportFiledIE
CustomerCancellationTermsViewer.view(…) : …Response                      // CustomerViewCancellationTerms
CustomerReservationViewer.view(…), CustomerReservationsViewer.view(…)    // status, team, check-in and face check results, completion

// Provider
ProviderRequestsViewer.view(…)                                           // ProviderViewRequests: only requests whose payment is secured (RE1)
ProviderRequestAccepter.accept(…)                                        // ProviderAcceptRequest → ReservationAcceptedIE (DP1, DP4)
ProviderRequestDecliner.decline(…)                                       // ProviderDeclineRequest → ReservationDeclinedIE
ProviderReservationCanceller.cancel(…)                                   // ProviderCancelReservation → ReservationCancelledIE, SettlementDecidedIE, FineChargedIE when fined
ProviderTeamNamer.name(…)                                                // ProviderNameTeam → TeamNamedIE (the roster from TeamRosterView)
ProviderCheckInRecorder.checkIn(…)                                       // ProviderCheckIn → ProviderCheckedInIE (DistanceMeter, then checkIn)
ProviderFaceCheckStarter.start(…)                                        // ProviderStartFaceCheck: opens the KYC provider's face check for a named person
ProviderCustomerNoShowReporter.report(…)                                 // ProviderReportCustomerNoShow → ReservationCancelledIE (the settlement waits, RE6)
ProviderCompletionSubmitter.submit(…)                                    // ProviderSubmitCompletion → CompletionSubmittedIE
ProviderReportFiler.file(…)                                              // ProviderFileReport → ReportFiledIE
ProviderReservationViewer.view(…), ProviderReservationsViewer.view(…)    // access notes only from acceptance

// KycProvider, Admin and Homa
KycProviderFaceCheckRecorder.record(…)                // KycProviderRecordFaceCheck → FaceCheckRecordedIE; CheckInCompletedIE on the first pass
HomaFaceCheckOutageRecorder.record(…)                 // HomaRecordFaceCheckOutage → CheckInCompletedIE (infra found the face check service down)
AdminFlaggedReservationCanceller.cancel(…)            // AdminCancelFlaggedReservation → ReservationCancelledIE, SettlementDecidedIE (with a reason)
AdminFlaggedReservationRestorer.restore(…)            // AdminRestoreFlaggedReservation → ReservationRestoredIE (with a reason; once the flag is cleared)
AdminReservationViewer.view(…), AdminReservationsViewer.view(…)  // the full history; evidence only with an open case (OpenCaseView)
HomaDueWorkLister.list(…)                             // HomaListDueWork: by kind, the ids due now
HomaRequestExpirer.expire(…)                          // HomaExpireRequest → ReservationExpiredIE
HomaTeamReminder.remind(…)                            // HomaRemindTeam → TeamReminderDueIE (DP3)
HomaProviderNoShowMarker.mark(…)                      // HomaMarkProviderNoShow → ReservationCancelledIE, SettlementDecidedIE, FineChargedIE
HomaNoShowReportWindowCloser.close(…)                 // HomaCloseNoShowReportWindow → SettlementDecidedIE (RE6)
HomaCompletionReminder.remind(…)                      // HomaRemindCompletion → CompletionReminderDueIE (DP3)
HomaCompletionDeadlineMisser.miss(…)                  // HomaMissCompletionDeadline → CompletionDeadlineMissedIE
HomaDisputeWindowCloser.close(…)                      // HomaCloseDisputeWindow → ReservationCompletedIE, SettlementDecidedIE
HomaEnquiryCloser.close(…)                            // HomaCloseEnquiry → EnquiryClosedIE
HomaUndecidedFlaggedReservationCanceller.cancel(…)    // HomaCancelUndecidedFlaggedReservation → ReservationCancelledIE, SettlementDecidedIE

// Event handlers
PaymentSecuredIEHandler        // confirmPayment; for Instant book, acceptInstantly (RE7) → ReservationAcceptedIE
PaymentFailedIEHandler         // paymentFailed
CaseDecidedIEHandler           // resolve → ReservationCompletedIE or ReservationCancelledIE, and SettlementDecidedIE
ProviderFlaggedIEHandler       // ProviderStandingView; disputeForProviderFlag on every accepted reservation with no completion → ReservationFlaggedIE each
ProviderVerifiedIEHandler, ProviderReverifiedIEHandler, ReverificationRequiredIEHandler  // ProviderStandingView (a pass clears the flag)
TeamMemberLapsedIEHandler, TeamMemberRemovedIEHandler  // TeamRosterView; dropFromTeam on every reservation naming them (RE9) → TeamMemberDroppedIE or TeamMemberStoppedIE
TeamMemberAddedIEHandler, TeamMemberRoleChangedIEHandler, TeamMemberReverifiedIEHandler  // TeamRosterView
ProviderFreeTimeChangedIEHandler  // FreeTimeView; declineForTakenTime on open requests no longer free (RE8) → RequestDeclinedForTakenTimeIE each
ListingWentLiveIEHandler, ListingChangedIEHandler, ListingWentOfflineIEHandler, ListingPausedIEHandler,
ListingResumedIEHandler, ListingHoursNarrowedIEHandler, ListingWentOfflineForHoursIEHandler,
ListingWentOfflineForRetiredJobIEHandler  // OfferingView
HomaPolicyChangedIEHandler     // PolicyView
CustomerRegisteredIEHandler, CustomerAddressChangedIEHandler, CustomerAccessNotesChangedIEHandler  // CustomerView
CaseOpenedIEHandler, CaseDecidedIEHandler (above), CaseClosedForProviderFlagIEHandler  // OpenCaseView
```

**Rules**
- **RE1. A request is paid before the provider answers.** The provider sees a request, and can accept it,
  only once `PaymentSecuredIE` has recorded an amount that covers the total. An edit that raises the
  total waits for payment again; the expiry still runs from the first request.
- **RE2. A request ends with one event, and never waits unpaid.** Cancelling a request publishes
  `RequestCancelledIE` only. `PaymentFailedIE` ends the request (`paymentFailed`) and publishes nothing
  more. A late `PaymentSecuredIE` or `PaymentFailedIE` for a reservation that is no longer a request is
  refused by the domain; the handler records it and changes nothing.
- **RE3. The provider's standing is as it is now.** `request`, `edit`, `accept` and `acceptInstantly`
  take the verified-until date from `ProviderStandingView`, the listing's current state from
  `OfferingView` (DP4), and the free time from `FreeTimeView` (DP1).
- **RE4. The acceptance holds the slot.** The free-time check at acceptance can be a moment stale; the
  hold in the acceptance's transaction decides (AV1).
- **RE5. A seriously flagged provider's reservations.** `ProviderFlaggedIEHandler` disputes every
  accepted reservation with no completion, including one already disputed over the job. Admins cancel
  each with a full refund, or restore it once the flag is cleared and before the start;
  `HomaCancelUndecidedFlaggedReservation` cancels any undecided at the booked start (A3).
- **RE6. A customer no-show's settlement waits for its window.** `ProviderReportCustomerNoShow`
  publishes `ReservationCancelledIE` only; `HomaCloseNoShowReportWindow` sends the no-show settlement
  when the report window ends unchallenged. If it is challenged, Homa's decision is the only settlement
  sent. A customer's cancellation after check-in is settled at once.
- **RE7. Instant book accepts on payment.** `PaymentSecuredIEHandler` calls `acceptInstantly` for a
  request whose snapshot says Instant book, with the checks of RE3 and RE4. If the acceptance is
  refused, the request stays Requested; if its time is taken, RE8 declines it.
- **RE8. Taken time declines open requests.** When a provider's free time changes, each of their open
  requests whose slot is no longer free is declined at once with `declineForTakenTime` (U2).
- **RE9. A person who comes off the team comes off their jobs.** On `TeamMemberLapsedIE` and
  `TeamMemberRemovedIE`, every reservation naming them that is reserved or disputed over the job gets
  `dropFromTeam`.
- **RE10. A reminder is sent once** (DP3).
- **RE11. A party acts only on their own reservation** (`NotYourReservationException`); the provider
  sees access notes only from acceptance.

```gherkin
Feature: Requesting and accepting

  Background:
    Given Kemas is live in Cyberjaya: RM30 per helper per hour, travel RM20, Standard, Request and accept
    And Sarah lives at "12 Jalan Teknokrat 3", Cyberjaya, with access notes "Unit 12-3"

  Scenario: A request is saved with the customer's address and notes, and told to payment
    When Sarah requests KITCHEN and LAUNDRY_FOLD on Saturday 10:00 for 3 hours and 2 helpers by card
    Then the reservation is saved as Requested at Sarah's address, with her access notes
    And ReservationRequestedIE is published with the total RM230.00

  Scenario: The provider can't accept until the request is paid
    Given Sarah's request is not paid yet
    When Nur Aina accepts it
    Then it is refused with PaymentNotSecuredException
    And nothing is saved or published

  Scenario: Instant book accepts the moment the payment is secured
    Given Kemas uses Instant book
    When PaymentSecuredIE arrives for Sarah's request with RM230.00
    Then the reservation is saved as Reserved
    And ReservationAcceptedIE is published, saying Instant book

  Scenario: An open request whose time is taken is declined
    Given Kemas has Sarah's paid request for Saturday 10:00 and another for Saturday 11:00
    When ProviderFreeTimeChangedIE arrives without Saturday 10:00 to 14:00
    Then both requests are saved as Declined, with the ending TIME_TAKEN
    And RequestDeclinedForTakenTimeIE is published for each

Feature: Job day and after

  Scenario: A named person who lapses comes off the team before the start
    Given Sarah's reservation names Aina and Ben for Saturday 10:00
    When TeamMemberLapsedIE arrives for Ben on Friday 18:00
    Then the reservation is saved with Aina only
    And TeamMemberDroppedIE is published, saying the team is not complete

  Scenario: The team reminder is sent once
    Given Sarah's reservation has no team named at Saturday 04:00
    When Homa reminds the team, and reminds it again
    Then TeamReminderDueIE is published once

  Scenario: An unchallenged no-show is settled when its window ends
    Given Nur Aina reported Sarah a no-show at Saturday 10:15
    When Homa closes the no-show report window at Saturday 11:15
    Then SettlementDecidedIE is published: Kemas earns RM74.00, for a CUSTOMER_NO_SHOW_FEE, and Sarah is refunded RM144.90
```

---

## payment

**Purpose:** money: authorising, charging and holding the customer's payment; settling it; refunds;
payouts with fine deductions; fines, holds and chargebacks.

| Out port | Kind |
|---|---|
| `PaymentRepository`, `PayoutRepository`, `FineRepository` | Repository |
| `PaymentIEPublisher` | IE publisher |
| `AdminAuditLog` | Audit log |
| `ReservationMoneyView`, `PayoutStandingView`, `FineCapView` | Projections |
| `PaymentGateway` | Client: starts a card authorisation or an FPX payment, replaces an authorisation, captures, releases, refunds, transfers a payout |

```java
// Customer
CustomerRequestPayer.pay(PayForRequestRequest) : PayForRequestResponse   // CustomerPayForRequest: starts paying what is owed now (the result arrives by webhook)
CustomerPaymentsViewer.view(…)                                           // CustomerViewPayments: payments and refunds, with status and expected timing
CustomerProviderRecordViewer.view(…)                                     // CustomerViewProviderRecord: jobs done (completed-job payouts) and cancellations (late-cancellation and no-show fines, waived or not)

// Provider
ProviderIncomeViewer.view(…)                                             // ProviderViewIncome: earnings, payouts, fines being deducted

// PaymentProvider (webhooks)
PaymentProviderPaymentSecuredRecorder.record(…)    // PaymentProviderRecordPaymentSecured → PaymentSecuredIE (authoriseCard, payUpfrontByFpx, or adjustTotal after an edit)
PaymentProviderPaymentFailedRecorder.record(…)     // PaymentProviderRecordPaymentFailed → PaymentFailedIE (after an edit: cancelBeforeAcceptance first)
PaymentProviderRefundRecorder.record(…)            // PaymentProviderRecordRefund → PaymentRefundedIE
PaymentProviderPayoutFailedRecorder.record(…)      // PaymentProviderRecordPayoutFailed (retried; shown to admins)
PaymentProviderChargebackRecorder.record(…)        // PaymentProviderRecordChargeback → ChargebackOpenedIE

// Admin and Homa
AdminFineWaiver.waive(…)                           // AdminWaiveFine → FineWaivedIE (a reason category and text, A5)
AdminPayoutReleaser.release(…)                     // AdminReleasePayout → PayoutHoldReleasedIE (with a reason)
AdminPayoutWithholder.withhold(…)                  // AdminWithholdPayout → PayoutWithheldIE (with a reason)
AdminPaymentsViewer.view(…), AdminFinesViewer.view(…)  // with links to the payment provider's console
HomaDueWorkLister.list(…)                          // HomaListDueWork: payouts and releases due
HomaPayouter.payOut(…)                             // HomaPayOut: transfers a payable payout once its release time has come and the payout account is ready

// Event handlers
ReservationRequestedIEHandler, ReservationEditedIEHandler  // ReservationMoneyView; for FPX, a decrease is refunded (adjustTotal) → PaymentRefundedIE
ReservationAcceptedIEHandler       // capture → PaymentCapturedIE
ReservationDeclinedIEHandler, ReservationExpiredIEHandler, RequestCancelledIEHandler,
RequestDeclinedForTakenTimeIEHandler  // cancelBeforeAcceptance → PaymentRefundedIE for FPX
SettlementDecidedIEHandler         // settle; Payout.create with the kind from earningFor (DP2); FineDeductionPolicy.apply; hold for a flagged business → PayoutCreatedIE; PayoutHeldIE
FineChargedIEHandler               // Fine.charge
ChargebackDecidedIEHandler         // Fine.charge with CHARGEBACK for what the provider covers
ProviderFlaggedIEHandler           // PayoutStandingView; hold every payable payout → PayoutHeldIE each
ProviderReverifiedIEHandler, PayoutAccountReadyIEHandler  // PayoutStandingView
HomaPolicyChangedIEHandler         // FineCapView
```

**Rules**
- **PA1. Money moves only through Homa's rules** (A6): a refund, release or payout is made only from a
  domain instruction.
- **PA2. Instructions wait for their time.** A refund goes out at once; the provider's earning and
  Homa's fee go out from the settlement's release time, through `HomaPayOut`.
- **PA3. Paying more after an edit is the customer's step.** An edit that raises the total is paid with
  `CustomerPayForRequest`, as the first payment was (U3); a decrease needs nothing from the customer.
- **PA4. A withheld payout stays with Homa**, owed to the business, never revenue.
- **PA5. Processing fees are Homa's.** No usecase passes a payment provider's fee to a customer or a
  provider.

```gherkin
Feature: Settling and paying out

  Scenario: A completed job's payout has a fine deducted within the cap
    Given Kemas owes a RM54.00 fine
    When SettlementDecidedIE arrives for Sarah's job: Kemas earns RM200.00, for a COMPLETED_JOB
    Then the payment is settled and a payout of RM200.00 is saved with RM54.00 deducted
    And PayoutCreatedIE is published with RM146.00 net

  Scenario: A cancellation fee's payout has no fine deducted
    Given Kemas owes a RM54.00 fine
    When SettlementDecidedIE arrives with Kemas earning RM18.00, for a CANCELLATION_FEE
    Then a payout of RM18.00 is saved with nothing deducted

  Scenario: A flagged business's payout is held
    Given Kemas is seriously flagged
    When SettlementDecidedIE arrives for a completed job
    Then the payout is saved held
    And PayoutHeldIE is published

  Scenario: A chargeback reaches the admins
    When the payment provider reports a RM230.00 chargeback on Sarah's payment
    Then ChargebackOpenedIE is published for Sarah's reservation with RM230.00
```

---

## resolution

**Purpose:** the admins' case queue and their decisions.

| Out port | Kind |
|---|---|
| `CaseRepository` | Repository |
| `ResolutionIEPublisher` | IE publisher |
| `AdminAuditLog` | Audit log |

```java
AdminCaseDecider.decide(DecideCaseRequest) : DecideCaseResponse   // AdminDecideCase → CaseDecidedIE, or ChargebackDecidedIE for a chargeback (with a reason)
AdminCaseQueueViewer.view(…)                                       // AdminViewCaseQueue: Case.QUEUE_ORDER, with each case's time left of its 72 hours
AdminCaseViewer.view(…)                                            // AdminViewCase: the case and its evidence
CustomerCaseViewer.view(…), ProviderCaseViewer.view(…)             // status, and when the decision is due

// Event handlers
ReportFiledIEHandler               // Case.open or addEvidence; markSafety for a safety, damage or loss report → CaseOpenedIE for a new case
NoShowChallengedIEHandler          // Case.open → CaseOpenedIE
CompletionDeadlineMissedIEHandler  // Case.open → CaseOpenedIE
ChargebackOpenedIEHandler          // Case.open with CHARGEBACK → CaseOpenedIE
ReservationFlaggedIEHandler        // closeForProviderFlag on the reservation's open case → CaseClosedForProviderFlagIE
```

**Rules**
- **RS1. One decision event per outcome** (A2): a chargeback case publishes `ChargebackDecidedIE` with
  what the provider covers; every other case publishes `CaseDecidedIE`.
- **RS2. The decision target is 72 hours,** every day counted. The queue and both parties see when each
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
    Then the case is saved with Kemas's evidence added, and no new case is opened
```

---

## conversation

**Purpose:** the chats for enquiries and reservations.

| Out port | Kind |
|---|---|
| `ConversationRepository` | Repository |
| `PeopleView`, `OpenCaseView` | Projections |

```java
CustomerChatStarter.start(…)          // CustomerStartChat: an enquiry's chat, with the first message
CustomerMessagePoster.post(…)         // CustomerPostMessage
ProviderMessagePoster.post(…)         // ProviderPostMessage
CustomerInboxViewer.view(…), ProviderInboxViewer.view(…)  // chats grouped by provider, or by customer
CustomerChatViewer.view(…), ProviderChatViewer.view(…)
AdminChatViewer.view(…)               // AdminViewChat: only while the reservation has an open case

// Event handlers
EnquiryRequestedIEHandler             // linkReservation
EnquiryClosedIEHandler                // archive
ReservationRequestedIEHandler         // openForReservation, for a request not from an enquiry (fromEnquiry false)
CustomerRegisteredIEHandler, CustomerRenamedIEHandler, ListingWentLiveIEHandler, ListingChangedIEHandler  // PeopleView
CaseOpenedIEHandler, CaseDecidedIEHandler, CaseClosedForProviderFlagIEHandler  // OpenCaseView
```

**Rules**
- **CO1. A party writes and reads only their own chats** (`NotYourChatException`).
- **CO2. Admins open a chat only for a case** (A4).

---

## feedback

**Purpose:** customers rate completed reservations; the averages are shown.

| Out port | Kind |
|---|---|
| `RatingRepository`, `RatingSummaryRepository` | Repository |
| `FeedbackIEPublisher` | IE publisher |
| `AdminAuditLog` | Audit log |
| `RateableReservationView`, `FeedbackLimitsView` | Projections |

```java
CustomerReservationRater.rate(…)        // CustomerRateReservation → RatingSubmittedIE (Rating.submit; RatingSummary.record for the provider and each rated person)
AdminRatingHider.hide(…)                // AdminHideRating → RatingHiddenIE (Rating.hide; RatingSummary.withdraw; with a reason)
CustomerProviderRatingsViewer.view(…)   // averages once there are enough; written feedback, newest first
CustomerTeamRatingsViewer.view(…)       // each named person's overall average
AdminRatingsViewer.view(…)              // every rating, hidden included

// Event handlers
ReservationRequestedIEHandler, TeamNamedIEHandler, ReservationCompletedIEHandler  // RateableReservationView
HomaPolicyChangedIEHandler                                                        // FeedbackLimitsView
```

**Rules**
- **FE1. A reservation is rated once:** the rating and the reservation's rated mark are saved in the same
  change.
- **FE2. A rating never changes money.** No context handles the rating events for payouts, fines or
  decisions.

---

## notification

**Purpose:** telling people what happened and what to do, in their language (Bahasa Melayu or English),
by push and, where it matters most, by SMS. Its domain comes first (U5).

| Out port | Kind |
|---|---|
| `NoticeRepository`, `PersonSettingsRepository` | Repository |
| `RecipientsView` | Projection |
| `PushSender`, `SmsSender` | Clients |

```java
CustomerLanguageChooser.choose(…), ProviderLanguageChooser.choose(…)
CustomerDeviceRegistrar.register(…), ProviderDeviceRegistrar.register(…)
CustomerNoticesViewer.view(…), ProviderNoticesViewer.view(…)

// Event handlers: one per event that tells someone, for example
PaymentSecuredIEHandler         // the provider: a new request (or, for Instant book, a new booking)
ReservationAcceptedIEHandler    // the customer: booked
TeamNamedIEHandler, TeamMemberDroppedIEHandler, TeamMemberStoppedIEHandler  // the customer: who is coming
ReservationFlaggedIEHandler     // the customer: the job is on hold
TeamReminderDueIEHandler, CompletionReminderDueIEHandler  // the provider
CaseDecidedIEHandler            // both parties: the outcome
… and the others listed under the context map
```

**Rules**
- **NO1. Telling never blocks a change.** A notice is sent after the change that caused it is committed
  (I3); a failed send is retried and never undoes the change.

---

## privacy

**Purpose:** admins record and answer requests to see, correct, copy or delete personal data, under the
PDPA. Its domain comes first (U5).

| Out port | Kind |
|---|---|
| `DataRequestRepository` | Repository |
| `AdminAuditLog` | Audit log |

```java
AdminDataRequestRecorder.record(…)    // AdminRecordDataRequest
AdminDataRequestCloser.close(…)       // AdminCloseDataRequest (with what was done)
AdminDataRequestsViewer.view(…)       // AdminViewDataRequests
```

---

## tracking

**Purpose:** the admins' goals and guardrails, from the spec's Goals and Measures. It listens to every
event and is read-only: nothing depends on it, and if it falls behind no rule is affected. Its domain,
the measure definitions, comes first (U5).

| Out port | Kind |
|---|---|
| One store per view | View stores |

```java
AdminGoalsViewer.view(…)            // AdminViewGoals: the measures in Goals and Measures, with trends, by city and month
AdminGuardrailsViewer.view(…)       // AdminViewGuardrails: acceptance and response time, expiry, cancellation and no-show rates, disputes, face check failures, lapses and flags, the fine waiver rate
AdminSearchGapsViewer.view(…)       // AdminViewSearchGaps: searches with no or only close matches
AdminFlaggedProvidersViewer.view(…) // AdminViewFlaggedProviders: each flagged business, its disputed reservations and held payouts

// Event handlers: one per event, each updating the views it feeds
```

Every other admin view the spec lists belongs to the context that owns its data: reservations
(`AdminViewReservations`), payments and fines (`AdminViewPayments`, `AdminViewFines`), providers and
customers (`AdminViewProvider`, `AdminViewCustomer`), ratings (`AdminViewRatings`).

---

## Integration events (`ie/`)

`ie/` is in this phase's scope. It holds only event interfaces (`ie/port`) and event records
(`ie/events`), with no behaviour (Discipline §2). Its rules stay as they are:

- Events carry primitives, Strings, `kernel` types and JDK value types (`Instant`, `Duration`, immutable
  `List` and `Set` of Strings); never a domain object; no `Optional` field. A value that is not always
  there is split into its own event, or carried as an empty String or 0, as the event documents.
- Collections are copied in the record's compact constructor; every reference field is required.
- One event interface per publishing context; every event implements it.
- One event per outcome, never one generic event with a status.
- `ie/` depends only on `kernel/`, and no domain imports it.

### New event interface

| Interface | Context | Why |
|---|---|---|
| `AvailabilityIE` | availability | availability publishes for the first time (DP1) |

### New events

| Event | Published by | Handled by | Fields | Carries the result of |
|---|---|---|---|---|
| `ProviderFreeTimeChangedIE` | availability | discovery, reservation | `providerId`, `freeStretches` (`List<String>`, each `start/end` in ISO-8601), `from`, `until`, `occurredAt` | `ProviderCalendar.freeTime` (DP1) |
| `CustomerAddressChangedIE` | account | reservation | `customerId`, `street`, `city`, `latitude`, `longitude` (Strings, decimal degrees), `occurredAt` | `CustomerAccount.changeAddress` |
| `ChargebackOpenedIE` | payment | resolution | `reservationId`, `paymentId`, `amount` (`Money`), `occurredAt` | the payment provider's chargeback report |
| `CaseOpenedIE` | resolution | reservation, conversation, notification, tracking | `caseId`, `reservationId`, `source`, `safety` (`boolean`), `occurredAt` | `Case.open` (and `markSafety`) |
| `TeamReminderDueIE` | reservation | notification | `reservationId`, `providerId`, `start`, `occurredAt` | `Reservation.recordTeamReminded` (DP3) |
| `CompletionReminderDueIE` | reservation | notification | `reservationId`, `providerId`, `end`, `occurredAt` | `Reservation.recordCompletionReminded` (DP3) |

### Changed events

| Event | Change | Why |
|---|---|---|
| `CustomerRegisteredIE` | adds `street`, `latitude`, `longitude` | reservation needs the customer's address and location for a request and for check-in |
| `ListingWentLiveIE`, `ListingChangedIE` | add `displayName`, `picture` (empty when none) | search results, chats and notices show the provider's name and picture |
| `ReservationRequestedIE` | adds `fromEnquiry` (`boolean`) | conversation opens a reservation's chat only for a request not made from an enquiry |
| `SettlementDecidedIE` | adds `earningFor` (`COMPLETED_JOB`, `CANCELLATION_FEE` or `CUSTOMER_NO_SHOW_FEE`) | payment deducts fines only from payouts for jobs (DP2) |

### Work in `ie/`

1. Add the interface, the six events and the four changes, each with the domain prerequisite it
   belongs to, in the same step.
2. `IntegrationEventContractTest`: add the new events (82 become 88), so each is checked for its
   required fields, immutability, allowed types and its publisher's interface.
3. `IntegrationEventArchitectureTest` stays as it is: `ie/` imports only `kernel` and Java.
4. Update the domain plan's event table and the domain audit's event count with each change.

---

## Rules for the admin console

Every admin usecase follows these. Each has a usecase test.

1. **A1. Every decision records who and why.** An admin usecase takes the admin's id (`decidedBy`,
   from authentication) and, for a decision about a person, money or providers' listings, a reason. It
   writes who, what, why and when to its context's `AdminAuditLog`. Plain curation records who, what
   and when. A missing admin or reason is refused before anything else (`DecidedByRequiredException`,
   `AdminReasonRequiredException`).
2. **A2. One event per outcome.** A usecase publishes the event of the outcome the domain returned,
   never a generic event with a status.
3. **A3. Red-code rules can't be overridden.** A flagged reservation is cancelled with a full refund, or
   restored only once the flag is cleared; there is no "let it go ahead".
4. **A4. Personal data only where needed.** Chats and evidence open only for a case. Identity documents
   and selfies stay with the KYC provider; card details stay with the payment provider.
5. **A5. Fine waivers stay exceptional.** A waiver has a reason category (customer-caused, emergency,
   Homa fault, other) and text, kept in the audit log; it is given once and only for what is left, and
   covers only the money.
6. **A6. Money moves only through Homa's rules.** Admins never refund or pay out directly in the payment
   provider's console for a reservation.

## Rules for the infra phase

The usecase ports promise these; infra keeps them, each with an infra test.

1. **I1. A save never overwrites a change made meanwhile.** Every repository refuses a save on an
   out-of-date copy with its context's `ChangedMeanwhileException` (optimistic locking). The Homa-set
   values save only as the next version.
2. **I2. Only authentication says who is acting.** `infra/security/` sets the customer's, provider's or
   admin's id from the authenticated user; a controller never takes it from the request body. Only
   admins reach `Admin…` ports; only the KYC provider's and payment provider's signed webhooks reach
   theirs; only the scheduler reaches `Homa…` ports.
3. **I3. A change and its events are all or nothing.** The save, the events and the audit entry of one
   usecase happen in one transaction. Events go out through an outbox written in that transaction
   (Discipline §10), so a change is never saved without its event and an event is never sent for a
   change that was rolled back. The one handler that runs inside the publishing transaction is the
   slot hold (AV1), because it guards an invariant across two contexts.
4. **I4. Webhooks are checked and deduplicated.** The KYC provider's and payment provider's webhooks are
   verified by signature, and a repeated delivery is skipped by its id before any usecase runs.
5. **I5. Aggregates are rebuilt from storage by infra.** Domain constructors are private and generate
   new ids, so each repository rebuilds an aggregate as it was saved; usecase tests keep aggregates in
   memory.

## Build order

Each step passes its gates before the next starts (Discipline §9). A step's domain prerequisite and its
`ie/` changes come first in that step.

1. **Test support:** the fakes for `AdminAuditLog` and `ProcessedEvents`, a fixed clock, and the shape of
   each context's `<Context>UsecaseArchitectureTest`.
2. `catalogue`
3. `policy`
4. `account` (with `CustomerRegisteredIE` changed and `CustomerAddressChangedIE` added)
5. `offering` (with `ListingWentLiveIE` and `ListingChangedIE` changed)
6. `availability` (DP1 in `availability`; `AvailabilityIE` and `ProviderFreeTimeChangedIE`)
7. `discovery` (DP1 in `discovery`)
8. `reservation` (DP1 in `reservation`, DP2, DP3, DP4; `SettlementDecidedIE` and `ReservationRequestedIE`
   changed; `TeamReminderDueIE` and `CompletionReminderDueIE` added), in slices:
   1. Enquiries, requests and edits
   2. Payment results, acceptance, decline, expiry, Instant book and taken time
   3. Cancellations and fines
   4. The team, reminders, and coming off the team
   5. Check-in, face checks and no-shows
   6. Completion, reports, the dispute window and decisions
   7. Flagged providers
   8. Queries
9. `payment` (`ChargebackOpenedIE` added)
10. `resolution` (`CaseOpenedIE` added)
11. `conversation`
12. `feedback`
13. `notification`, `privacy` and `tracking`, once U5 is decided, each with its domain first
14. **`app/`:** the composition root with in-memory adapters, and one end-to-end test of the running
    example: Sarah books Kemas, the team checks in and completes, Kemas is paid and Sarah rates.

## Testing approach

- **What a usecase test proves:** the orchestration, never the domain's rules again: what is loaded,
  what is passed to the domain, what is saved, published and recorded, and that a refused change saves,
  publishes and records nothing (Discipline §9: behavioural focus).
- **Framework-free,** against in-memory fakes of every out port, kept in each context's
  `<Context>UsecaseFixtures` (shared fixtures). Each repository fake can be told to refuse a save, so
  the order of a change is tested.
- **Every Gherkin scenario becomes a test**, and the domain audit gains a usecase section that maps
  each scenario to its test.

### Test naming convention

Tests are named in the ubiquitous language, as in the domain plan: they say the business outcome, never
the class or method under test.

**Test classes:** one per feature, named for it, in the context's `usecase` test package.

| Feature | Test class |
|---|---|
| Requesting and accepting | `RequestingAndAcceptingTest` |
| Adding a team member | `AddingATeamMemberTest` |
| Settling and paying out | `SettlingAndPayingOutTest` |
| The admins' queue | `TheAdminsQueueTest` |

**Test methods:** one per scenario, a camelCase sentence of the outcome, such as
`theProviderCannotAcceptUntilTheRequestIsPaid` or `anUnchallengedNoShowIsSettledWhenItsWindowEnds`.
Don't start with `test` or `should`, and don't use technical words the business doesn't use.

### Architecture test per context

`<Context>UsecaseArchitectureTest` (ArchUnit) checks that:
- the usecase layer depends only on its own domain, `kernel`, `ie` and Java;
- in ports and out ports are interfaces; DTOs are records and hold no domain object or event;
- every usecase implements exactly one in port, and no other usecase implements it;
- every handler is named `Handle<Event>` and implements `<Event>Handler`;
- usecase exceptions live in `usecase/exception`.

### Gates per context

Unit tests; CRAP below 6; mutation at least 80% for `usecase`, reporting score and test strength, with
the ratchet; and the dependency rule (Discipline §9). For `ie/`, the contract test; it has no mutation
gate, because records carry no logic.

## Open questions

1. **U1. Free time across contexts (DP1).** Reservation and discovery need availability's answer, and
   the discipline allows contexts to talk only through events. *Recommendation:* availability publishes
   each provider's free time, and the slot hold inside the acceptance's transaction stays the final
   word. The cost is a small domain change in three contexts and a view for search that can be a moment
   stale, which the hold covers.
2. **U2. What declines a request for taken time.** The spec declines open requests whose time clashes
   with an accepted booking or its buffer. With DP1, any change that takes the time, such as a synced
   calendar event, declines them too. *Recommendation:* widen the spec to "any open request the provider
   can no longer take", in line with "nobody waits on something that can't happen".
3. **U3. Paying for an edit that raises the total.** *Recommendation:* the customer pays the difference
   with `CustomerPayForRequest`, as for the first payment, since a card may need them to confirm again.
4. **U4. Deleting personal data across contexts.** *Recommendation:* in the MVP, admins carry out a
   deletion by hand, context by context, and record it with `AdminCloseDataRequest`; automated deletion
   comes after launch.
5. **U5. `notification`, `privacy` and `tracking`.** The discipline gives every bounded context a
   domain, a usecase and an infra layer, and the domain plan left these three out of the domain phase.
   *Recommendation:* add each as a bounded context with a small domain first: `notification` (a notice,
   its recipient, language and channel), `privacy` (a data request and its status) and `tracking` (the
   measure definitions in Goals and Measures).
