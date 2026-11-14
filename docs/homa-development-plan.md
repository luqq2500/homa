# Homa Development Plan: Domain and Usecase

This plan turns `homa-product-description.md` (the spec) into bounded contexts, and orders their development under `backend_module_discipline.md` (the discipline). It covers the `kernel/`, every bounded context's `domain` and `usecase` layers, and the `ie/` contracts between them. Infra and `app/` come after, and are only touched on here where a usecase port has to be shaped for them.

When this plan and the spec disagree, the spec wins (discipline, Section 8). Where the spec leaves something open, this plan does not decide it: it lists it under **Questions for the product owner** (Section 9), and the module that needs it waits for the answer.

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

**Decisions before code.** The open questions in Section 9 are answered. Before kickoff, those answers and the new words in Section 10 go into the spec, so every rule the code enforces is traced to the spec's own text. Q17 is still open; only `availability` waits for it.

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

| Startup budget | Not measured until Phase 4: there is no deployable before infra and `app/` |

The gate script and ratchet removed in `8ef06db` can be restored from history and adapted to this layout: PIT runs once per context and layer, with `targetClasses` and `targetTests` set to `<context>.domain.*` or `<context>.usecase.*`, and the script fails if a context package has no PIT run. An equivalent mutant is excluded only with a written reason next to the exclusion.

### `kernel/`

