# Homa Usecase Development Plan

This plan covers the **usecase module of each bounded context** (the orchestrators, their ports, DTOs and exceptions) and the **`ie/` module** (the integration events the usecases publish and handle). It follows Backend Module Discipline §2, §3, §6, §7 and §8, step 2.

**Status:** draft for review. Nothing is built.

**Sources:**
- `homa-product-description.md` (the spec) says **what** each usecase must do. Every usecase below names the spec section it comes from.
- `backend_module_discipline.md` (the discipline) says **how** a usecase module is built.

When this plan and the spec disagree, the spec wins; fix this plan.

**Scope:**
- **In:** the `usecase` module of the eleven bounded contexts: `catalogue`, `policy`, `account`, `offering`, `availability`, `reservation`, `payment`, `resolution`, `conversation`, `discovery` and `feedback`.
- **In:** the shared `ie/` module: the event contracts between the contexts, with the few additions the usecases need (see "The `ie/` module").
- **Not the domain.** The usecases orchestrate the domain objects as they are. This plan designs no domain object.
- **Not infra.** Persistence, controllers, webhooks, schedulers, security, event delivery, the adapters to outside services and `app/` come in the next phase. This plan only declares the ports they will implement or call.
- **Not modules without a bounded context:** sending notifications, the admin console's tracking views and data requests (see "Outside this plan").

## Contents

- The usecase module (from the discipline)
- The `ie/` module
- How a usecase gets another context's data
- Usecases per bounded context
- Rules the usecases must keep
- Build order
- Testing and quality gates
- Outside this plan
- Questions answered from the spec
- Open questions

## The usecase module (from the discipline)

**Layout** (§3). Every bounded context gets the same module:

```text
<context>/usecase/
├── port/
│   ├── in/         usecase interfaces and IE handler interfaces
│   └── out/        outbound interfaces: repositories, the IE publisher, outside services
├── dto/            a dedicated request and response per usecase
├── exception/      usecase-level exceptions
└── <Impl>.java     usecase and IE handler implementations
```

**Rules of the module**

| Rule | From | How it is applied here |
|---|---|---|
| A usecase is a business logic orchestrator | §3 | It loads domain objects, calls them, saves and publishes. It decides nothing: each rule stays in the domain object that owns it |
| One usecase, one service | §6 | Each usecase has its own interface and its own implementation. No usecase serves two intents |
| The interface is named for the actor, the implementation for the action | §6 | `CustomerRequestReservation implements CustomerReservationRequester` |
| Intent-revealing methods | §6 | `CustomerReservationRequester.request(...)` |
| Dedicated DTOs | §6 | `RequestReservationRequest`, `RequestReservationResponse`; never shared between usecases, and never a domain object |
| Depends only on domain and shared modules | §3, §5 | Its own context's `domain`, `kernel/` and `ie/`. Never another context, never infra, never a framework |
| Infra is reached through `port/out` | §5 | Storage, outside services and the clock's callers are interfaces the usecase declares |
| The inbound gateway | §4 | Callers depend on the `port/in` interface, never on the implementation |
| One IE publisher per context | §7 | `<Context>IEPublisher.publish(<Context>IE)` in `port/out` |
| One handler per event | §7 | `HandleJobRetiredIE implements JobRetiredIEHandler`, in `port/in` and `usecase/` |
| Usecase-level exceptions | §6 | Orchestration failures only, such as `ReservationNotFoundException`. Business rule violations stay domain exceptions and pass through unchanged |

**Actors.** The interface is named for who acts:

| Actor | Who, in the spec |
|---|---|
| `Customer` | A customer, or a guest when searching |
| `Provider` | The business, through the provider app |
| `Registrant` | Anyone registering with a phone number, before they have an account |
| `Party` | The customer or the provider, where either can act |
| `Homa` | The founders in the admin console, or Homa acting on its own at a set time |
| `Kyc`, `Stripe`, `Calendar` | An outside service reporting a result back |

**Conventions for every usecase**

- **Time:** a usecase takes a `java.time.Clock` and passes the time to the domain.
- **Who is acting** arrives in the request DTO. A usecase checks that the actor owns what they act on, for example `NotYourReservationException`.
- **Events:** a usecase turns what the domain returned into the event from `ie/` and publishes it. One event per outcome.
- **Nothing changed, nothing done.** When the domain says a change changed nothing, the usecase saves and publishes nothing.
- **Order of a change:** check, call the domain, save, then publish. A refused change saves and publishes nothing.
- **A handler whose event no longer applies** changes nothing and does not fail, for example a payment secured for a reservation that is no longer a request.
- **Founders' actions** take the founder's id and record who, what, why and when through an `AdminAuditLog` out port, because the spec says "every change made in the admin console is recorded with who made it and when".
- **Queries** are usecases too, with their own interface and response DTO. A query with no input has no request DTO.

## The `ie/` module

**From the discipline** (§2, §7):

| Rule | How it is applied here |
|---|---|
| `ie/` holds only event interfaces (`ie/port`) and event definitions (`ie/events`) | One interface per publishing context, such as `ReservationIE`; each event is a record that implements it |
| No behaviour and no business logic | An event only carries what one usecase's outcome was |
| Events carry primitives, Strings, kernel types and JDK value types; never domain objects; no `Optional` fields | A value that isn't always there means two events, one per outcome |
| Collections are copied in the record's constructor | Checked by the event contract test |
| `ie/` depends only on `kernel/`, and no `domain` imports it | Only the usecase modules use it: to publish through `<Context>IEPublisher`, and to handle through one handler per event |

**What exists:** 82 events under nine interfaces: `CatalogueIE`, `PolicyIE`, `AccountIE`, `OfferingIE`, `ReservationIE`, `PaymentIE`, `ResolutionIE`, `DiscoveryIE` and `FeedbackIE`. availability and conversation only handle events. Every one of the 82 has a publishing usecase and its handlers in the tables below.

**What this plan adds.** Four events and one change, each because the spec needs a usecase that has nothing to publish or handle today (see "Questions answered from the spec"). That makes 86 events.

