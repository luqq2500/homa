# Homa Development Plan: Domain and Usecase

This plan turns `homa-product-description.md` (the spec) into bounded contexts, and orders their development under `backend_module_discipline.md` (the discipline). It covers the `kernel/`, every bounded context's `domain` and `usecase` layers, and the `ie/` contracts between them. Infra and `app/` come after, and are only touched on here where a usecase port has to be shaped for them.

When this plan and the spec disagree, the spec wins (discipline, Section 8). Where the spec leaves something open, this plan does not decide it: it lists it under **Open questions for the product owner**, and the module that needs it waits for the answer.

---

## 1. How the work is ordered

| Phase | What is built | Done when |
|---|---|---|
| 0. Foundations | The Maven build, `kernel/`, the quality gates | `kernel/` passes every gate; ArchUnit fails a planted wrong import |
| 1. Domain | Every bounded context's `domain`, upstream first (Section 4) | Each context passes the domain gates; every rule it owns is traced to the spec and tested |
| 2. Integration events | `ie/port` and `ie/events`, from the catalogue in Section 6 | Every event in the catalogue exists, with its producer and consumers named |
| 3. Usecase | Every bounded context's `usecase`, in the same order | Each context passes the usecase gates; every actor's act and every IE reaction in Section 5 has a usecase |
| 4. Infra and `app/` | Not in this plan | Planned once Phase 3 has fixed the ports |

**Why all domains before any usecase.** The reservation lifecycle touches almost every other context: the offering's terms, the provider's verification and team, availability, Homa-set values and money. Building every domain first settles the ubiquitous language and the rules before any port or event is fixed, so the ports are written once. It is the discipline's own order (domain first, details decided late), applied across the whole system.

**Answer the open questions first.** Each context lists the open questions it needs. A context starts only when its questions are answered and the spec is updated.

---

## 2. Phase 0: Foundations

### Build layout

One Maven project with one source tree. Each shared module and each bounded context is a top-level package, and its tests sit in the same package under `src/test/java`. ArchUnit enforces both context isolation and the layer direction.

```text
pom.xml                      Java 21, JUnit 5, AssertJ, ArchUnit, JaCoCo, PIT
src/main/java/
  kernel/                    depends on nothing
  ie/                        depends on kernel
  policy/  catalogue/  account/  provider/  offering/  availability/
  reservation/  payment/  resolution/  conversation/  feedback/  discovery/  notification/
                             each depends on kernel and ie only
  app/                       later (Phase 4): the only package that depends on every context
src/test/java/
  kernel/  ie/  policy/  ...  the same packages, one test class per feature, plus a
                             test-support package per context for builders and fixtures
```

Inside each context: `<context>.domain`, `<context>.usecase`, and later `<context>.infra`, laid out as in the discipline's blueprint (Section 3). For example `src/main/java/reservation/domain/Reservation.java` and `src/test/java/reservation/domain/RequestExpiryTest.java`.

### Quality gates (discipline, Section 9)

| Gate | How |
|---|---|
| Dependency rule | ArchUnit test per context: `domain` imports only itself, `kernel` and `java..`; `usecase` imports only its own `domain`, `kernel`, `ie` and `java..`; no framework type in either; no context imports another context's package; `kernel` imports only `java..`, and `ie` only `kernel` and `java..` |
| Unit tests | JUnit 5, named in the ubiquitous language; one test-support package per context for builders and fixtures |
| CRAP | Below 6 per method, from JaCoCo coverage and cyclomatic complexity |
| Mutation | PIT per layer: at least 90% for `kernel` and each `domain`, at least 80% for each `usecase`; report test strength too |
| Ratchet | `quality/ratchet.json` keeps the last passing score per layer, and a drop fails the build |

The gate script and ratchet removed in `8ef06db` can be restored from history and adapted to the module-per-context layout.

### `kernel/`