Only types that pass the admission rule (at least two domains, a value object, no context's rule, Java standard library only):

| Type | Why it qualifies | Rules it carries |
|---|---|---|
| `Money` | Every money rule in reservation, payment, offering, resolution | Ringgit in whole sen; adding, subtracting, comparing; a percentage of an amount rounded half up to the nearest sen, and one rounded down for "rounded in the provider's favour" (the fine deduction cap) |
| `Percentage` | Platform fee, tiers, fines, no-show fee, deduction cap | A value from 0 to 100% |
| `TimeSlot` | A start and an end, used by availability, reservation and discovery | End after start; overlaps; touching end-to-start does not overlap (spec: "a booking that ends exactly when a slot starts does not block that slot") |
| `City` | Account, offering, discovery, reservation | The three cities: Salak Tinggi, Cyberjaya, Putrajaya, fixed for the MVP (Q1) |
| `MalaysiaTime` | Reservation (an enquiry closes when its date passes), discovery and reservation (the booking horizon by calendar date, Q9) | Homa's time zone, `Asia/Kuala_Lumpur`; the Malaysian date of an instant |

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
- **Rules:** every account has a verified phone number, which is always a way in and can't be removed; a phone number, a Google account or an Apple account opens at most one customer account and one business account; Google or Apple sign-in can be added and removed; a new address is used for new searches and requests only; the address must be in one of the three cities (Q1).
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
  - A serious flag lapses at once; a flag the KYC provider clears ends that lapse, the same as a pass (Q12).
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
  - A lapsed business is paused. When it passes, the offering goes live again if it was live when paused and still meets every go-live condition; otherwise it goes offline with the list of what is missing (Q12).
  - What the provider receives per hour per helper after Homa's platform fee.
  - The Insured badge until the insurance's valid-until date.
- **Spec:** A provider's offering; Teams, helpers and supervisors; Pricing; Acceptance Policy; Damage Policy; Homa-Set Values (narrowing ranges); Provider App.

### 3.6 `availability`: when a provider can work

- **Owns:** the provider's availability, busy blocks synced from Google Calendar or Apple Calendar, and the buffer between jobs.
- **Domain:** `ProviderAvailability`, `AvailabilityTime` (how it is set waits for Q17), `CalendarEvent` (synced from Google or Apple), `BufferBetweenJobs`.
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
  - `ProviderBookings`: one per provider; the accepted reservations' times, each with the buffer it was accepted with, plus a request whose payment is being completed at acceptance (Q4), which holds its time until the payment succeeds or fails. It is the consistency boundary that stops two acceptances taking the same time.
  - `NamedTeam`, `CheckIn`, `FaceCheckResult`, `CustomerNoShow`, `CompletionSubmission`, `Report` (job, finished work, or safety, damage or loss), `DisputeWindow`.
  - Its copies of other contexts' facts: `ProviderStanding` (verified-until, lapsed, seriously flagged), `ProviderTeam` (members, roles, standing), `ProviderOffering`, `ProviderAvailability` (availability times and calendar events), `CurrentHomaSetValues`, `CustomerDetails` (address, access notes).
- **Built in slices**, each with its own tests:

| Slice | Rules | Spec |
|---|---|---|
| R1 Price and terms | Hours fee = rate × helpers × hours; provider price adds a separate travel fee; whether a supervisor is included; terms captured at request | Pricing; Teams (When a supervisor comes) |
| R2 Enquiry and request | Start on a 15-minute step, at least the minimum notice ahead, within the booking horizon by Malaysian calendar date (Q9); duration within Homa's range and the provider's hours; helpers within the provider's range; jobs from the offering; the job ends by the business's verified-until date; the request waits for its payment and ends at once when payment fails; edits (not the address) until accepted, with a new total to be secured; expiry at 24 hours after first submission or the response deadline, whichever is sooner, not restarted by an edit; free cancellation before a response; enquiry closes when its date passes; book again; access notes copied at request, changeable until the job is completed, seen by the provider from acceptance; an open request ends at once, **Declined**, when the provider can no longer accept it: the business lapsed or was flagged, the job now ends after its verified-until date, or it asks for more helpers than the team now has (Q5) | How Homa Works; Time; Request Policy; Payment Protection; Chat; Verified for the whole job |
| R3 Acceptance | Accept or decline by the expiry; accept only once the current total is secured, the time is free (availability, busy blocks, other bookings and their buffers) and the job ends by verified-until; Instant book accepts the moment payment is secured, checking again, and ends the request **Declined** if the time is no longer free (Q3); on acceptance Homa completes the payment, and the request becomes **Reserved** only when the payment succeeds, or ends **Cancelled** as a failed payment (Q4); how each payment method gets there is the payment provider's detail; once Reserved, other open requests that now clash, given the larger of the two buffers (Q6), are declined at once; automatic declines don't count against the provider | Acceptance Policy; Request Policy; Availability |
| R4 Naming the team | Helpers matching the number booked, and a supervisor when the threshold is reached; only current team members; changes until the booked start; the team reminder while not named; a lapsed or removed person comes off at once, so the team counts as not named again | Check-in Policy (Who is coming); Verification Policy |
| R5 Check-in | From the earliest check-in to the booked start plus the check-in period, including its last minute; only once the team is named; within the check-in radius; each named person's face check; the check-in time, and the deadline, are the moment the first named person passes, or the moment the provider marks check-in when the face check is unavailable (Q10); "face check unavailable" goes ahead on location only with no fine; a person who fails can't work; no check-in by the end is a provider no-show | Check-in Policy |
| R6 Cancellation | Customer: free before a response; by tier before the booked start (fee window, travel fee refunded, full provider price when travel is included); no cancelling during the check-in period before check-in; after check-in, the customer no-show fee, released at once and not challengeable. Provider: full refund always; no fine before the fee window; 20% within it; 30% after the booked start, after check-in, or on a no-show | Cancellation Policy; Provider Cancellation Fines; Check-in Policy |
| R7 Customer no-show | After the wait (from the booked start, or from check-in if later) up to and including the booked end; fee = travel fee plus 30% of the hours fee; the no-show report window; a challenge moves it to Disputed and it ends Cancelled whatever Homa decides | Check-in Policy |
| R8 Completion and disputes | Completion with photo or video evidence, also while Disputed over the job, never while Disputed for a serious flag (Q11); no cancelling or no-show report after it; completion reminder and deadline; the dispute window from the booked start to the extension after the later of the booked end and completion; one report per party, of one of three types; a report moves it to Disputed; Completed when the window ends with no report, or when Homa's decision comes | Completion Policy; Dispute Policy |
| R9 Seriously flagged business | Every accepted reservation without completion moves to Disputed for the flag at once, the team stops, and check-in is locked; an admin cancels it with a full refund (no fine, no fee), or restores it if the flag was cleared and the booked start hasn't come; undecided at the booked start, it is cancelled with a full refund | Verification Policy (A seriously flagged business's bookings and money) |

- **Time:** the domain takes `Instant now`; dates ("its date passes", the booking horizon) come from `kernel`'s `MalaysiaTime`.
- **Location:** the geoservice works out the device's distance from the customer's pin (infra, behind a `Geoservice` port); the domain only checks it against the check-in radius.
- **Evidence:** the domain holds references to evidence and the date it may be deleted (the evidence retention period after the reservation is resolved); storing files is infra.

### 3.8 `payment`: payments, refunds, payouts and fines