| Event | Interface | Carries | Published by | Handled by | Spec |
|---|---|---|---|---|---|
| `CustomerRegisteredIE` (changed) | `AccountIE` | Adds the street, latitude and longitude to the name and city | `CustomerRegisterAccount` | reservation, conversation | Homa uses the address to match providers by city and to locate check-in |
| `CustomerAddressChangedIE` (new) | `AccountIE` | The customer, street, city, latitude and longitude | `CustomerChangeAddress` | reservation | A new address is used for new searches and requests |
| `TeamReminderDueIE` (new) | `ReservationIE` | The reservation, the provider and the booked start | `HomaRemindTeams` | Whoever tells the provider (outside this plan) | Homa reminds the provider at the team reminder time |
| `CompletionReminderDueIE` (new) | `ReservationIE` | The reservation, the provider and the booked end | `HomaRemindCompletions` | Whoever tells the provider (outside this plan) | Homa reminds them to submit it |
| `ChargebackReceivedIE` (new) | `PaymentIE` | The reservation, the payment and the amount the bank took back | `StripeRecordChargeback` | resolution | A customer who disputes a card payment with their bank opens a case for Homa |

- Each new or changed event gets its case in the event contract test: it can only be built with every field, it can't be changed afterwards, it carries only allowed types, and it belongs to its context's interface.
- An event is added to `ie/` in the same step as the usecase that publishes it.

## How a usecase gets another context's data

A bounded context never imports another (§4). So a usecase that needs another context's data reads its own **projection**: a small copy behind an out port, kept up to date by this context's IE handlers.

| Context | Keeps | Kept up to date by handlers of |
|---|---|---|
| offering | The jobs and tools providers can pick, and the cities Homa serves | `JobAddedIE`, `JobRetiredIE`, `ToolAddedIE`, `ToolRetiredIE`, `ServiceAreaPublishedIE` |
| offering | Homa's duration range | `HomaPolicyChangedIE` |
| offering | Whether each business is ready to go live, lapsed or flagged | `ProviderVerifiedIE`, `ProviderVerificationRejectedIE`, `ProviderReverifiedIE`, `ProviderVerificationLapsedIE`, `ProviderFlaggedIE`, `PayoutAccountReadyIE` |
| offering, reservation | Each business's team, by role | `TeamMemberAddedIE`, `TeamMemberRoleChangedIE`, `TeamMemberLapsedIE`, `TeamMemberReverifiedIE`, `TeamMemberRemovedIE` |
| reservation | Each live listing's offering, and whether it is paused | `ListingWentLiveIE`, `ListingChangedIE`, `ListingWentOfflineIE`, `ListingPausedIE`, `ListingResumedIE` |
| reservation | The Homa-set values in force | `HomaPolicyChangedIE` |
| reservation | Each business's verified-until date, and whether it is flagged | `ProviderVerifiedIE`, `ProviderReverifiedIE`, `ReverificationRequiredIE`, `ProviderFlaggedIE` |
| reservation | Each customer's current address with its pin, and their access notes | `CustomerRegisteredIE`, `CustomerAddressChangedIE`, `CustomerAccessNotesChangedIE` |
| payment | Which businesses are flagged | `ProviderFlaggedIE`, `ProviderReverifiedIE` |
| payment | The fine deduction cap in force when each reservation was requested | `HomaPolicyChangedIE`, `ReservationRequestedIE` |
| discovery | Each live listing's search fields, whether it is paused, and its Insured badge | The listing events, `ProviderInsuranceDeclaredIE`, `ProviderInsuranceClearedIE` |
| discovery | Each business's verified-until date, and Homa's search limits | `ProviderVerifiedIE`, `ProviderReverifiedIE`, `ReverificationRequiredIE`, `HomaPolicyChangedIE` |
| discovery | Job and tool names and groups | `JobAddedIE`, `JobRenamedIE`, `JobRegroupedIE`, `ToolAddedIE`, `ToolRenamedIE` |
| discovery | Each provider's jobs done and cancellations | `ReservationCompletedIE`, `SettlementDecidedIE`, `FineChargedIE` |
| conversation | Who each enquiry and reservation belongs to | `EnquiryCreatedIE`, `ReservationRequestedIE` |
| feedback | Each reservation's customer, provider, named team and when it became Completed | `ReservationRequestedIE`, `TeamNamedIE`, `TeamMemberDroppedIE`, `ReservationCompletedIE` |
| feedback | The rating window in force when each reservation was requested, and the minimum ratings to show now | `HomaPolicyChangedIE`, `ReservationRequestedIE` |

**A live answer.** "Is this provider free?" can't wait for a projection. reservation and discovery each declare an out port, `ProviderAvailability`; availability offers a query usecase, `AvailabilityChecker`. Joining the two is not this plan's work.

## Usecases per bounded context

Each table lists the **in ports** (a usecase or a handler), the **spec section** it comes from, what it **orchestrates** in its own domain, and what it **publishes**. After each table come the **out ports** and the **usecase exceptions**.

### catalogue

Spec: *Jobs and tools*, *Homepage*, *Settings: catalogue and curation*, *Where Homa Operates*.

| Usecase | Spec | Orchestrates | Publishes |
|---|---|---|---|
| `HomaAddJob`, `HomaRenameJob`, `HomaRegroupJob` | Homa sets the job list and its groups; it can rename a job or move it to another group | `JobList` | `JobAddedIE`, `JobRenamedIE`, `JobRegroupedIE` |
| `HomaRetireJob` | Homa can retire a job, with a reason; a retired job comes off the featured searches | `JobList`, then `Showcase` | `JobRetiredIE` |
| `HomaAddTool`, `HomaRenameTool`, `HomaRetireTool` | The tool list, the same way; retiring needs a reason | `ToolList` | `ToolAddedIE`, `ToolRenamedIE`, `ToolRetiredIE` |
| `HomaFeatureSearch`, `HomaRenameFeaturedSearch`, `HomaMoveFeaturedSearch`, `HomaRemoveFeaturedSearch` | Feature, rename, move and remove searches, each with its own place | `Showcase` | — |
| `HomaPublishServiceArea` | Homa launches in three cities | `ServiceArea` | `ServiceAreaPublishedIE` |
| `HomaViewJobList`, `HomaViewToolList`, `HomaViewShowcase` | The founders see everything, retired and hidden included | (query) | — |
| `ProviderViewJobList`, `ProviderViewToolList`, `ProviderViewServiceArea` | Providers choose jobs, tools and cities from Homa's lists | (query) | — |
| `CustomerViewShowcase`, `CustomerViewJobList` | The homepage showcase in Homa's order; picking jobs for a search | (query) | — |

- **Out ports:** `JobListRepository`, `ToolListRepository`, `ShowcaseRepository`, `ServiceAreaRepository`, `CatalogueIEPublisher`, `AdminAuditLog`.
- **Usecase exceptions:** `CatalogueNotSetUpException`, `ChangedMeanwhileException`, `AdminReasonRequiredException`.