Only types that pass the admission rule (at least two domains, a value object, no context's rule, Java standard library only):

| Type | Why it qualifies | Rules it carries |
|---|---|---|
| `Money` | Every money rule in reservation, payment, offering, resolution | Ringgit in whole sen; adding, subtracting, comparing; a percentage of an amount rounded half up to the nearest sen, and one rounded down for "rounded in the provider's favour" (the fine deduction cap) |
| `Percentage` | Platform fee, tiers, fines, no-show fee, deduction cap | A value from 0 to 100% |
| `TimeSlot` | A start and an end, used by availability, reservation and discovery | End after start; overlaps; touching end-to-start does not overlap (spec: "a booking that ends exactly when a slot starts does not block that slot") |
| `City` | Account, offering, discovery, reservation | The three cities: Salak Tinggi, Cyberjaya, Putrajaya (see open question Q1) |

Anything about tiers, fees, fines or the platform fee stays in the context that owns it, never in `Money`.

---

## 3. Bounded contexts

Each context names what it owns, its main domain objects, the spec sections it implements, and what it keeps from other contexts. A context never imports another; it keeps **its own copy** of the facts it needs, as domain objects named in its own language, updated only by its IE handlers (for example, reservation's `ProviderStanding` holds the business's verified-until date and whether it is seriously flagged).

### 3.1 `policy`: Homa-set values

- **Owns:** every value in the spec's Homa-Set Values table, and their versions. A change is one value at a time, with a reason and the admin who made it; it applies to new reservations only.
- **Domain:** `HomaSetValues` (one version: every value), `HomaSetValuesVersion`, `CancellationTier` (Flexible, Standard, Firm with their percentages and the fee window).
- **Rules it enforces when a value changes:**
  - Flexible always stays free.
  - No cancellation fee is higher than the customer no-show fee (a tier's percentage of the hours fee never exceeds the no-show fee's).
  - The provider no-show fine is never below the provider late cancellation fine.
  - The response deadline is always shorter than the minimum notice.
- **Spec:** Homa-Set Values; Cancellation Policy; Provider Cancellation Fines; Homa Admin, Settings.

### 3.2 `catalogue`: job list, tool list and showcase

- **Owns:** the job list and its groups, the tool list, and the homepage showcase of featured searches in Homa's order.
- **Domain:** `Job`, `JobGroup`, `Tool`, `FeaturedSearch` (jobs, duration, helpers), `Showcase`.
- **Rules:** rename a job or tool, move a job to another group; retire a job or tool with a reason, never deleting it and never reusing its code; a retired job comes off every featured search, and a featured search left with no job is no longer shown; a featured search holds only jobs providers can offer.
- **Spec:** Jobs and tools; Homepage; Homa Admin, Settings (Catalogue and curation).

### 3.3 `account`: customers, businesses and signing in

- **Owns:** customer accounts (name, address with map pin and city, access notes up to 500 characters, chosen language), business accounts (signed in to by the owner or director), and the ways into each account (phone number, Google, Apple), plus personal data requests.
- **Domain:** `CustomerAccount`, `BusinessAccount`, `SignInMethod`, `PhoneNumber` (a Malaysian mobile number), `Address`, `AccessNotes`, `DataRequest`.
- **Rules:** every account has a verified phone number, which is always a way in and can't be removed; a phone number, a Google account or an Apple account opens at most one customer account and one business account; Google or Apple sign-in can be added and removed; a new address is used for new searches and requests only.
- **Not here:** sending and checking SMS codes is the OTP provider's (see Q2); Google and Apple do their own sign-in. The account usecases take their verified results.
- **Spec:** Signing up and signing in; Account; Privacy Policy (user rights, data requests).

### 3.4 `provider`: the business, its verification and its team

- **Owns:** the business's KYB result (verified, verified-until, SSM number, registered business name), lapses, serious flags and their clearing, early re-checks; and the team: adding a team member (start, verify on the spot, confirm lawful employment), roles, removal, re-verification, lapses.
- **Domain:** `Business`, `TeamMember`, `TeamRole` (helper, supervisor), `TeamMemberAddition` (waiting for verification, waiting for your confirmation, joined, or ended with a reason), `LawfulEmploymentConfirmation` (person, time, terms version), `VerificationResult`.
- **Rules:**
  - Team members can be added only once the KYB has passed and while it hasn't lapsed.
  - A team member joins only after the KYC result passes, the profile photo matches, the person is 18 or older, and the provider confirms lawful employment; otherwise nothing is added and the provider is told why.
  - The registered name comes only from the KYC result.
  - Changing a role needs no new verification.
  - Re-verification: a pass moves verified-until out; a failed attempt changes nothing; "re-verification needed by a date" moves verified-until to that date if sooner; a missed one lapses the business or person.
  - A serious flag lapses at once.
  - A lapsed team member leaves the team until they pass.
  - Homa can ask for an early re-check, with a reason.
- **Spec:** Safety principle; Teams, helpers and supervisors; Verification Policy; Provider App (Onboarding, Teams, Verification); Homa Admin, Providers.

### 3.5 `offering`: what a provider offers

- **Owns:** the provider's one offering: profile name and picture, jobs and tools (from the catalogue), cities, helpers per booking (minimum and maximum), when a supervisor is sent, hourly rate per helper, travel fee (separate and more than RM0, or included), minimum and maximum hours, cancellation tier, acceptance policy, liability insurance; and its state: **live**, **paused** or **offline**.
- **Domain:** `Offering`, `OfferingState`, `HelperRange`, `SupervisorRule`, `TravelFee`, `HourRange`, `AcceptancePolicy`, `LiabilityInsurance`, `GoLiveChecklist`; and its copies of the provider's team counts, KYB status, payout account and Homa's duration range and platform fee.
- **Rules:**
  - At least one job and one city.
  - The helper maximum is no more than the helpers on the team; when helpers drop below it, the maximum shrinks, and the minimum with it if needed; a growing team doesn't widen the range.
  - Going live needs a verified KYB, a complete profile, a payout account, helpers at least the minimum per booking, and a supervisor if a supervisor rule is set. Going live too early lists everything missing.
  - A live provider with no helper, or with a supervisor rule and no supervisor, stops being live.
  - A retired job or tool comes off the offering; no job left takes it offline.
  - Narrowing Homa's duration range shrinks the hours to fit; no hours left takes it offline.
  - A lapsed business is paused.
  - What the provider receives per hour per helper after Homa's platform fee.
  - The Insured badge until the insurance's valid-until date.
- **Spec:** A provider's offering; Teams, helpers and supervisors; Pricing; Acceptance Policy; Damage Policy; Homa-Set Values (narrowing ranges); Provider App.

### 3.6 `availability`: when a provider can work

- **Owns:** the provider's availability, busy blocks synced from Google Calendar or Apple Calendar, and the buffer between jobs.
- **Domain:** `ProviderAvailability`, `AvailabilityWindow`, `BusyBlock`, `BufferBetweenJobs`.
- **Rules:** back-to-back availability counts as one stretch; an event that ends exactly when a slot starts doesn't block it; the buffer applies around Homa jobs only, never around synced events.
- **Not here:** which times are booked. Accepted reservations are owned by `reservation`, which also keeps each one's buffer as it was at acceptance.
- **Spec:** Availability; Time.

### 3.7 `reservation`: enquiries and the reservation lifecycle

The heart of Homa, and the largest context. It owns everything from an enquiry to a Completed or Cancelled reservation, and decides what each outcome means in money (what the provider earns, what fine is owed). `payment` moves the money.

- **Domain objects:**
  - `Enquiry`: jobs, date and time, duration, helpers; closes when its date passes; becomes a reservation when requested.
  - `Reservation`: the aggregate for one request or reservation, with its status (Requested, Reserved, Declined, Expired, Cancelled, Disputed, Completed) and everything that happens to it.
  - `ReservationTerms`: what is captured at request and never changes: the offering's terms, the Homa-set values version, the address, pin and city, and the access notes copied from the account.
  - `Price`: hours fee, travel fee, provider price (total).
  - `ProviderBookings`: one per provider; the accepted reservations' times, each with the buffer it was accepted with. It is the consistency boundary that stops two acceptances taking the same time.
  - `NamedTeam`, `CheckIn`, `FaceCheckResult`, `CustomerNoShow`, `CompletionSubmission`, `Report` (job, finished work, or safety, damage or loss), `DisputeWindow`.
  - Its copies of other contexts' facts: `ProviderStanding` (verified-until, lapsed, seriously flagged), `ProviderTeam` (members, roles, standing), `ProviderOffering`, `ProviderAvailability` (windows and busy blocks), `CurrentHomaSetValues`, `CustomerDetails` (address, access notes).
- **Built in slices**, each with its own tests:

| Slice | Rules | Spec |
|---|---|---|
| R1 Price and terms | Hours fee = rate × helpers × hours; provider price adds a separate travel fee; whether a supervisor is included; terms captured at request | Pricing; Teams (When a supervisor comes) |
| R2 Enquiry and request | Start on a 15-minute step, at least the minimum notice ahead, within the booking horizon; duration within Homa's range and the provider's hours; helpers within the provider's range; jobs from the offering; the job ends by the business's verified-until date; the request waits for its payment and ends at once when payment fails; edits (not the address) until accepted, with a new total to be secured; expiry at 24 hours after first submission or the response deadline, whichever is sooner, not restarted by an edit; free cancellation before a response; enquiry closes when its date passes; book again | How Homa Works; Time; Request Policy; Payment Protection; Chat; Verified for the whole job |
| R3 Acceptance | Accept or decline by the expiry; accept only once the current total is secured, the time is free (availability, busy blocks, other bookings and their buffers) and the job ends by verified-until; Instant book accepts the moment payment is secured, checking again; on accept, other open requests that now clash are declined at once and not counted against the provider | Acceptance Policy; Request Policy; Availability |
| R4 Naming the team | Helpers matching the number booked, and a supervisor when the threshold is reached; only current team members; changes until the booked start; the team reminder while not named; a lapsed or removed person comes off at once, so the team counts as not named again | Check-in Policy (Who is coming); Verification Policy |
| R5 Check-in | From the earliest check-in to the booked start plus the check-in period, including its last minute; only once the team is named; within the check-in radius; each named person's face check, check-in counting from the first pass; "face check unavailable" goes ahead on location only with no fine; a person who fails can't work; no check-in by the end is a provider no-show | Check-in Policy |
| R6 Cancellation | Customer: free before a response; by tier before the booked start (fee window, travel fee refunded, full provider price when travel is included); no cancelling during the check-in period before check-in; after check-in, the customer no-show fee, released at once and not challengeable. Provider: full refund always; no fine before the fee window; 20% within it; 30% after the booked start, after check-in, or on a no-show | Cancellation Policy; Provider Cancellation Fines; Check-in Policy |
| R7 Customer no-show | After the wait (from the booked start, or from check-in if later) up to and including the booked end; fee = travel fee plus 30% of the hours fee; the no-show report window; a challenge moves it to Disputed and it ends Cancelled whatever Homa decides | Check-in Policy |
| R8 Completion and disputes | Completion with photo or video evidence, also while Disputed over the job; no cancelling or no-show report after it; completion reminder and deadline; the dispute window from the booked start to the extension after the later of the booked end and completion; one report per party, of one of three types; a report moves it to Disputed; Completed when the window ends with no report, or when Homa's decision comes | Completion Policy; Dispute Policy |
| R9 Seriously flagged business | Every accepted reservation without completion moves to Disputed for the flag at once, the team stops, and check-in is locked; an admin cancels it with a full refund (no fine, no fee), or restores it if the flag was cleared and the booked start hasn't come; undecided at the booked start, it is cancelled with a full refund | Verification Policy (A seriously flagged business's bookings and money) |