- **Owns:** each reservation's payment and its states (secured, succeeded and held, released, refunded, let go with nothing taken, and failed), the split of what was paid, payouts per reservation, fines and their deduction, held payouts for a flagged business, chargebacks, receipts and payout statements, and the provider's payout account.
- **Domain:** `Payment`, `PaymentState`, `PaymentSplit` (refund, provider earns, platform fee, payout), `Payout`, `PayoutStatement`, `Receipt`, `Fine` (amount, deducted so far, waiver), `FineDeduction`, `HeldPayout`, `Chargeback`, `PayoutAccount`.
- **Rules:**
  - A request's payment is secured before the provider sees it; an edit that changes the total secures the new total.
  - On acceptance the payment is completed; it succeeds (and the reservation is Reserved) or fails (and the request is Cancelled) (Q4).
  - A request that ends without a reservation (declined, expired, cancelled) lets its payment go in full: the customer pays nothing.
  - Every payment that succeeded is held until released or refunded.
  - The split always adds up to what the customer paid: refund = paid − provider earns; platform fee = 10% of provider earns (half up); payout = provider earns − platform fee − fine deduction, worked out last.
  - A cancellation fee is released at once; the customer no-show fee and the rest's refund at the end of the no-show report window, or after Homa's decision.
  - Fines: deducted only from completed jobs' payouts, oldest first, each deduction at most 50% of the payout after the platform fee, rounded in the provider's favour; a waiver covers what is left, with a reason, once.
  - A flagged business's payouts are held; an admin releases each one or keeps it back with a reason, recorded as owed to the business and never as Homa's revenue.
  - Chargebacks: the decision sets what the provider finally earns; whatever they were paid above that is recovered from their next payouts the same way as a fine, paid out or not; the platform fee follows what they finally earn, and Homa bears the rest (Q13).
  - A completed job, for fine deductions, is a Completed reservation that pays the provider more than RM0, including one decided by Homa (Q14).
  - Processing fees are never charged to customers or providers.
- **Not here:** how each payment method works. Card authorisation and charge, FPX paid upfront, releasing an authorisation or refunding a difference, and refund timing are the payment provider's details, behind the `PaymentProvider` port; Homa's domain only knows whether a payment is secured, succeeded, failed or let go.
- **Spec:** Pricing; Payment Protection; Provider Cancellation Fines; Verification Policy (payouts held); Homa Admin (Payments, Fines).

### 3.9 `resolution`: Homa's queue and decisions