### policy

Spec: *Homa-Set Values*, *Settings: Homa-set values*.

| Usecase | Spec | Orchestrates | Publishes |
|---|---|---|---|
| `HomaLaunchPolicy` | The values Homa sets, the same for every reservation and provider | `HomaPolicy` (version 1, once) | `HomaPolicyChangedIE` |
| `HomaChangePolicyValue` | Change the values one at a time and with a reason; a change applies to new reservations only | `HomaPolicy` | `HomaPolicyChangedIE` |
| `HomaViewPolicy` | View the values | (query) | — |

- The console names each value, such as `SERVICE_FEE`, and writes it by kind: a percentage (`15%`), a period (`PT2H`), a count (`300`) or a range of hours (`1-6`). The usecase only translates; the domain checks each value and the rules across values.
- **Out ports:** `HomaPolicyRepository`, `PolicyIEPublisher`, `AdminAuditLog`.
- **Usecase exceptions:** `PolicyNotLaunchedException`, `PolicyAlreadyLaunchedException`, `UnknownPolicyValueException`, `ChangedMeanwhileException`.

### account

Spec: *Account*, *Verification Policy*, *Onboarding*, *Teams*, *Providers* (admin).

| Usecase | Spec | Orchestrates | Publishes |
|---|---|---|---|
| `RegistrantStartPhoneVerification`, `RegistrantConfirmPhone` | Register with a phone number, verified by an SMS code | `PhoneVerification` | — |
| `CustomerRegisterAccount` | Customers register with their phone number, name and address, confirmed with a pin | `CustomerAccount` | `CustomerRegisteredIE` |
| `CustomerRename`, `CustomerChangeAddress`, `CustomerChangeAccessNotes` | They can change their name and address at any time; the account keeps access notes | `CustomerAccount` | `CustomerRenamedIE`, `CustomerAddressChangedIE`, `CustomerAccessNotesChangedIE` |
| `ProviderRegisterAccount` | Register the business with a phone number | `ProviderAccount` | — |
| `ProviderStartKyb` | Complete KYB on the KYC provider's pages; re-verification the same way | (opens the KYC provider's pages) | — |
| `KycRecordBusinessResult` | Homa records each result and relies on it | `ProviderAccount` | By outcome (rule R1) |
| `ProviderAddPayoutAccount` | Add a payout account: a business bank account | (opens Stripe's onboarding) | — |
| `StripeRecordPayoutAccountReady` | Providers cannot go live until the payout account is added | `ProviderAccount` | `PayoutAccountReadyIE`, once |
| `ProviderStartAddingTeamMember` | Start: the role and a profile photo taken with the camera; only once KYB has passed | `MemberVerification` | `MemberVerificationStartedIE` |
| `KycRecordMemberVerificationResult` | Verify on the spot; the check fails if the photo doesn't match or the person is under 18 | `MemberVerification` | `MemberVerifiedIE`, or `MemberVerificationRejectedIE` with the reason |
| `ProviderConfirmTeamMember` | Confirm: the lawful-employment tick for this named person; they join only then | `MemberVerification`, `TeamMember` | `TeamMemberAddedIE` |
| `ProviderChangeTeamMemberRole` | Change a member between helper and supervisor at any time | `TeamMember` | `TeamMemberRoleChangedIE` |
| `ProviderRemoveTeamMember`, `HomaRemoveTeamMember` | The provider can remove a member at any time; Homa removes one not allowed to work, and records it | `TeamMember` | `TeamMemberRemovedIE` |
| `ProviderStartMemberReverification` | Run each re-verification on the spot with the person present | (opens the KYC provider's check) | — |
| `KycRecordTeamMemberResult` | A pass moves the verified-until date out; a failed attempt changes nothing | `TeamMember` | By outcome (rule R1) |
| `KycRecordBusinessLapse`, `KycRecordMemberLapse` | Missing a re-verification: the business or person lapses | `ProviderAccount`, `TeamMember` | `ProviderVerificationLapsedIE`, `TeamMemberLapsedIE` |
| `KycRecordBusinessReverificationRequired`, `KycRecordMemberReverificationRequired` | Ongoing monitoring: re-verification needed by a date | `ProviderAccount`, `TeamMember` | `ReverificationRequiredIE` |
| `KycRecordBusinessFlag`, `KycRecordMemberFlag` | A serious flag: the business or person lapses at once | `ProviderAccount`, `TeamMember` | `ProviderFlaggedIE`, `TeamMemberLapsedIE` |
| `HomaRequestBusinessRecheck`, `HomaRequestMemberRecheck` | Homa can ask for an early re-check, with a reason | (asks the KYC provider) | — |
| `CustomerViewAccount`, `ProviderViewAccount`, `ProviderViewTeam` | The account; when the business and each member are next due; who is waiting for verification or for the confirmation | (query) | — |
| `CustomerViewProviderTeam` | The provider's team: registered name, role, photo, last verification date; only added members | (query) | — |

- **Out ports:** `CustomerAccountRepository`, `ProviderAccountRepository`, `PhoneVerificationRepository`, `MemberVerificationRepository`, `TeamMemberRepository`, `AccountIEPublisher`, `VerificationCodes`, `SmsSender`, `Geocoder`, `IdentityVerificationService`, `PayoutAccountService`, `AdminAuditLog`.
- **Usecase exceptions:** `PhoneVerificationNotFoundException`, `PhoneAlreadyRegisteredException` (a phone number has at most one customer account and one business account), `CustomerNotFoundException`, `ProviderNotFoundException`, `MemberVerificationNotFoundException`, `TeamMemberNotFoundException`, `NotYourTeamMemberException`, `AddressNotFoundException`, `AdminReasonRequiredException`, `ChangedMeanwhileException`.

### offering

Spec: *Pricing*, *Teams, helpers and supervisors*, *Acceptance Policy*, *Damage Policy*, *Onboarding*, *After going live: Offerings*.

| Usecase | Spec | Orchestrates | Publishes |
|---|---|---|---|
| `ProviderDraftListing` | Set up the profile | `ProviderListing` | — |
| `ProviderSetProfile`, `ProviderOfferJobs`, `ProviderBringTools`, `ProviderSetPricing`, `ProviderSetHours`, `ProviderSetHelpers`, `ProviderSetSupervisorRule`, `ProviderClearSupervisorRule`, `ProviderCoverCities`, `ProviderChooseCancellationTier`, `ProviderChooseAcceptancePolicy` | Each provider sets these; a change applies to new requests only | `ProviderListing` | `ListingChangedIE`, when the listing is live |
| `ProviderDeclareInsurance`, `ProviderClearInsurance` | A provider can declare liability insurance | `ProviderListing` | `ProviderInsuranceDeclaredIE`, `ProviderInsuranceClearedIE` |
| `ProviderGoLive` | Go live once everything is set up; Homa lists everything still missing | `ProviderListing` | `ListingWentLiveIE` |
| `ProviderViewListing` | Manage offerings | (query) | — |

| Handler | Spec | Orchestrates | Publishes |
|---|---|---|---|
| `JobRetiredIE`, `ToolRetiredIE` | A retired job or tool comes off every provider's listing, and the provider is told | `ProviderListing` on each listing (rule R8) | `ListingChangedIE` or `ListingWentOfflineIE`, and the notice for the provider |
| `HomaPolicyChangedIE` | When Homa narrows the duration range, each provider's hours shrink to fit | `ProviderListing` on each listing (rule R8) | The same pattern |
| The five team events | If the team's helpers drop below the maximum, the maximum shrinks; with no helper left the listing stops being live | `ProviderListing` (rule R4) | `ListingChangedIE`, or `ListingWentOfflineIE` |
| `ProviderVerificationLapsedIE`, `ProviderFlaggedIE` | A lapsed business has its listing paused | `ProviderListing` | `ListingPausedIE` |
| `ProviderReverifiedIE` | Until it passes | `ProviderListing` | `ListingResumedIE` |
| The catalogue, service area and business readiness events | — | Update the projections | — |

- **Out ports:** `ProviderListingRepository`, the projections, `OfferingIEPublisher`.
- **Usecase exceptions:** `ListingNotFoundException`, `ListingAlreadyDraftedException`, `ChangedMeanwhileException`.

### availability

Spec: *Availability*, *After going live: Calendar*.

| Usecase | Spec | Orchestrates | Publishes |
|---|---|---|---|
| `ProviderSetAvailabilityWindows` | Providers manage their availability in the app | `ProviderCalendar` | — |
| `ProviderSetBufferBetweenJobs` | A provider can set a buffer between jobs | `ProviderCalendar` | — |
| `ProviderConnectCalendar` | Sync with Google Calendar or Apple Calendar | (opens the calendar's consent) | — |
| `CalendarSyncBusyBlocks` | Results reflect when a provider can actually work | `ProviderCalendar` | — |
| `AvailabilityChecker` | Bookable times come from each provider's own calendar; a close match's start is always bookable | (query: is a slot free; the nearest free start) | — |
| `ProviderViewCalendar` | Manage availability | (query) | — |

Handlers: `ReservationAcceptedIE` holds the slot; `ReservationCancelledIE` and `ReservationCompletedIE` release it. availability publishes no events.

- **Out ports:** `ProviderCalendarRepository`, `CalendarService`.
- **Usecase exceptions:** `CalendarNotFoundException`, `CalendarNotConnectedException`.

### reservation

Spec: *How Homa Works*, *Reservation Lifecycle*, *Request*, *Acceptance*, *Check-in*, *Completion*, *Dispute*, *Cancellation* and *Verification* policies, *Provider Cancellation Fines*, *Chat* (enquiries).

**The customer**

| Usecase | Spec | Orchestrates | Publishes |
|---|---|---|---|
| `CustomerCreateEnquiry` | Tap **Chat**: this creates an enquiry, at the customer's current address | `Enquiry` | `EnquiryCreatedIE` |
| `CustomerBookAgain` | From any past enquiry or reservation, at the current address and a new date | `Enquiry` | `EnquiryCreatedIE` |
| `CustomerViewReviewPage` | Review: the price, and the exact cancellation amount and time for that booking | Pricing and cancellation terms | — |
| `CustomerRequestReservation` | Tap **Reserve**; Homa checks availability; the request is made at the customer's current address and keeps its pin; each new request copies the account's access notes | `Reservation`, `Enquiry` (rules R2, R3, R12, R14) | `EnquiryRequestedIE` when from an enquiry, then `ReservationRequestedIE` |
| `CustomerEditRequest` | The customer can edit a request until the provider accepts it | `Reservation` (rule R3) | `ReservationEditedIE` |
| `CustomerChangeReservationAccessNotes` | The customer can change a reservation's notes until the job is completed | `Reservation` | `AccessNotesChangedIE` |
| `CustomerCancelReservation` | Free before the provider responds; then by the tier; the no-show fee after check-in | `Reservation` | A request: `RequestCancelledIE`. A reserved booking: `ReservationCancelledIE`, `SettlementDecidedIE` |
| `CustomerChallengeNoShow` | The customer can report a wrongful no-show within the no-show report window | `Reservation` | `NoShowChallengedIE` |
| `CustomerViewReservations`, `CustomerViewReservation`, `CustomerViewEnquiries` | Who is coming, the check-in time, each person's face check result and time, the cancellation terms | (query) | — |

**The provider**

| Usecase | Spec | Orchestrates | Publishes |
|---|---|---|---|
| `ProviderViewRequests` | The provider sees a request only once it is paid | (query; rule R2) | — |
| `ProviderAcceptRequest` | Accept by the response deadline; other requests whose time is now taken are declined at once | `Reservation` (rules R2, R3, R5) | `ReservationAcceptedIE`, then `RequestDeclinedForTakenTimeIE` for each |
| `ProviderDeclineRequest` | Decline; not for an instant-book request | `Reservation` | `ReservationDeclinedIE` |
| `ProviderCancelReservation` | The customer is refunded in full and the provider may pay a fine | `Reservation` | `ReservationCancelledIE`, `SettlementDecidedIE`, and `FineChargedIE` when fined |
| `ProviderNameTeam` | Name the helpers, and a supervisor when one is due, before the booked start | `Reservation`, against the team as it is now | `TeamNamedIE` |
| `ProviderCheckIn` | Check-in is location-based, within the check-in radius | `Reservation`, with the distance from the device to the reservation's pin, from the `Geoservice` port (rule R14) | `ProviderCheckedInIE` |
| `ProviderStartFaceCheck` | Every named person takes a live selfie | (opens the KYC provider's face check) | — |
| `KycRecordFaceCheck` | Check-in counts once the first named person passes; a person who fails cannot work | `Reservation` | `FaceCheckRecordedIE`, and `CheckInCompletedIE` on the first pass |
| `KycRecordFaceCheckUnavailable` | If the face check service is unavailable, check-in goes ahead on location only | `Reservation` | `CheckInCompletedIE` |
| `ProviderReportCustomerNoShow` | After the customer no-show wait, up to the booked end | `Reservation` (rule R6) | `ReservationCancelledIE` |
| `ProviderSubmitCompletion` | The provider marks the job completed, with photo or video evidence | `Reservation` | `CompletionSubmittedIE` |
| `ProviderViewReservations`, `ProviderViewReservation` | The provider sees the access notes from acceptance | (query) | — |

**Either party, and the founders**

| Usecase | Spec | Orchestrates | Publishes |
|---|---|---|---|
| `PartyFileReport` | Each can file one report per reservation, of one of three types | `Reservation` | `ReportFiledIE` |
| `HomaCancelFlaggedReservation` | The founders cancel it with a full refund, no fine and no fee | `Reservation` (rule R7) | `ReservationCancelledIE`, `SettlementDecidedIE` |
| `HomaRestoreFlaggedReservation` | Or restore it, if the flag is cleared and the booked start hasn't come | `Reservation` (rule R7) | `ReservationRestoredIE` |

**Homa, at a set time.** Each is an ordinary usecase with no input that works through everything due now; what calls it on time is infra.

| Usecase | Spec | Orchestrates | Publishes |
|---|---|---|---|
| `HomaExpireRequests` | A request expires at the request expiry time, never later than the response deadline | `Reservation` | `ReservationExpiredIE` |
| `HomaCloseEnquiries` | An enquiry never requested closes when its date passes | `Enquiry` | `EnquiryClosedIE`, once |
| `HomaRemindTeams` | Homa reminds the provider at the team reminder time, and again when a named person comes off | `Reservation` | `TeamReminderDueIE`, once each time the team is not complete |
| `HomaMarkProviderNoShows` | No check-in by the end of the check-in period is a provider no-show | `Reservation` | `ReservationCancelledIE`, `SettlementDecidedIE`, `FineChargedIE` |
| `HomaCancelUndecidedFlaggedReservations` | If nobody has decided by the booked start, Homa cancels with a full refund | `Reservation` | `ReservationCancelledIE`, `SettlementDecidedIE` |
| `HomaReleaseNoShowFees` | The no-show fee is released when the report window ends without a report | The kept settlement (rule R6) | `SettlementDecidedIE` |
| `HomaRemindCompletions` | Homa reminds the provider at the completion reminder time | `Reservation` | `CompletionReminderDueIE`, once |
| `HomaMissCompletionDeadlines` | No completion by the completion deadline: Disputed | `Reservation` | `CompletionDeadlineMissedIE` |
| `HomaCloseDisputeWindows` | The dispute window ends with no report: Completed, and the payment is released | `Reservation` | `ReservationCompletedIE`, `SettlementDecidedIE` |

**Handlers**

| Handler | Spec | Orchestrates | Publishes |
|---|---|---|---|
| `PaymentSecuredIE` | The provider can accept only once paid; Instant book accepts the moment the payment is secured, and never a time that isn't free | `Reservation` (rules R2, R3, R5) | For Instant book: `ReservationAcceptedIE`, or `RequestDeclinedForTakenTimeIE` |
| `PaymentFailedIE` | A payment that fails: the request ends straight away | `Reservation` (rule R2) | — |
| `CaseDecidedIE` | Homa resolved the review | `Reservation` | `ReservationCompletedIE`, or `ReservationCancelledIE` for a challenged no-show; `SettlementDecidedIE` |
| `ProviderFlaggedIE` | Every accepted reservation whose completion hasn't been submitted moves to Disputed at once | `Reservation` on each (rule R7) | `ReservationFlaggedIE` each |
| `TeamMemberLapsedIE`, `TeamMemberRemovedIE` | A lapsed or removed person comes off every upcoming job at once | `Reservation` on each (rule R13) | `TeamMemberDroppedIE` or `TeamMemberStoppedIE` |
| The listing, policy, verification, team and customer events | — | Update the projections | — |

- **Out ports:** `ReservationRepository`, `EnquiryRepository`, the projections, `NoShowSettlements` (rule R6), `CheckInLocations` (rule R14), `ReservationIEPublisher`, `ProviderAvailability`, `Geoservice`, `IdentityVerificationService`, `EvidenceStore`, `AdminAuditLog`.
- **Usecase exceptions:** `ReservationNotFoundException`, `EnquiryNotFoundException`, `NotYourReservationException`, `ListingNotAvailableException` (not live any more), `ListingPausedException`, `ProviderNotAvailableException`, `ProviderStillFlaggedException`, `AdminReasonRequiredException`, `ChangedMeanwhileException`.

### payment

Spec: *Payment Protection*, *Request Policy* (payment on edit), *Provider Cancellation Fines*, *Verification Policy* (held payouts), *After going live: Income*.

| Handler | Spec | Orchestrates | Publishes |
|---|---|---|---|
| `ReservationRequestedIE` | A card is authorised when submitting the request; FPX is paid in full | Starts the payment through `PaymentGateway`, and keeps it as pending (rule R2) | — |
| `ReservationEditedIE` | A new authorisation for the new total replaces the old one; FPX pays any increase, and any decrease is refunded | `Payment` | `PaymentSecuredIE` with the new amount, or `PaymentFailedIE` |
| `ReservationAcceptedIE` | A card is charged only when the reservation is accepted | `Payment` | `PaymentCapturedIE` |
| `ReservationDeclinedIE`, `ReservationExpiredIE`, `RequestCancelledIE`, `RequestDeclinedForTakenTimeIE` | A card authorisation is released, or an upfront payment is refunded in full | `Payment` | `PaymentRefundedIE` for an FPX refund |
| `SettlementDecidedIE` | Homa holds every charged payment until it is released or refunded; payouts per reservation; fines deducted from completed-job payouts within the cap, oldest first | `Payment`, `Payout`, `FineDeductionPolicy` (rule R7 for a flagged business) | `PaymentRefundedIE`, `PayoutCreatedIE`, `PayoutHeldIE` |
| `FineChargedIE` | A provider who cancels late or does not show pays a fine | `Fine` | — |
| `ChargebackDecidedIE` | The amount is recovered from the provider's next payouts the same way as a fine | `Fine` | — |
| `ProviderFlaggedIE` | The business's payouts are held | `Payout` on each payable payout | `PayoutHeldIE` each |
| `ProviderReverifiedIE`, `HomaPolicyChangedIE` | — | Update the projections | — |

| Usecase | Spec | Orchestrates | Publishes |
|---|---|---|---|
| `StripeRecordCardAuthorised`, `StripeRecordFpxPaid` | Paid before the provider answers | `Payment` | `PaymentSecuredIE` |
| `StripeRecordPaymentFailed` | If the card is declined, or the FPX payment fails or is abandoned, the request ends | (no `Payment` exists; rule R2) | `PaymentFailedIE` |
| `StripeRecordChargeback` | A customer who disputes a card payment with their bank after the job opens a case for Homa | Finds the reservation's `Payment` | `ChargebackReceivedIE` |
| `StripeRecordRefund`, `StripeRecordPayoutPaid`, `StripeRecordPayoutFailed` | The app shows the refund status; the provider's payouts | Record the status | — |
| `HomaWaiveFine` | Homa can waive a fine after review, once, with a reason | `Fine` (rule A5) | `FineWaivedIE` |
| `HomaReleasePayout`, `HomaWithholdPayout` | The founders release held payouts or keep them back, with a reason | `Payout` | `PayoutHoldReleasedIE`, `PayoutWithheldIE` |
| `CustomerViewPaymentToComplete` | You pay by card or online banking (FPX) | (query: what the app needs to finish paying with Stripe) | — |
| `CustomerViewPayments` | The account shows payments and refunds with their status and expected timing; a receipt for each | (query) | — |
| `ProviderViewIncome` | Earnings, payouts with a statement each, and any fines being deducted | (query) | — |

- **Out ports:** `PaymentRepository`, `PendingPayments`, `PayoutRepository`, `FineRepository`, the projections, `PaymentIEPublisher`, `PaymentGateway`, `PayoutGateway`, `AdminAuditLog`.
- **Usecase exceptions:** `PaymentNotFoundException`, `PayoutNotFoundException`, `FineNotFoundException`, `AdminReasonRequiredException`, `ChangedMeanwhileException`.

### resolution

Spec: *Dispute Policy*, *Payment Protection* (chargebacks), *Homa Admin: Reports*.

| Usecase | Spec | Orchestrates | Publishes |
|---|---|---|---|
| `HomaViewCaseQueue` | One queue: safety, damage or loss first, the rest oldest first, each with its age against the 3-working-day target | (query) | — |
| `HomaViewCase` | The founders see the case and its evidence | (query) | — |
| `HomaDecideCase` | A release, a partial refund or a full refund, with a short reason and the founder who made it | `Case` | `CaseDecidedIE`; for a chargeback, `ChargebackDecidedIE` instead |
| `PartyViewCaseStatus` | The customer and the provider see the expected date, and the outcome | (query) | — |

| Handler | Spec | Orchestrates | Publishes |
|---|---|---|---|
| `ReportFiledIE`, `NoShowChallengedIE`, `CompletionDeadlineMissedIE`, `ChargebackReceivedIE` | Each opens a case; if the other party also reports, their report joins the same case; a safety report marks it | `Case` | — |
| `CompletionSubmittedIE` | Completion submitted while Disputed: the evidence goes to Homa's review | `Case`, when one is open | — |
| `ReservationFlaggedIE` | A case already open for a reservation the flag takes over is closed without a decision | `Case` | `CaseClosedForProviderFlagIE` |

- **Out ports:** `CaseRepository`, `ResolutionIEPublisher`, `AdminAuditLog`.
- **Usecase exceptions:** `CaseNotFoundException`, `NotYourCaseException`, `ChangedMeanwhileException`.

### conversation

Spec: *Chat*.

| Usecase | Spec | Orchestrates | Publishes |
|---|---|---|---|
| `CustomerStartChat` | Only the customer starts a chat, on their enquiry | `Conversation` | — |
| `PartyPostMessage` | Once a chat is open, both can write | `Conversation` | — |
| `PartyViewChat` | The chat stays open for the life of the reservation; an archived chat stays readable; every chat shows the Homa Protection reminder | (query) | — |
| `PartyViewInbox` | Chats are grouped by provider | (query) | — |
| `HomaViewChatForCase` | Chats are opened only for a case | (query; rule A4) | — |

Handlers: `EnquiryCreatedIE` (remember whose enquiry it is, so the customer's first message starts its chat); `EnquiryRequestedIE` (the same chat carries on); `ReservationRequestedIE` (requesting opens the reservation's chat with no message, unless it already has one; rule R12); `EnquiryClosedIE` (the chat is archived); the case-opening events (rule A4). conversation publishes no events.

- **Out ports:** `ConversationRepository`, the projections, `AdminAuditLog`.
- **Usecase exceptions:** `ChatNotFoundException`, `NotYourChatException`, `EnquiryNotFoundException`, `ChatAlreadyStartedException`, `NoCaseForReservationException`.

### discovery

Spec: *How Homa Works* (search and browse), *Provider page*, *Privacy Policy* (searches).

| Usecase | Spec | Orchestrates | Publishes |
|---|---|---|---|
| `CustomerSearchProviders` | Providers who match exactly, plus close matches; only providers who cover the city; a guest picks a city | `SearchCriteria`, a `ProviderCandidate` per listing with the nearest free start from `ProviderAvailability`, `MatchClassifier` | `SearchPerformedIE` (rule R10) |
| `CustomerViewProviderPage` | Jobs by group, tools brought and not brought, price and minimum hours, the cancellation policy, Instant book, Insured, jobs done and cancellations | (query) | — |

Handlers: the projections listed earlier.

- **Out ports:** the projections, `ProviderAvailability`, `DiscoveryIEPublisher`.
- **Usecase exceptions:** `ProviderNotFoundException`, `SearchLimitsNotKnownException`.

### feedback

Spec: *Rating Policy*, *Ratings*, *Homa Admin: Ratings*.

| Usecase | Spec | Orchestrates | Publishes |
|---|---|---|---|
| `CustomerRateReservation` | Only the customer, only a Completed reservation, once, within the rating window | `Rating`, then `RatingSummary` for the provider and each rated team member | `RatingSubmittedIE` |
| `HomaHideRating` | Homa can hide a rating that breaks the rules, with a recorded reason; it no longer counts | `Rating`, then `RatingSummary` | `RatingHiddenIE` |
| `CustomerViewProviderRatings` | The averages once they have the minimum ratings to show, the count with "New" until then, and written feedback, newest first | (query) | — |
| `CustomerViewTeamRatings` | Each team member's overall average | (query) | — |
| `HomaViewRatings` | Every rating | (query) | — |

Handlers: the projections listed earlier. Only `ReservationCompletedIE` makes a reservation rateable.

- **Out ports:** `RatingRepository`, `RatingSummaryRepository`, the projections, `FeedbackIEPublisher`, `AdminAuditLog`.
- **Usecase exceptions:** `ReservationNotKnownException`, `RatingNotFoundException`, `AdminReasonRequiredException`, `ChangedMeanwhileException`.

## Rules the usecases must keep

These are the spec's rules that no single domain object can keep, because they span contexts or time. Each needs a usecase test.

1. **R1. One event per verification outcome** (*Verification Policy*). The usecase publishes the event for the outcome the domain returns, and nothing when nothing changed.

   | Outcome | Adding a team member | A team member | A business |
   |---|---|---|---|
   | Verified | `MemberVerifiedIE`; the provider's confirmation then adds the person, with `TeamMemberAddedIE` | — | `ProviderVerifiedIE` |
   | Rejected | `MemberVerificationRejectedIE`, with the reason | — | `ProviderVerificationRejectedIE` |
   | Re-verified | — | `TeamMemberReverifiedIE` | `ProviderReverifiedIE` |
   | Re-verification failed | — | `RecheckFailedIE` | `RecheckFailedIE` |
   | Unchanged | none | none | none |

2. **R2. A request is paid before the provider answers, and never waits unpaid** (*Payment Protection*).
   - On `ReservationRequestedIE`, payment starts the payment with Stripe and keeps it as pending. The customer finishes paying in the app, and Stripe reports the result to a `Stripe…` usecase.
   - payment publishes `PaymentSecuredIE` with the amount; reservation records it, and `ProviderViewRequests` lists only requests whose payment is secured.
   - After an edit that raises the total, the request waits for the new amount.
   - A failed or abandoned payment publishes `PaymentFailedIE`; reservation ends the request and publishes nothing more. A payment held from before an edit is released or refunded first. A request that ends while still unpaid cancels the pending payment.
   - A request ends with exactly one event, and payment treats all four the same way: `ReservationDeclinedIE`, `ReservationExpiredIE`, `RequestCancelledIE`, `RequestDeclinedForTakenTimeIE`.
3. **R3. Availability is checked at request, at edit and at acceptance** (*Acceptance Policy*, *Chat*).
   - A time the provider isn't free is refused (`ProviderNotAvailableException`).
   - Instant book checks again when the payment is secured; if the time is no longer free, the request ends and costs the customer nothing.
   - A paused listing can't accept (`ListingPausedException`), including a request made before the pause. That request stays Requested and expires normally unless the business passes in time.
   - The business's verified-until date, as it is now, is passed to the domain at request, edit and acceptance.
4. **R4. The team is only people on the team, by role** (*Teams*). A business's team is built from members who were added and are not lapsed or removed. A lapsed member who passes rejoins. A role change moves the person between helpers and supervisors.
5. **R5. Accepting a request declines the requests it makes impossible** (*Request Policy*). After an acceptance, by the provider or by Instant book, reservation asks `ProviderAvailability` about each of that provider's other open requests and declines each one that is no longer free. It does this at once, in the same usecase, after publishing `ReservationAcceptedIE`; the accepted booking's hold is in place by then, which infra guarantees.
6. **R6. A customer no-show fee is sent to payment only when its report window ends unchallenged** (*Payment Protection*).
   - `ProviderReportCustomerNoShow` publishes `ReservationCancelledIE` and keeps the settlement.
   - `HomaReleaseNoShowFees` publishes `SettlementDecidedIE` once the window has ended with no challenge.
   - If the customer challenges, the kept settlement is dropped; Homa's decision gives the only one sent.
   - A customer who cancels after check-in is different: that fee is released straight away.
7. **R7. A seriously flagged business** (*Verification Policy*). On `ProviderFlaggedIE`:
   - reservation moves every accepted reservation with no completion to Disputed, including one already disputed over a report.
   - resolution closes the open case of each reservation the flag took over.
   - offering pauses the listing. payment holds the payable payouts, and every payout created while the business is flagged.
   - `HomaRestoreFlaggedReservation` refuses while the business is still flagged (`ProviderStillFlaggedException`).
   - A payout already held stays held after the flag is cleared, until the founders decide.
8. **R8. Homa's changes reach the listings, and the provider is told** (*Jobs and tools*, *Homa-Set Values*).
   - A listing that was live and still is publishes `ListingChangedIE` plus the notice: `JobWithdrawnFromListingIE`, `ToolWithdrawnFromListingIE` or `ListingHoursNarrowedIE`.
   - A live listing that goes offline publishes `ListingWentOfflineIE` plus `ListingWentOfflineForRetiredJobIE` or `ListingWentOfflineForHoursIE`.
   - A draft, or a listing the change doesn't touch, publishes nothing.
   - Requests and reservations already made are never touched by a listing going offline or pausing.
9. **R9. Homa never marks anyone verified by itself** (*Verification Policy*). Only a result reported by the KYC provider, through a `Kyc…` usecase, changes a verification. `IdentityVerificationService` only starts checks.
10. **R10. Every search is recorded, without the customer** (*Privacy Policy*). `SearchPerformedIE` carries what was searched for and how many providers matched, and nothing about who searched. A paused listing is left out of search.
11. **R11. The customer sees who can work** (*Check-in Policy*). `CustomerViewReservation` shows each named person's face check result and time, and marks anyone who cannot work.
12. **R12. Requesting from an enquiry keeps one chat** (*Chat*). `EnquiryRequestedIE` is published before `ReservationRequestedIE`, so conversation carries the enquiry's chat on and opens no second one.
13. **R13. A person who lapses or is removed comes off every upcoming job** (*Verification Policy*). reservation applies it to every reservation that names them, and publishes `TeamMemberDroppedIE` (with whether the team is still complete) or `TeamMemberStoppedIE`.
14. **R14. A reservation keeps the address it was made with** (*Account*, *Check-in Policy*). A request, an enquiry and book again take the customer's current address from reservation's projection. reservation keeps the pin of that address for each reservation, behind `CheckInLocations`, and `ProviderCheckIn` measures from it. A later change of address touches no request or reservation already made.
15. **R15. A reservation keeps the Homa-set values it was requested under** (*Homa-Set Values*). payment deducts fines within the fine deduction cap that was in force when the payout's reservation was requested, and feedback applies that reservation's rating window. The minimum ratings to show is about showing, not about a reservation, so the current value applies.
16. **R16. The provider page is three queries** (*Provider page*). The app puts it together from `CustomerViewProviderPage` (discovery), `CustomerViewProviderTeam` (account) and `CustomerViewProviderRatings` (feedback), because no context imports another. "Who is coming" works the same way, from reservation, account and feedback.

**Founders' actions** (*Homa Admin*):

1. **A1. Every decision records who and why.** A reason is required for a decision about a person, money or providers' listings.
2. **A2. A flagged reservation is only cancelled or restored.** No usecase lets it go ahead while flagged.
3. **A3. No editing.** No usecase edits a rating, a chat, evidence, or a check-in or face check record.
4. **A4. Chats and evidence are opened only for a case**, and each opening is recorded.
5. **A5. A fine waiver** takes a reason, is given once, covers what is left, and leaves the cancellation count as it is.
6. **A6. Money moves only through Homa's rules.** The founders' usecases decide; only payment's usecases instruct the payment provider.

## Build order

The discipline's order is domain, then usecase, then infra (§8). Within this phase, contexts are built so each one's handlers have events to listen to. A context passes its gates before the next starts.

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

## Testing and quality gates

From the discipline, §9, for the `usecase` layer:

| Gate | Threshold | How |
|---|---|---|
| Unit tests | Robust and intent-focused | Framework-free; a fixed clock; in-memory fakes of the out ports, kept in test-support files per context |
| CRAP index | Below 6 | `tools/quality_gates.py` |
| Mutation testing | At least 80%, with the ratchet and test strength reported | PIT; each context's `usecase` package is added to `pom.xml` and `quality/ratchet.json` |
| Dependency rule | The usecase depends only on its own domain, `kernel` and `ie`; `ie` depends only on `kernel` | A `<Context>UsecaseArchitectureTest` per context, and the existing `IntegrationEventArchitectureTest` |
| Event contracts | Every event, new ones included | The existing event contract test |

- **A usecase test proves the orchestration:** what is loaded, what is saved, which events are published and in which order. It does not test the domain's rules again; one refused case per usecase shows that a refusal saves and publishes nothing.
- **Behaviour, in the spec's words:** tests are named for what happens, such as `theProviderSeesARequestOnlyOnceItIsPaid`.
- **Each rule above** has a test named for it.
- **The architecture test also checks** that in ports are interfaces, DTOs carry no domain object or event, every usecase implements exactly one in port, and usecase exceptions live in `usecase/exception`.

## Outside this plan

Listed so nothing is lost. None of it is planned here.

- **Infra:** repositories and storage, controllers, the webhooks of the KYC provider and Stripe, the scheduler that calls the `Homa…` timed usecases, security, event delivery and transactions, the adapters behind `SmsSender`, `Geocoder`, `Geoservice`, `IdentityVerificationService`, `PaymentGateway`, `PayoutGateway`, `CalendarService` and `EvidenceStore`, evidence retention, and `app/`, which also joins `ProviderAvailability` to `AvailabilityChecker`.
- **Modules without a bounded context:** sending push notifications, the admin console's tracking views, and recording data requests.
- **The domain:** not changed by this plan. Where a usecase needs something it doesn't offer today, it is raised as an open question below, for a separate decision.

## Questions answered from the spec

These were open in the first draft. The spec answers each one; the answer is written into the tables and rules above.

| # | Question | What the spec says | Answer |
|---|---|---|---|
| 1 | When are clashing requests declined after an acceptance? | "Any other open request of theirs that can no longer be accepted ... is declined at once" (*Request Policy*) | In the accepting usecase, straight after the acceptance (rule R5) |
| 2 | How does the customer pay? | "Card: authorised when submitting the request ... FPX: paid in full when submitting the request"; "the provider sees a request ... only once the card is authorised or the FPX payment is paid"; "Homa does not store card or bank details" (*Payment Protection*, *Privacy Policy*) | payment starts the payment when the request is made; the customer finishes with Stripe; Stripe reports the result (rule R2) |
| 3 | Who puts the provider page together? | The spec lists what the page shows, across the listing, the team and the ratings (*Provider page*) | Three queries, one per context that owns the data (rule R16) |
| 4 | How does a reminder reach the provider? | "Homa reminds the provider at the team reminder time"; "Homa reminds them to submit it" (*Check-in Policy*, *Completion Policy*) | Two new events, `TeamReminderDueIE` and `CompletionReminderDueIE` |
| 5 | Where does a request's address come from? | "A new address is used for new searches and requests; requests and reservations already made keep the address they were made with"; "Homa uses the address ... to locate check-in" (*Account*) | The address and its pin travel on `CustomerRegisteredIE` and the new `CustomerAddressChangedIE`; each reservation keeps its pin (rule R14) |
| 6 | Who learns of a chargeback? | "A customer who disputes a card payment with their bank after the job opens a case for Homa" (*Payment Protection*) | Stripe reports it to payment, which knows the reservation's payment; the new `ChargebackReceivedIE` opens the case in resolution |
| 8 | Which Homa-set values apply later? | "Each reservation keeps the values that were in force when it was requested"; "each deduction is at most the fine deduction cap of that payout" (*Homa-Set Values*, *Provider Cancellation Fines*) | The values of the reservation's request (rule R15) |

## Open questions

The spec does not answer these two. Each needs the product owner's decision before the context it touches is built.

1. **Q7. A named person whose role changes** (reservation). The spec lets a provider change a member between helper and supervisor "at any time", and says "the usual rules still apply". It doesn't say what happens to a job that already names the person in their old role.
   *Recommended:* before the booked start, they come off that job's team, as a lapsed or removed person does, so the team is not complete until the provider names it again. From the booked start, nothing changes. This follows the spec's own rule that the named helpers match the number booked, but it needs a line in the spec, and the domain decides whether it is a new rule there.
2. **Q9. What a working day is** (resolution). "Homa aims to decide every case within 3 working days of it opening. The customer and the provider see the expected date." The spec doesn't say whether public holidays count, and they differ between Selangor and Putrajaya.
   *Recommended:* Monday to Friday, with no public holidays in the MVP. Working out the date is a rule, so it belongs in the domain; until that is decided, `HomaViewCaseQueue` and `PartyViewCaseStatus` show when the case opened.