- **Time zone:** "a date passes" and the dates shown are Malaysia's (`Asia/Kuala_Lumpur`); the domain takes `Instant now` and this zone as a reservation-domain constant.
- **Evidence:** the domain holds references to evidence and the date it may be deleted (the evidence retention period after the reservation is resolved); storing files is infra.

### 3.8 `payment`: payments, refunds, payouts and fines

- **Owns:** each reservation's payment and its states (authorised, authorisation released, held, released, refunded, and failed), the split of what was paid, payouts per reservation, fines and their deduction, held payouts for a flagged business, chargebacks, receipts and payout statements, and the provider's payout account.
- **Domain:** `Payment` (card or FPX), `PaymentState`, `Settlement` (refund, provider earns, platform fee, payout), `Payout`, `PayoutStatement`, `Receipt`, `Fine` (amount, deducted so far, waiver), `FineDeduction`, `HeldPayout`, `Chargeback`, `PayoutAccount`.
- **Rules:**
  - Card: authorised at request, charged only on acceptance; authorisation released on decline, expiry or cancellation before acceptance.
  - FPX: paid in full upfront; refunded in full when the request ends.
  - An edit: a new card authorisation replaces the old, or the FPX difference is paid or refunded.
  - Every charged payment is held until released or refunded.
  - The split always adds up to what the customer paid: refund = paid − provider earns; platform fee = 10% of provider earns (half up); payout = provider earns − platform fee − fine deduction, worked out last.
  - A cancellation fee is released at once; the customer no-show fee and the rest's refund at the end of the no-show report window, or after Homa's decision.
  - Fines: deducted only from completed jobs' payouts, oldest first, each deduction at most 50% of the payout after the platform fee, rounded in the provider's favour; a waiver covers what is left, with a reason, once.
  - A flagged business's payouts are held; an admin releases each one or keeps it back with a reason, recorded as owed to the business and never as Homa's revenue.
  - Chargebacks recovered from the provider the same way as a fine when Homa decides the provider is responsible.
  - Processing fees are never charged to customers or providers.