- **Owns:** the one queue of everything that needs Homa's decision, and the decisions on cases.
- **Domain:** `Case` (from a report, a wrongful no-show challenge, a missed completion deadline, or a chargeback), `CaseQueue`, `Decision` (release, partial refund with the provider's amount, full refund; for a challenged no-show: uphold, part of the fee, or full refund), `DecisionTarget` (72 hours from opening), `SeriouslyFlaggedBusiness` (its disputed reservations and held payouts, listed but not a case).
- **Rules:**
  - Safety, damage or loss reports first; the rest oldest first; each case shows its due date and time.
  - The other party's report and evidence join an open case.
  - A partial refund gives the provider from RM0 up to what was at stake (the provider price, or the no-show fee). A chargeback is decided the same way: release (Homa contests), partial or full refund (Q13).
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
- **Rules:** only the customer, only for a Completed reservation, once, within the rating window including its last minute; provider overall 1 to 5 required, categories optional, written feedback up to 1,000 characters; only people who worked can be rated: named and passed the face check, or checked in when it was unavailable, including someone who later stopped (Q15); for each helper or supervisor rated, overall required and helper categories optional, no written feedback; an average is shown only from the minimum ratings to show, per subject and per category, otherwise the count with "New"; a hidden rating has a reason, no longer counts, and is never edited.
- **Spec:** Rating Policy; Ratings; Provider page.

### 3.12 `discovery`: search, browse and the provider page

- **Owns:** matching a search to providers, exact and close matches, the anonymous search record, and what the provider page shows.
- **Domain:** `SearchCriteria`, `Match` (exact or close, and how it differs), `SuggestedStart`, `SearchRecord`, `ProviderPage`, `ProviderStats` (jobs done, cancellations), and its copies of live offerings, availability, accepted bookings, verified-until dates, Homa-set values and shown averages.
- **Rules:**
  - Search by jobs, date and time (on a 15-minute step, at least the minimum notice ahead, within the booking horizon), duration (1 to 6 hours), helpers (at least 1) and city (from the address, or a guest's pick or pin; a pin outside the three cities is told Homa isn't there yet, Q1).
  - Exact match: covers the city, live, offers every chosen job, fits the helpers and the duration in its own hours, free for the whole time (with buffers), and the job ends by its verified-until date.
  - Close match: differs in exactly one way, either some but not all of the jobs, or free for the same duration at a start within 2 hours before or after; never both; a suggested start is always bookable.
  - Each provider appears once; a time close match suggests the start nearest the chosen time, the earlier on a tie (Q7).
  - Order: exact before close, close by time before close by jobs; within each, Instant book first, then shown average stars, then "New", then lower provider price (Q8).
  - Results show Instant book.
  - The search record keeps the criteria, the number of exact and of close matches, and whether it was the device's first search, and nothing that identifies the customer; a device's first request sends a separate anonymous record of the days since its first search (Q16).
  - Jobs done counts Completed reservations where the provider was paid more than RM0; cancellations counts late cancellations and no-shows that carry a fine, waived or not.
- **Spec:** How Homa Works (Search, Browse, Choose); Time; Provider page; Homepage; Privacy Policy (Searches); Goals and Measures.

### 3.13 `notification`: telling people

- **Owns:** what each person is told and when, across every context: new requests, acceptance, declines, expiry, team named or changed, reminders, check-in, completion, reports, decisions, lapses and offering changes.
- **Domain:** `Notification` (who, which kind, about what); the words and languages are infra.
- **Why a context:** the spec's principle 2 ("the system adjusts and tells people") makes telling people a rule of its own. Every other context only publishes what happened; `notification` decides who hears about it.
- **Spec:** Product Principles; every "is told" and "reminds" in the spec; External Services (Push notification service).

### Not a bounded context in this plan

- **Admin tracking** (Overview, Reservations, Payments, Providers, Customers, Fines, Ratings): read models built from the same events. Planned with infra, since it is mostly queries; the measures it needs are in the event catalogue (Section 6).
- **The admin record of changes:** each admin usecase records the admin, the time and the reason with the change, in its own context.

---

## 4. Build order

Upstream first. Where two contexts react to each other (reservation and payment, reservation and resolution, offering and payment), the one whose rules shape the other's goes first, and the later one is built against the earlier one's domain.

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
- Exceptions live in the layer that raises them: business rule violations in `domain/exception`, orchestration failures (not found, not yours) in `usecase/exception`.
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

### Identity and events

- Every entity id comes from its domain-owned generator, `ReservationId().generate()`, returned as a `String`.
- Other contexts' ids travel as Strings. The business's id is the provider's id everywhere: the offering, availability, bookings, payouts and ratings are all keyed by it, since each provider has one offering.
- `ie/port` has one event interface per publishing context (`ReservationIE`); every event of that context implements it, and the context's one `<Context>IEPublisher.publish()` takes that interface.
- An event that carries a snapshot (`OfferingChangedIE`, `AvailabilityChangedIE`, `HomaSetValuesChangedIE`) also carries its source's version, so a copy never goes back to an older snapshot when events arrive late or twice.

### Usecases per context

The lists below are the acts the spec names. Each one is planned as a port and an implementation, plus its tests.

| Context | Acts |
|---|---|
| `policy` | Admin changes a Homa-set value; anyone views the current values |
| `catalogue` | Admin adds, renames, moves or retires a job; adds, renames or retires a tool; features, renames, moves or removes a featured search; anyone views the showcase, job list and tool list |
| `account` | Customer registers (phone, Google or Apple); business registers; signs in; adds or removes Google or Apple sign-in; customer changes name, address, access notes or language; person requests to see, correct, copy or delete their data; admin records a data request |
| `provider` | Provider starts KYB; KYC provider reports the KYB result; provider starts adding a team member; KYC provider reports the member's result; provider confirms lawful employment; provider changes a role; provider removes a member; provider starts a re-verification; KYC provider reports re-verification needed by a date, a lapse, a serious flag or a cleared flag; admin asks for an early re-check; admin removes a member not allowed to work |
| `offering` | Provider sets each part of the offering; provider declares liability insurance; provider goes live (or sees what is missing); provider takes the offering offline; IE reactions: team changed, KYB passed, lapsed, flagged or cleared, job or tool retired, Homa-set values changed (duration range, platform fee), payout account added |
| `availability` | Provider sets availability; provider sets the buffer; calendar sync reports busy blocks; provider connects or disconnects a calendar |
| `reservation` | Customer opens an enquiry; requests (directly, from an enquiry, or book again); edits a request; cancels a request or reservation; changes access notes; files a report; challenges a no-show. Provider accepts or declines; names the team; checks in; reports a customer no-show; submits completion; cancels; files a report. KYC provider reports a face check result. Admin cancels or restores a flagged reservation. Homa expires requests; reminds the provider to name the team; ends the check-in period (provider no-show); ends the no-show report window; reminds and ends the completion deadline; ends the dispute window; cancels undecided flagged reservations at the booked start; closes enquiries past their date; deletes evidence past its retention. IE reactions: payment secured, succeeded or failed, team member joined, changed, lapsed or removed, business verified-until changed, lapsed, flagged or cleared, offering changed, availability changed, Homa-set values changed, customer details changed, case decided, case closed without a decision |
| `payment` | Customer pays for a request; payment provider reports a payment secured, succeeded or failed, a refund, a payout or a chargeback; provider adds a payout account; admin releases or keeps back a held payout; admin waives a fine (including a no-show fine when a serious flag came too close to the start to replace the person); Homa pays out released payments. IE reactions: request made, edited, declined, expired, cancelled or accepted; reservation cancelled; no-show reported, settled or decided; reservation completed; fine owed; business flagged |
| `resolution` | Admin views the queue; decides a case; IE reactions: report filed, no-show challenged, completion deadline missed, chargeback opened, flag taken over a reservation, flagged reservation restored or cancelled, payout held or decided, business flagged or cleared |
| `conversation` | Customer starts a chat; customer or provider sends a message; admin opens a chat for a case; IE reactions: enquiry opened, request made, enquiry closed |
| `feedback` | Customer rates a reservation; admin hides a rating; IE reactions: reservation completed, team named, face check passed, check-in without a face check |
| `discovery` | Guest or customer searches; views a provider page; views the showcase's searches filled in; a device records its first request (Q16); IE reactions: offering, availability, bookings, verification, ratings, Homa-set values and catalogue changes |
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
| `RequestSentToProviderIE` (its payment is secured, so the provider sees it) | reservation | notification |
| `RequestAcceptedIE` (the payment to complete, Q4) | reservation | payment |
| `ReservationReservedIE` (time and buffer) | reservation | payment, discovery, notification |
| `RequestDeclinedIE` (why, and whether it counts against the provider), `RequestExpiredIE`, `RequestCancelledIE` (why, including a failed payment) | reservation | payment, notification, admin tracking (response measures) |
| `TeamNamedIE`, `TeamReminderDueIE` | reservation | feedback, notification |
| `CheckedInIE` (the time, and whether the face check was unavailable), `FaceCheckPassedIE`, `FaceCheckFailedIE` | reservation | feedback, notification |
| `ReservationCancelledIE` (who, what the provider earns, any fine and its kind) | reservation | payment, discovery (cancellations), notification |
| `CustomerNoShowReportedIE`, `NoShowChallengedIE`, `NoShowFeeSettledIE` | reservation | payment, resolution, notification |
| `CompletionSubmittedIE`, `CompletionDeadlineMissedIE` | reservation | resolution, notification |
| `ReportFiledIE` | reservation | resolution, notification |
| `ReservationDisputedForFlagIE`, `FlaggedReservationRestoredIE` | reservation | resolution, payment, notification |
| `ReservationCompletedIE` (what the provider earns, the captured platform fee) | reservation | payment, feedback, discovery (jobs done), notification |
| `PaymentSecuredIE`, `PaymentSucceededIE`, `PaymentFailedIE` | payment | reservation, notification |
| `RefundIssuedIE`, `PayoutPaidIE`, `FineWaivedIE` | payment | notification |
| `PayoutAccountAddedIE` | payment | offering |
| `ChargebackOpenedIE` | payment | resolution |
| `PayoutHeldIE`, `HeldPayoutReleasedIE`, `HeldPayoutKeptBackIE` | payment | resolution, notification |
| `CaseOpenedIE`, `CaseDecidedIE`, `CaseClosedWithoutDecisionIE` | resolution | reservation, payment, notification |
| `MessageSentIE` | conversation | notification |
| `RatingsShownChangedIE` (shown averages and counts for a provider or team member) | feedback | discovery |
| `SearchPerformedIE`, `FirstRequestAfterSearchIE` (days since the device's first search, Q16) | discovery | admin tracking (later) |

Money events carry the platform fee percentage captured with the reservation's Homa-set values, so `payment` uses the value in force at request, not today's.

---

## 7. Quality and traceability

- **Rule to test.** Each domain test class names a spec feature, and each test method is the outcome in the spec's words (for example `aRequestExpiresAtTheResponseDeadlineWhenThatComesFirst`). `docs/traceability.md` lists, per context, every spec rule it owns with the test that covers it, and is updated with each slice.
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
| Rules invented to fill gaps | Every gap is an open question (Section 9), answered and written into the spec before the code |
| Words invented in the code | Every word the spec doesn't have is in Section 10, named with the product owner and added to the spec before it is used |

---

## 9. Questions for the product owner

Q1 to Q16 are **answered**: the product owner accepted the suggested answers below. Each is written into the spec before the context it blocks starts. Q17 is still **open**, with a suggested answer. Each answer is grounded in the spec's own words and its five product principles (P1 to P5).

| # | Question | Blocks | Suggested answer | Why |
|---|---|---|---|---|
| Q1 | Are the three cities fixed in the product (a code change to add one), or a Homa-set list the admins change? Can a customer register an address outside them? | kernel, account, discovery | Fixed in the product for the MVP. A customer's address must be in one of the three cities. Outside them, at sign-up or when a guest drops a pin, the app says Homa isn't there yet; a guest can still pick one of the three cities to browse | "Homa launches in three cities", and "three neighbouring cities have enough demand and supply" is an assumption to test, so opening a city is a launch decision, not a setting. Search "only shows providers who cover the city of the customer's address", so an account outside them could never book anyone (P3: nobody waits on something that can't happen) |
| Q2 | The spec says the SMS code is 6 digits, valid 5 minutes, and needs renewing after 5 wrong codes, and also that the OTP provider handles codes. Are these requirements on the OTP provider (nothing for Homa to enforce)? | account | Yes. The OTP provider generates, sends and checks the codes; the 6 digits, 5 minutes and 5 wrong codes move to its requirements under External Services, and apply to businesses as well as customers. The spec calls the same service both "OTP provider" and "SMS provider": use one name | "One time password by OTP provider ... Homa uses their results and does not build those checks itself" (Where Homa puts its effort). One word per concept (discipline, Section 7) |
| Q3 | With Instant book, if the time is no longer free when the payment is secured, the request "ends": is its status Declined or Cancelled? | reservation R3 | **Declined.** Its meaning widens to "the provider declined, accepted another booking that takes the same time, or the time was no longer free when Instant book checked it". Like the automatic declines in the Request Policy, it doesn't count against the provider's rates | Declined already covers "accepted another booking that takes the same time", the most common cause. Cancelled means someone chose to end it, or the customer's payment failed; using it here would tell the customer they did something wrong (Norman: the status should match how people think it happened) |
| Q4 | If the payment fails at acceptance, what happens to the reservation? | reservation R3, payment | **Decided by the product owner:** a reservation is **Reserved** when its payment succeeds. If it fails, the request ends **Cancelled** as a failed payment: nothing is taken, the customer is told and can request again. Other requests are declined for a clash only once the reservation is Reserved, so none is lost to a booking that didn't happen. Card, FPX and how each is authorised, charged, released or refunded are payment provider details, not Homa's rules | Cancelled already covers "a request whose payment failed", and a failed payment "ends straight away and nothing is charged. The customer is told and can request again" (Payment Protection). P2: the system sorts it out and tells people |
| Q5 | When an offering is paused or goes offline, or the business's verified-until date moves earlier than a request's end, do open requests end at once (P3), or stay until they expire? | reservation R2, R3 | One rule: **an open request ends at once, Declined, when the provider can no longer accept it**: the business has lapsed (including a serious flag), the job would now end after its verified-until date, or the request asks for more helpers than the team now has. These don't count against the provider's rates. Otherwise, pausing or going offline doesn't touch open requests: they keep the offering they were made with, and the provider can still accept or decline them until they expire | P3, whose stated trade-off is exactly "requests end sooner, sometimes without a person deciding", and P5. P1: a lapsed business isn't verified, so it takes no new work. "A business can ... accept only for a job that ends by its verified-until date." "The maximum cannot be more than the number of helpers on the team, so the provider can always name everyone who is coming." "A request already made keeps the offering it was made with" |
| Q6 | When two bookings were accepted with different buffers, which buffer must the gap between them respect: the existing booking's, the new one's, or the larger? | reservation R3, discovery | **The larger.** Each accepted reservation keeps its own buffer free around itself: an earlier one with the buffer it was accepted with, the new one with the buffer in force now | The buffer is "time kept free before and after each accepted reservation", and "changing it does not affect bookings already accepted". Respecting both is the only reading that keeps both promises. It protects the 5-star line, "arrives on time" |
| Q7 | For a close match by time, which start is suggested when several are free within 2 hours: the nearest, and on a tie the earlier? Does each provider appear at most once? | discovery | Each provider appears **once**: as an exact match if they are one, otherwise as one close match. For a time close match, suggest the start **nearest** the chosen time; on a tie, the **earlier** one | The customer chose a time, so the nearest start is the smallest change. One card per provider keeps a phone-sized list focused (Wroblewski: ruthless editing). Earlier on a tie is arbitrary but firm (P5) |
| Q8 | How are search results ordered? | discovery | Exact matches before close matches; close matches by time before close matches by jobs. Within each group: **Instant book first**, then by shown average stars, highest first, then providers still "New", then by lower provider price. Treated as an assumption, checked with launch data on the share of searches that lead to a reservation | Instant book is how "you know you're booked at once" (7 stars), and a Homa goal. Stars rank only where they are honestly shown (P4). A close match by time still does every chosen job at the same price, since "you are buying the provider's time". Trade-off: new providers start low, which may slow supply in a small launch; revisit after the first 8 weeks (Zhuo: diagnose with data) |
| Q9 | "Including its last minute": for a 10:15 limit, is 10:15:59 still in time? Is "up to 30 days ahead" a start no later than exactly 30 × 24 hours from now, or any time on the 30th day? | reservation, discovery, feedback | A limit given as a minute includes that **whole minute**: 10:15:59 is in time; 10:16:00 is not. The same reading applies to "up to and including the booked end" and the rating window. The booking horizon is by **calendar date in Malaysia**: any start on a date up to 30 days after today | The spec's own example: "for a 10:00 start, from 09:45 up to and including 10:15". People read clocks to the minute. A horizon by date gives the customer a simple 30-day calendar, with no cut-off that moves through the day (Norman: one clear conceptual model) |
| Q10 | Which moment is the recorded check-in time: the location check, or the first named person passing the face check? | reservation R5 | The moment the **first named person passes the face check**, or the moment the provider marks check-in when the face check is unavailable. The check-in deadline is judged on that same moment | "Check-in counts once the first named person passes" (Check-in Policy). The safety principle is a verified person at the door, not a phone near it. Trade-off: a face check still running at 10:16 is late; check-in opens at 09:45, which leaves 30 minutes |
| Q11 | Can the provider submit completion while a reservation is Disputed for a serious flag? | reservation R8, R9 | **No.** The spec already implies it: completion while Disputed is allowed only "after a report or a missed completion deadline", and a flag stops the team. Make it explicit. Evidence and a report filed before the flag stay with the reservation | P1: only verified people work, even if a booking is lost. A flagged job that has started can only be cancelled with a full refund, since restoring needs "the booked start hasn't come", so a completion would complete nothing |
| Q12 | A paused offering of a lapsed business: does it go live again on its own once the business passes re-verification? | offering | **Yes**, and the spec already implies it: paused means "takes no new requests until it passes", while only offline waits "until the provider goes live again". It goes live again only if it still meets every go-live condition; otherwise it goes offline, and the provider sees what is missing. Also make explicit that a flag the KYC provider clears ends that lapse the same way | P2: the system adjusts and tells people. The spec's own split between paused (ends by itself) and offline (the provider acts). A restored reservation "goes ahead as booked", so a cleared flag has to leave the business able to work |
| Q13 | A chargeback case is "decided with a release, a partial refund or a full refund": how does each map to "the provider is responsible" (recovered from payouts) or "Homa contests"? What if the payout was already paid out? | resolution, payment | A chargeback decision sets **what the provider finally earns**, like any case. **Release:** the provider isn't responsible and keeps their payout, and Homa contests. **Partial refund:** the provider is responsible for part. **Full refund:** the provider is responsible for all of it. Whatever the provider was paid above what they now earn is recovered from their next payouts the same way as a fine. That works whether or not the payout was paid out, since chargebacks come after the job. Homa's platform fee follows what the provider finally earns, and Homa bears the rest, including a chargeback it loses after contesting | "If Homa decides the provider is responsible, the amount is recovered from the provider's next payouts the same way as a fine; otherwise the provider keeps their payout and Homa contests." "Homa's platform fee follows what the provider finally earns", and is nothing on a full refund. One decision type for every case (P5) |
| Q14 | Do payouts from a Homa decision (release or partial refund) count as "completed jobs" that fines can be deducted from? | payment | **Yes**, when the provider is paid more than RM0. A full refund pays nothing, so there is nothing to deduct | The reservation is Completed, and "jobs done counts Completed reservations where the provider was paid more than RM0"; a full refund "doesn't count as a job done". Fines come off "the provider's next successful reservations" and "payouts for completed jobs only". One meaning of completed job everywhere (P5) |
| Q15 | Can the customer rate a named helper who failed the face check and didn't work? | feedback | **No.** The customer can rate the people who worked on the job: named and passed the face check, or checked in when the face check was unavailable. That includes someone who later stopped because they lapsed, since they did work. A person who never worked is reported, not rated | "A person who fails ... cannot work on that job". P4: honest records; a rating of someone who wasn't there is about something else. "Ratings and reports are separate" |
| Q16 | "First-time searchers who make a first reservation within 7 days" needs to link a search to a customer, but searches keep nothing that identifies the customer. How should this measure be taken? | discovery, admin tracking | Keep searches anonymous and measure by device. Each search record also says whether it was the **first search on that device**: that counts first-time searchers. The device keeps the date of its first search, and when it makes its first request, it sends a separate anonymous record of how many days that took, linked to no customer and no reservation: that counts who booked within 7 days. Labelled in the admin console as a device-based estimate | Searches keep "nothing that identifies the customer" (Privacy Policy), and the goal still needs a number. Zhuo: agree how success is measured before shipping. An honest estimate beats an exact number that collects personal data (P4) |

| Q17 | **Open.** How does a provider set their availability: hours that repeat each week, specific dates, or both? | availability, discovery | **Weekly hours** (one or more stretches per weekday), plus **time off** on dates the provider picks. One-off events come from the synced calendar | Providers are small businesses with regular working hours, so a weekly pattern is set once (Linear: one good default, no busywork). The spec already relies on calendar sync for "when a provider can actually work", which covers the exceptions |

## 10. New words for the product owner

The discipline names every concept in the spec's words, and a concept the spec has no word for is named with the product owner and added to the spec (Section 7). These are the words this plan adds. Each is approved, or renamed, and added to the spec before kickoff.

| Word | Context | Meaning |
|---|---|---|
| `TimeSlot` | kernel | A start and an end in time (the spec's "slot") |
| `MalaysiaTime` | kernel | Homa's time zone, and the Malaysian date of a moment |
| `ReservationTerms` | reservation | Everything captured at request that never changes: the offering's terms, the Homa-set values in force, the address and access notes |
| `ProviderBookings` | reservation | A provider's accepted reservations, each with its buffer, and a request whose payment is being completed |
| `ProviderStanding` | reservation | Reservation's copy of a business's verified-until date, lapse and flag |
| `PaymentSplit` | payment | How a payment is split: refund, what the provider earns, platform fee, payout (the spec's "when a payment is split") |
| `GoLiveChecklist` | offering | Everything a provider still needs before going live (the spec's "Homa lists everything still missing") |
| `SeriouslyFlaggedBusiness` | resolution | A flagged business's entry in the queue: its disputed reservations and held payouts |
| `AvailabilityTime` | availability | A time the provider can work (the spec's "availability times"), shaped by Q17 |
| `Notification` | notification | Telling one person one thing |

## 11. What comes after this plan

1. Before kickoff: the answers to Q1 to Q16 and the words in Section 10 go into the spec. Q4's decision also rewrites the spec's payment wording: How Homa Works (step 5), the payment states under Reservation Lifecycle, Payment Protection, the Request Policy's edit and refund lines and the Acceptance Policy's Instant book line say only that a payment is secured, succeeds, fails or is let go, and the card and FPX mechanics move to the payment provider's row under External Services; Q17 is answered before `availability` starts.
2. Phase 0: the build, `kernel/` and the gates.
3. Phase 1, step by step from Section 4, each context reviewed against the spec before the next starts.
4. Phase 2, then Phase 3 in the same order.
5. A plan for infra and `app/`, once the ports are fixed.