- **Spec:** Pricing; Payment Protection; Provider Cancellation Fines; Verification Policy (payouts held); Homa Admin (Payments, Fines).

### 3.9 `resolution`: Homa's queue and decisions

- **Owns:** the one queue of everything that needs Homa's decision, and the decisions on cases.
- **Domain:** `Case` (from a report, a wrongful no-show challenge, a missed completion deadline, or a card chargeback), `CaseQueue`, `Decision` (release, partial refund with the provider's amount, full refund; for a challenged no-show: uphold, part of the fee, or full refund), `DecisionTarget` (72 hours from opening), `FlaggedBusinessEntry`.
- **Rules:**
  - Safety, damage or loss reports first; the rest oldest first; each case shows its due date and time.
  - The other party's report and evidence join an open case.
  - A partial refund gives the provider from RM0 up to what was at stake (the provider price, or the no-show fee).
  - Each decision has a reason and the admin who made it.
  - A case is closed without a decision when a serious flag takes its reservation over.
  - Seriously flagged businesses are listed in the queue but are not cases: their reservations are decided in `reservation` and their payouts in `payment`.
- **Spec:** Dispute Policy; Conduct Policy; Homa Admin (Reports).

### 3.10 `conversation`: chats

- **Owns:** one chat per enquiry or reservation, its messages, and who may read it.
- **Domain:** `Chat`, `Message`, `ChatState` (open, archived).
- **Rules:** only the customer starts a chat, by tapping Chat or by requesting; once open, both can write; it stays open for the life of the reservation; an enquiry's chat is archived when the enquiry closes and stays readable; admins open a chat only for a case; the inbox groups chats by provider.
- **Spec:** Chat; Homa Admin (Reports).

### 3.11 `feedback`: ratings

- **Owns:** customers' ratings of providers and of helpers and supervisors, the averages shown, and hiding.
- **Domain:** `Rating`, `ProviderRating`, `TeamMemberRating`, `ProviderCategory`, `HelperCategory`, `ShownAverage`, `HiddenRating`.
- **Rules:** only the customer, only for a Completed reservation, once, within the rating window including its last minute; provider overall 1 to 5 required, categories optional, written feedback up to 1,000 characters; for each helper or supervisor rated, overall required and helper categories optional, no written feedback; an average is shown only from the minimum ratings to show, per subject and per category, otherwise the count with "New"; a hidden rating has a reason, no longer counts, and is never edited.
- **Spec:** Rating Policy; Ratings; Provider page.

### 3.12 `discovery`: search, browse and the provider page

- **Owns:** matching a search to providers, exact and close matches, the anonymous search record, and what the provider page shows.
- **Domain:** `SearchCriteria`, `Match` (exact or close, and how it differs), `SuggestedStart`, `SearchRecord`, `ProviderPage`, `ProviderStats` (jobs done, cancellations), and its copies of live offerings, availability, accepted bookings, verified-until dates, Homa-set values and shown averages.
- **Rules:**
  - Search by jobs, date and time, duration (1 to 6 hours), helpers (at least 1) and city (from the address, or a guest's pick or pin).
  - Exact match: covers the city, live, offers every chosen job, fits the helpers and the duration in its own hours, free for the whole time (with buffers), and the job ends by its verified-until date.
  - Close match: differs in exactly one way, either some but not all of the jobs, or free for the same duration at a start within 2 hours before or after; never both; a suggested start is always bookable.
  - Results show Instant book.
  - The search record keeps the criteria and the number matched, and nothing that identifies the customer.
  - Jobs done counts Completed reservations where the provider was paid more than RM0; cancellations counts late cancellations and no-shows that carry a fine, waived or not.
- **Spec:** How Homa Works (Search, Browse, Choose); Time; Provider page; Homepage; Privacy Policy (Searches); Goals and Measures.

### 3.13 `notification`: telling people

- **Owns:** what each person is told and when, across every context: new requests, acceptance, declines, expiry, team named or changed, reminders, check-in, completion, reports, decisions, lapses and offering changes.
- **Domain:** `Notice` (who, which kind, about what); the words and languages are infra.
- **Why a context:** the spec's principle 2 ("the system adjusts and tells people") makes telling people a rule of its own. Every other context only publishes what happened; `notification` decides who hears about it.
- **Spec:** Product Principles; every "is told" and "reminds" in the spec; External Services (Push notification service).

### Not a bounded context in this plan

- **Admin tracking** (Overview, Reservations, Payments, Providers, Customers, Fines, Ratings): read models built from the same events. Planned with infra, since it is mostly queries; the measures it needs are in the event catalogue (Section 6).
- **The admin record of changes:** each admin usecase records the admin, the time and the reason with the change, in its own context.

---

## 4. Build order

Upstream first, so each context's copies of other contexts' facts describe something already built.

| Step | Context | Depends on (through events) |
|---|---|---|
| 0 | `kernel` | — |
| 1 | `policy` | — |
| 2 | `catalogue` | — |
| 3 | `account` | — |
| 4 | `provider` | — |
| 5 | `offering` | policy, catalogue, provider, payment (payout account) |
| 6 | `availability` | — |
| 7 | `reservation`, slices R1 to R9 | policy, account, provider, offering, availability, payment, resolution |
| 8 | `payment` | reservation, provider, resolution |
| 9 | `resolution` | reservation, payment, provider |
| 10 | `conversation` | reservation |
| 11 | `feedback` | reservation, provider |
| 12 | `discovery` | policy, catalogue, offering, availability, provider, reservation, feedback |
| 13 | `notification` | all |

The same order is used in Phase 1 (domain) and Phase 3 (usecase).

---

## 5. Usecase layer conventions

### Shape (discipline, Sections 3, 6 and 7)

- One usecase, one interface, one implementation. The implementation is named for the actor and the act, and the interface for the actor: `CustomerRequestReservation implements CustomerReservationRequester`, method `request(...)`.
- Each interface has its own request and response DTOs in `usecase/dto`.
- Every IE reaction is `Handle<Event>` in `usecase/ie`, implementing `<Event>Handler` in `usecase/port/in`.
- Out ports in `usecase/port/out`: one repository per aggregate (`ReservationRepository`), one `<Context>IEPublisher`, and one port per outside service the context uses, named for the service in the spec's words (`KycProvider`, `PaymentProvider`, `CalendarSync`, `Geoservice`, `PushNotifications`).
- The time comes from an injected `java.time.Clock` and is passed to the domain as `Instant now`.
- Who is acting comes in the request DTO, already mapped by infra security; the usecase checks the record belongs to them.

### What a usecase does, in order

Load through out ports → check the record belongs to the actor → ask the domain → save → publish the IE → return the response. No `if` on a business value: any such branch is a domain rule.

### Actors

| Actor | Who |
|---|---|
| Guest | Someone searching without an account |
| Customer | A signed-in customer |
| Provider | The business's owner or director, in the provider app |
| Admin | A Homa founder in the admin console |
| Homa | The scheduler: deadlines and reminders that fire on time |
| KYC provider, Payment provider, Calendar | Results arriving from outside services, through infra |

Scheduled acts are ordinary usecases with the actor Homa (for example `HomaExpireRequests`), triggered from `infra/transport/messaging/`.

### Usecases per context

The lists below are the acts the spec names. Each one is planned as a port and an implementation, plus its tests.

| Context | Acts |
|---|---|
| `policy` | Admin changes a Homa-set value; anyone views the current values |
| `catalogue` | Admin adds, renames, moves or retires a job; adds, renames or retires a tool; features, renames, moves or removes a featured search; anyone views the showcase, job list and tool list |
| `account` | Customer registers (phone, Google or Apple); business registers; signs in; adds or removes Google or Apple sign-in; customer changes name, address, access notes or language; person requests to see, correct, copy or delete their data; admin records a data request |
| `provider` | Provider starts KYB; KYC provider reports the KYB result; provider starts adding a team member; KYC provider reports the member's result; provider confirms lawful employment; provider changes a role; provider removes a member; provider starts a re-verification; KYC provider reports re-verification needed by a date, a lapse, a serious flag or a cleared flag; admin asks for an early re-check; admin removes a member not allowed to work |
| `offering` | Provider sets each part of the offering; provider declares liability insurance; provider goes live (or sees what is missing); provider takes the offering offline; IE reactions: team changed, KYB changed, job or tool retired, duration range narrowed, payout account added |
| `availability` | Provider sets availability; provider sets the buffer; calendar sync reports busy blocks; provider connects or disconnects a calendar |
| `reservation` | Customer opens an enquiry; requests (directly, from an enquiry, or book again); edits a request; cancels a request or reservation; changes access notes; files a report; challenges a no-show. Provider accepts or declines; names the team; checks in; reports a customer no-show; submits completion; cancels; files a report. KYC provider reports a face check result. Admin cancels or restores a flagged reservation. Homa expires requests; reminds the provider to name the team; ends the check-in period (provider no-show); ends the no-show report window; reminds and ends the completion deadline; ends the dispute window; cancels undecided flagged reservations at the booked start; closes enquiries past their date; deletes evidence past its retention. IE reactions: payment secured or failed, team member joined, changed, lapsed or removed, business verified-until changed, lapsed, flagged or cleared, offering changed, availability changed, Homa-set values changed, customer details changed, case decided, case closed without a decision |
| `payment` | Customer pays for a request; payment provider reports a payment, failure, refund, payout or chargeback; provider adds a payout account; admin releases or keeps back a held payout; admin waives a fine (including a no-show fine when a serious flag came too close to the start to replace the person); Homa pays out released payments. IE reactions: request made, edited, ended or accepted; reservation cancelled; no-show reported, settled or decided; reservation completed; fine owed; business flagged |
| `resolution` | Admin views the queue; decides a case; IE reactions: report filed, no-show challenged, completion deadline missed, chargeback opened, flag taken over a reservation, business flagged or cleared |
| `conversation` | Customer starts a chat; customer or provider sends a message; admin opens a chat for a case; IE reactions: enquiry opened, request made, enquiry closed |
| `feedback` | Customer rates a reservation; admin hides a rating; IE reactions: reservation completed, team named |
| `discovery` | Guest or customer searches; views a provider page; views the showcase's searches filled in; IE reactions: offering, availability, bookings, verification, ratings, Homa-set values and catalogue changes |
| `notification` | IE reactions only: one handler per event that tells someone something |

---

## 6. Integration event catalogue

Events carry only primitives, Strings, kernel types and JDK value types, never domain objects (discipline, Section 2). Each carries what its consumers need so they never have to ask back. Every handler is safe to run twice for the same event, because delivery through an outbox can repeat.

| Event | Published by | Consumed by |
|---|---|---|
| `HomaSetValuesChangedIE` (the whole new version) | policy | offering, reservation, payment, discovery |
| `JobAddedIE`, `JobChangedIE`, `JobRetiredIE`, `ToolAddedIE`, `ToolChangedIE`, `ToolRetiredIE` | catalogue | offering, discovery |
| `CustomerRegisteredIE`, `CustomerDetailsChangedIE` | account | reservation, notification |
| `BusinessRegisteredIE`, `AccountClosedIE` | account | provider, every context holding personal data |
| `BusinessVerifiedIE`, `BusinessKybRejectedIE`, `BusinessVerifiedUntilChangedIE`, `BusinessLapsedIE`, `BusinessSeriouslyFlaggedIE`, `BusinessFlagClearedIE` | provider | offering, reservation, payment, resolution, discovery, notification |
| `TeamMemberJoinedIE`, `TeamMemberRoleChangedIE`, `TeamMemberRemovedIE`, `TeamMemberLapsedIE`, `TeamMemberVerifiedUntilChangedIE` | provider | offering, reservation, discovery, feedback, notification |
| `OfferingChangedIE` (the whole offering, its state and why it changed) | offering | reservation, discovery, notification |
| `AvailabilityChangedIE` | availability | reservation, discovery |
| `EnquiryOpenedIE`, `EnquiryClosedIE` | reservation | conversation |
| `ReservationRequestedIE`, `RequestEditedIE` (with the total to secure) | reservation | payment, conversation, notification |
| `ReservationAcceptedIE` (time and buffer) | reservation | payment, discovery, notification |
| `RequestDeclinedIE`, `RequestExpiredIE`, `RequestCancelledIE`, `RequestEndedIE` | reservation | payment, notification, discovery (response measures) |
| `TeamNamedIE`, `TeamReminderDueIE` | reservation | feedback, notification |
| `CheckedInIE`, `FaceCheckFailedIE` | reservation | notification |
| `ReservationCancelledIE` (who, what the provider earns, any fine and its kind) | reservation | payment, discovery (cancellations), notification |
| `CustomerNoShowReportedIE`, `NoShowChallengedIE`, `NoShowFeeSettledIE` | reservation | payment, resolution, notification |
| `CompletionSubmittedIE`, `CompletionDeadlineMissedIE` | reservation | resolution, notification |
| `ReportFiledIE` | reservation | resolution, notification |
| `ReservationDisputedForFlagIE`, `FlaggedReservationRestoredIE` | reservation | resolution, payment, notification |
| `ReservationCompletedIE` (what the provider earns, the captured platform fee) | reservation | payment, feedback, discovery (jobs done), notification |
| `PaymentSecuredIE`, `PaymentFailedIE` | payment | reservation, notification |
| `RefundIssuedIE`, `PayoutPaidIE`, `FineWaivedIE` | payment | notification |
| `PayoutAccountAddedIE` | payment | offering |
| `ChargebackOpenedIE` | payment | resolution |
| `CaseOpenedIE`, `CaseDecidedIE`, `CaseClosedWithoutDecisionIE` | resolution | reservation, payment, notification |
| `MessageSentIE` | conversation | notification |
| `RatingsShownChangedIE` (shown averages and counts for a provider or team member) | feedback | discovery |
| `SearchPerformedIE` | discovery | admin tracking (later) |

Money events carry the platform fee percentage captured with the reservation's Homa-set values, so `payment` uses the value in force at request, not today's.

---

## 7. Quality and traceability

- **Rule to test.** Each domain test class names a spec feature, and each test method is the outcome in the spec's words (for example `aRequestExpiresAtTheResponseDeadlineWhenThatComesFirst`). Each context's README lists every spec rule it owns with the test that covers it.
- **Boundaries.** Each "including its last minute", "up to and including" and "within" in the spec gets a test on both sides of the boundary.
- **Money.** Every split has a test that the parts add up to exactly what the customer paid, including odd sen amounts.
- **Definition of done for a context in a phase:** all its rules traced and tested; CRAP below 6; mutation at or above its layer's threshold and the ratchet; ArchUnit green; its open questions answered.

---

## 8. Risks and how the plan handles them

| Risk | How it is handled |
|---|---|
| `reservation` grows too large to reason about | Built in nine slices, each with its own tests; time and money rules in small value objects (`DisputeWindow`, `CheckInPeriod`, `Price`, `CancellationOutcome`) rather than in `Reservation` itself |
| A context imports another by mistake, since all share one source tree | The ArchUnit context-isolation test runs with every build and fails it |
| Two acceptances take the same time | `ProviderBookings` per provider is the aggregate that accepts, with a version checked when saved |
| Contexts drift on the same fact | Only the owning context changes a fact; every other context holds a copy updated by its IE handler, and events carry whole snapshots where a copy is kept (offering, Homa-set values, availability) |
| A value changes between request and decision | Each reservation captures the Homa-set values version and the offering terms at request; events carry what was captured |
| Rules invented to fill gaps | Every gap is an open question below, answered and written into the spec before the code |

---

## 9. Open questions for the product owner

Each one blocks the context named. Each has a **suggested answer** with the reason behind it, mostly from the spec's own product principles. A suggestion is not a decision: the product owner decides, the answer goes into the spec first, and only then into code.

| # | Question | Blocks | Suggested answer | Why |
|---|---|---|---|---|
| Q1 | Are the three cities fixed in the product (a code change to add one), or a Homa-set list the admins change? Can a customer register an address outside them? | kernel, account, discovery | Fixed in the product for the MVP. A customer's address must be in one of the three cities; outside them, the app says Homa isn't there yet, and the person can still browse as a guest by picking a city | Opening a city is a launch decision (recruiting providers, testing demand), not a setting to flip. Principle 3: an account at an address no provider covers would only ever search and find nobody |
| Q2 | The spec says the SMS code is 6 digits, valid 5 minutes, and needs renewing after 5 wrong codes, and also that the OTP provider handles codes. Are these requirements on the OTP provider (nothing for Homa to enforce)? | account | Yes. Move them to the OTP provider's requirements under External Services. Homa only records that the phone number was verified | "Homa uses their results and does not build those checks itself" (Where Homa puts its effort) |
| Q3 | With Instant book, if the time is no longer free when the payment is secured, the request "ends": is its status Declined or Cancelled? | reservation R3 | **Declined**, and the meaning of Declined widens to "or the time was no longer free when Instant book checked it". Like other automatic declines, it doesn't count against the provider's rates | Cancelled means someone chose to end it, or the customer's payment failed. Here the provider couldn't take the time, which is what Declined already means ("accepted another booking that takes the same time") |
| Q4 | If charging an authorised card fails at acceptance, what happens to the reservation? | reservation R3, payment | A request becomes Reserved only once the charge goes through. If the charge fails, the request ends **Cancelled** as a failed payment: nothing is charged, the customer is told and can request again, and no other request was declined for it, because clashing requests are declined only once the reservation is Reserved | Same rule as any failed payment (Payment Protection). Principle 2: the system sorts it out and tells people. And the provider's other customers never lose a request to a booking that didn't happen |
| Q5 | When an offering is paused or goes offline, or the business's verified-until date moves earlier than a request's end, do open requests end at once (principle 3), or stay until they expire? | reservation R2, R3 | One rule: **a request ends at once, Declined, when it can no longer be accepted**: the business lapsed or was seriously flagged, or the job now ends after its verified-until date. These don't count against the provider's rates. Going paused or offline otherwise doesn't touch open requests: they keep their captured terms, and the provider can still accept or decline them until they expire | Principle 3: nobody waits on something that can't happen. Principle 5: one clear rule. A request already made keeps the offering it was made with (Provider App, Offering), so a provider who goes offline can still honour customers who already paid |
| Q6 | When two bookings were accepted with different buffers, which buffer must the gap between them respect: the existing booking's, the new one's, or the larger? | reservation R3, discovery | **The larger.** Each accepted reservation keeps its own buffer free around itself: the existing one with the buffer it was accepted with, the new one with the buffer in force now | The buffer exists so the team arrives on time (the 5-star line). "Changing it does not affect bookings already accepted" protects the old booking's buffer; the new booking's buffer protects the new one |
| Q7 | For a close match by time, which start is suggested when several are free within 2 hours: the nearest, and on a tie the earlier? Does each provider appear at most once? | discovery | Each provider appears **once**: as an exact match if they are one, otherwise as one close match. For a time close match, suggest the start **nearest** the chosen time; on a tie, the **earlier** one | The customer chose a time, so the nearest start is the least change. One card per provider keeps results focused (Wroblewski). Earlier on a tie is arbitrary but fixed (principle 5) |
| Q8 | How are search results ordered? | discovery | Exact matches before close matches. Within each group: **Instant book first**, then by shown average stars, highest first, then providers still "New", then by lower provider price. Close matches by time before close matches by jobs. Treated as an assumption, checked against the share of searches that lead to a reservation | "Customers know at once" is a Homa goal, and Instant book is how it happens. Stars rank only where they are honestly shown (principle 4). A time-shifted match still does every chosen job. Risk to watch: new providers sit low; revisit with launch data |
| Q9 | "Including its last minute": for a 10:15 limit, is 10:15:59 still in time? Is "up to 30 days ahead" a start no later than exactly 30 × 24 hours from now, or any time on the 30th day? | reservation, discovery, feedback | A limit given as a minute includes that **whole minute**: 10:15:59 is still in time. The booking horizon is by **calendar date in Malaysia**: any start on a date up to 30 days after today | People read times to the minute, and the spec's own example says "up to and including 10:15". A date picker that shows the next 30 days is easier to understand than a cut-off at the current time of day |
| Q10 | Which moment is the recorded check-in time: the location check, or the first named person passing the face check? | reservation R5 | The moment the **first named person passes the face check**, or the location check when the face check is unavailable. The check-in deadline is judged on that same moment | "Check-in counts once the first named person passes." The safety principle is about verified people at the door, not a phone near it. Check-in opens 15 minutes before the start, so a provider has 30 minutes to finish it |
| Q11 | Can the provider submit completion while a reservation is Disputed for a serious flag? | reservation R8, R9 | **No.** The team has stopped, and the reservation ends either cancelled with a full refund or, before the booked start only, restored. A report and evidence filed before the flag stay with it | Principle 1: only verified people work, even if a booking is lost. Once the start has passed, a flagged reservation can't be restored, so a completion would have nothing to complete |
| Q12 | A paused offering of a lapsed business: does it go live again on its own once the business passes re-verification? | offering | **Yes**, if it was live when it was paused and still meets every go-live condition; otherwise it goes offline and the provider sees what is missing. Either way the provider is told | Principle 2: the system adjusts and tells people. Paused means "takes no new requests until it passes", so passing is the end of the pause |
| Q13 | A card chargeback case is "decided with a release, a partial refund or a full refund": how does each map to "the provider is responsible" (recovered from payouts) or "Homa contests"? What if the payout was already paid out? | resolution, payment | A chargeback decision sets **what the provider finally earns**, like any case. **Release:** the provider isn't responsible and keeps their payout, and Homa contests the chargeback. **Partial refund:** the provider is responsible for part. **Full refund:** the provider is responsible for all of it. Whatever the provider was paid above what they now earn is recovered from their next payouts the same way as a fine, so it doesn't matter whether the payout was already paid. Homa's platform fee follows what they finally earn, and Homa bears the rest, including a chargeback it loses after contesting | It reuses the queue's one decision type (principle 5) and the existing rules: "the platform fee follows what the provider finally earns", and recovery "the same way as a fine" |
| Q14 | Do payouts from a Homa decision (release or partial refund) count as "completed jobs" that fines can be deducted from? | payment | **Yes**, when the provider is paid more than RM0. The reservation is Completed and it counts in jobs done, so it is a completed job for fine deductions too. A full refund pays nothing, so there is nothing to deduct | One meaning of "completed job" everywhere (principle 5), matching the provider page's "jobs done" |
| Q15 | Can the customer rate a named helper who failed the face check and didn't work? | feedback | **No.** The customer can rate only people who worked: named on the job and passed the face check, or checked in with the face check unavailable. A missing or replaced person is reported, not rated | Principle 4: honest records. A rating of someone who wasn't there would be about something else; "ratings and reports are separate" |
| Q16 | "First-time searchers who make a first reservation within 7 days" needs to link a search to a customer, but searches keep nothing that identifies the customer. How should this measure be taken? | discovery, admin tracking | Keep searches anonymous. The app knows whether it has made a search before, so each search record also says whether it was the **first search on that device**, which gives the number of first-time searchers. The app keeps the date of that first search on the device, and sends it with that customer's **first request**, which gives how many booked within 7 days. Nothing links a search record to a person. It is a device-based estimate, and is labelled that way in the admin console | It meets the goal without breaking the Privacy Policy ("nothing that identifies the customer"). An approximate honest measure is better than an exact one that collects personal data |

## 10. What comes after this plan

1. The product owner answers the open questions, and the spec is updated.
2. Phase 0: the build, `kernel/` and the gates.
3. Phase 1, step by step from Section 4, each context reviewed against the spec before the next starts.
4. Phase 2, then Phase 3 in the same order.
5. A plan for infra and `app/`, once the ports are fixed.
