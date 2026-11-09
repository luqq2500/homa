# Homa Domain Stories

> **Everyone deserves a well-cared home.**

These stories tell how Homa works, one business process at a time, the way the people involved would
tell it. They are written with **Domain Storytelling**: each story is a numbered sequence of sentences
of the form *actor – activity – work object – (to whom)*. Read in order, the sentences are the process.

The stories describe Homa **as it is built**: the to-be process in the product description
(`docs/homa-product-description.md`) and the rules in the domain code. Each story ends with where it
lives in the code: the bounded contexts, the domain methods and the integration events. That way a
reader can go from a sentence to the rule that enforces it.

## Contents

- How to read the stories
- Actors
- Work objects
- The cast and the running example
- Big picture
- Stories
  - 1. Homa sets up the job list, the tool list and the Homa-set values
  - 2. A customer registers
  - 3. An independent provider onboards
  - 4. A business provider onboards
  - 5. A provider adds a helper or a supervisor
  - 6. A provider sets up the listing and goes live
  - 7. A provider manages availability
  - 8. A customer searches and browses
  - 9. A customer enquires in chat and reserves from it
  - 10. A customer requests a reservation directly
  - 11. The provider answers the request
  - 12. The customer cancels a reserved booking
  - 13. The provider cancels a reserved booking
  - 14. The provider names who is coming
  - 15. Job day: check-in with location and face checks
  - 16. The customer is not there: a customer no-show
  - 17. The job is completed and the provider is paid
  - 18. Something went wrong: a report and Homa's decision
  - 19. The customer rates the provider and the team
  - 20. Re-verification every three months
  - 21. Homa waives a fine
- Story map: where each story lives in the code
- Not told here

## How to read the stories

| Notation | Meaning |
|---|---|
| **Bold** | An **actor**: a person, a role, or a system that does something |
| *Italics* | A *work object*: the thing the actor works with, such as a *request* or a *face check* |
| `1.`, `2.`, … | The order of the activities. A sentence can start a branch, shown under **Variants** |
| "Homa" | The Homa platform acting on its own: checking a rule, sending a reminder, or running a scheduled step. When the founders act, the actor is **Homa founders** |
| A **bold** name inside a sentence, such as **request expiry time** | A Homa-set value from the product description. The launch value is shown next to it |

Each story has the same parts:
- **Goal**: why the story happens.
- **Story**: the numbered sentences.
- **Variants**: the other ways it can go.
- **Rules**: what the domain enforces.
- **In the code**: contexts, methods and integration events.

A story is **fine-grained** when it shows one process in detail, and **coarse-grained** when it shows
the whole journey; the big picture is coarse-grained, the rest are fine-grained.

## Actors

| Actor | Who they are |
|---|---|
| **Customer** | A homeowner who searches, reserves, pays, chats, reports and rates. Registers with a Malaysian phone number |
| **Provider** | A provider who sells their team's time. Either an **independent provider** (a Malaysian individual) or a **business provider** (a business registered with SSM, acting through its owner or director) |
| **Helper** | A team member who does the booked jobs. The customer books and pays for helpers. Has no account |
| **Supervisor** | A team member in charge on site. Not counted or charged as a helper. Has no account |
| **Homa** | The platform: applies the rules, sends reminders, and runs scheduled steps such as expiring requests and closing dispute windows |
| **Homa founders** | The people behind the admin console: they curate the lists and values, decide cases and waive fines |
| **eKYC provider** (Kenal) | Verifies a person's identity document and face, and matches faces at check-in |
| **eKYB provider** (to be chosen) | Verifies a business's SSM registration and its owner's or director's eKYC in one flow |
| **Stripe** | Authorises and charges cards, takes FPX payments, holds payments, pays out and refunds |
| **SMS provider** | Sends the customer's phone verification code |
| **Calendar service** | Google Calendar or Apple Calendar, synced into a provider's availability |
| **Geoservice** | Turns addresses into locations and measures the distance at check-in |

Homa's rules never depend on which eKYC, eKYB or payment provider is used.

## Work objects

| Work object | What it is | Context |
|---|---|---|
| *job list*, *tool list* | Homa's lists that providers choose from; retired items are never deleted | `catalogue` |
| *service area* | The launch cities: Salak Tinggi, Cyberjaya and Putrajaya | `catalogue` |
| *shelf* | A curated homepage row driven by a preset search | `catalogue` |
| *Homa-set values* | The values the policies use, in versions | `policy` |
| *phone verification* | A 6-digit SMS code, valid 5 minutes, 5 wrong attempts allowed | `account` |
| *customer account* | Name, phone number and address with its city | `account` |
| *provider account* | Type, verification, payout account readiness | `account` |
| *eKYC result*, *eKYB result* | Verified (with the document read) or rejected | `account` |
| *add form* | Role, document type, issuing country, profile photo and the lawful-employment confirmation for one person | `account` (`MemberVerification`) |
| *team member* | A verified helper or supervisor, with a registered name and a due date | `account` |
| *re-check* | A re-verification that is open before a person's due date | `account` |
| *listing* | What a provider sells: jobs, tools, price, hours, helpers, cities, tier, team, supervisor rule | `offering` |
| *supervisor rule* | When a supervisor is sent: from a number of helpers, of hours, or both | `offering` |
| *calendar* | Availability windows, synced busy blocks, holds for accepted jobs, and a buffer | `availability` |
| *search* | Jobs, start, hours, helpers and city | `discovery` |
| *match list* | Exact matches, close matches, and why | `discovery` |
| *enquiry* | Booking details to chat about before requesting; has no payment | `reservation` |
| *chat* | One per enquiry or reservation | `conversation` |
| *request*, *reservation* | The booking, from Requested to its end | `reservation` |
| *price* | Hours fee, travel fee, provider price, service fee and total | `reservation` |
| *payment* | A card authorisation or an upfront FPX payment, then held, released or refunded | `payment` |
| *team assignment* | The helpers and any supervisors named for a reservation | `reservation` |
| *location check*, *face check* | The two steps of check-in | `reservation` |
| *completion* | Photo or video evidence that the job is done | `reservation` |
| *report*, *case*, *decision* | A problem, Homa's queue entry for it, and the outcome | `reservation`, `resolution` |
| *settlement* | Who gets what of the total: provider, Homa and customer | `reservation` |
| *fine*, *payout* | A provider's fine, and a payout with fine deductions | `payment` |
| *rating*, *rating summary* | A customer's stars and feedback, and the averages shown | `feedback` |

## The cast and the running example

The stories follow one booking from start to end.

| Who | In the stories |
|---|---|
| **Sarah** | A customer in Cyberjaya |
| **Kemas Home Care** | A provider. As an independent provider, it is run by **Nur Aina**. As a business, it is **Kemas Home Care Sdn Bhd**, with Nur Aina as director |
| **Aina** and **Ben** | Helpers on the Kemas team |
| **Chan** | A third helper |
| **Siti** | A supervisor on the Kemas team |

The booking: **kitchen care and laundry folding, Saturday 10:00, 3 hours, 2 helpers, in Cyberjaya.**
Kemas charges RM30 per helper per hour, with a separate RM20 travel fee, on the **Standard**
cancellation tier.

| Price part | Amount |
|---|---|
| Hours fee: RM30 × 2 helpers × 3 hours | RM180.00 |
| Travel fee | RM20.00 |
| **Provider price** | **RM200.00** |
| Service fee: 15% of the provider price | RM30.00 |
| **Total Sarah pays** | **RM230.00** |

## Big picture

The whole journey, coarse-grained. Each step is told in detail by the story in brackets.

1. **Homa founders** set up the *job list*, the *tool list* and the *Homa-set values*. (1)
2. **Sarah** registers a *customer account* with her phone number. (2)
3. **Nur Aina** registers *Kemas Home Care* as a provider and verifies through eKYC (or eKYB for a business). (3, 4)
4. **Nur Aina** adds *Aina*, *Ben* and *Siti* to the team; each verifies on the spot. (5)
5. **Nur Aina** sets up the *listing* and takes it live. (6)
6. **Nur Aina** keeps the *calendar* up to date. (7)
7. **Sarah** searches and finds *Kemas* in the *match list*. (8)
8. **Sarah** chats in an *enquiry*, or requests directly, and pays by card or FPX. (9, 10)
9. **Nur Aina** accepts the *request*. (11)
10. **Nur Aina** names *Aina* and *Ben* for the job. (14)
11. On Saturday, **the team** checks in: the *location check*, then each person's *face check*. (15)
12. **The team** does the job, and **Nur Aina** submits the *completion* evidence. (17)
13. **Homa** closes the *dispute window* and releases the *payout* to Kemas. (17)
14. **Sarah** rates *Kemas*, *Aina* and *Ben*. (19)
15. Every three months, **Kemas** and its team **re-verify**. (20)

What can go wrong along the way: cancellations (12, 13), a customer no-show (16), a report and a
decision (18), and a waived fine (21).

```mermaid
flowchart LR
    F([Homa founders]) -- "1 set up lists and values" --> H[[Homa]]
    S([Sarah, customer]) -- "2 registers" --> H
    P([Nur Aina, provider]) -- "3 verifies" --> V([eKYC or eKYB provider])
    P -- "4 adds team, 5 goes live" --> L[listing]
    S -- "7 searches, 8 requests" --> R[request]
    R -- "9 accepted by" --> P
    T([Aina and Ben, helpers]) -- "11 check in with face checks" --> R
    P -- "12 submits completion" --> R
    H -- "13 releases payout" --> P
    S -- "14 rates" --> P
```

## Stories

### 1. Homa sets up the job list, the tool list and the Homa-set values

**Goal:** every provider chooses from the same lists, so search can match jobs exactly, and every
reservation runs on known values.

1. **Homa founders** add *jobs* to the *job list*, each in a group such as "kitchen" or "living room".
2. **Homa founders** add *tools* to the *tool list*.
3. **Homa founders** create *shelves* for the homepage, each with a preset search.
4. **Homa founders** launch *Homa-set values* version 1.
5. Later, **Homa founders** change one *Homa-set value*, such as the **fee window**.
6. **Homa** keeps the old version for every reservation already requested, and applies the new
   version to new reservations only.

**Variants**
- 1a. **Homa founders** rename or retire a *job*. A retired job stays in the list, is never deleted,
  and its code is never reused. Providers can no longer pick it.
- 5a. **Homa** refuses a change that breaks a rule across values: Flexible must stay free, no
  cancellation fee may be higher than the customer no-show fee, and the provider no-show fine must stay
  the highest fine.

**Rules**
- The launch values include: service fee 15%; 1 to 6 hours; minimum notice 2 hours; booking horizon
  30 days; request expiry 24 hours; fee window 24 hours; tiers 0%, 10% and 20%; provider fines 20% and
  30%; fine deduction cap 50%; team reminder 6 hours; earliest check-in and check-in period 15 minutes;
  check-in radius 300 metres; face check attempts 3; customer no-show wait 15 minutes; no-show fee
  travel + 30%; no-show report window 1 hour; completion reminder 30 minutes; completion deadline
  24 hours; dispute window extension 1 hour; evidence retention 7 days; re-verification every
  3 months, with a 14-day reminder and a 14-day early re-check deadline; rating window 14 days;
  minimum ratings to show 3; and the accepted documents list.
- Homa sets no limit on helpers.

**In the code:** `catalogue` (`JobList.add`, `rename`, `retire`; `ToolList`; `Shelf.create`), `policy`
(`HomaPolicy.initial`, `HomaPolicy.change`). Events: `JobAddedIE`, `JobRetiredIE`, `ToolAddedIE`,
`ToolRetiredIE`, `HomaPolicyChangedIE`.

### 2. A customer registers

**Goal:** Sarah can sign in, providers see who the booking is for, and Homa knows her city.

1. **Sarah** enters her *phone number* in the Homa app.
2. **Homa** asks the **SMS provider** to send a 6-digit *verification code* to Sarah.
3. **Sarah** enters the *code*.
4. **Homa** confirms the *phone verification*.
5. **Sarah** enters her *name* and *address*.
6. **Homa** asks the **geoservice** for the address's *location* and *city*.
7. **Homa** registers Sarah's *customer account*.

**Variants**
- 3a. The *code* is wrong: **Homa** refuses it. After 5 wrong codes, Sarah asks for a new one.
- 3b. The *code* has expired after 5 minutes: **Homa** refuses it.
- 7a. **Sarah** later changes her *address*.

**Rules:** a Malaysian mobile number; registration only with a confirmed phone.

**In the code:** `account` (`PhoneVerification.start`, `confirm`; `CustomerAccount.register`,
`changeAddress`). Event: `CustomerRegisteredIE`.

### 3. An independent provider onboards

**Goal:** Nur Aina sells Kemas Home Care's time on Homa as an individual.

1. **Nur Aina** registers a *provider account* with a confirmed phone number, choosing the type
   **independent**.
2. **Nur Aina** chooses her *identity document*: a MyKad or a Malaysian passport.
3. **Nur Aina** completes *eKYC* with the **eKYC provider**: she scans her document and takes a live
   selfie.
4. The **eKYC provider** reports a *verified result* with the *document* it read.
5. **Homa** records Nur Aina's *registered name* from the document and sets her *due date* 3 months
   ahead.
6. **Homa** makes Nur Aina a *helper* on her own *team*, by default.
7. **Nur Aina** adds a *payout account* (a personal bank account) with **Stripe**.
8. **Stripe** reports the *payout account* as ready.

**Variants**
- 4a. The **eKYC provider** reports a *rejected result*: **Homa** asks Nur Aina to redo it, and she
  can pass on a later attempt.
- 4b. The document is not Malaysian: **Homa** refuses the result.
- 6a. **Nur Aina** removes herself from her team, for example when she runs a team and does not work
  on site.

**Rules:** the registered name comes only from the eKYC result; nobody types or changes it. A provider
is ready for go-live once verified, not lapsed, and with a ready payout account.

**In the code:** `account` (`ProviderAccount.register`, `recordEkycResult`, `includeSelfAsHelper`,
`recordPayoutAccountReady`, `isReadyForGoLive`). Events: `ProviderRegisteredIE`, `ProviderVerifiedIE`,
`ProviderVerificationRejectedIE`, `PayoutAccountReadyIE`.

### 4. A business provider onboards

**Goal:** Kemas Home Care Sdn Bhd sells its team's time on Homa as a registered business.

1. **Nur Aina** registers a *provider account* for the business, choosing the type **business**.
2. **Nur Aina** starts *eKYB* with the **eKYB provider**.
3. The **eKYB provider** checks the business's *SSM registration*.
4. **Nur Aina**, as director, completes the *eKYC* within the same eKYB flow.
5. The **eKYB provider** reports a *verified result* with the *SSM number*, the *registered business
   name* and the director's *document*.
6. **Homa** records the result and sets the business's *due date* 3 months ahead. Homa does no check of
   its own.
7. **Homa** starts the business with **no team members**.
8. **Nur Aina** adds a *payout account* (a business bank account) with **Stripe**, and **Stripe**
   reports it ready.

**Variants**
- 5a. The **eKYB provider** reports a *rejected result*: the business redoes eKYB.
- 5b. The director's document is not Malaysian: **Homa** refuses the result.
- 7a. Nur Aina works on site too: she adds herself as a *team member*, like anyone else (story 5).

**Rules:** a business verifies through eKYB only, never a separate eKYC; a business is never a helper
on its own team. The provider page shows the profile's display name; the registered business name is
kept for Homa's records.

**In the code:** `account` (`ProviderAccount.register`, `recordEkybResult`; `EkybResult`,
`BusinessRegistration`). Events: `ProviderRegisteredIE`, `ProviderVerifiedIE`,
`ProviderVerificationRejectedIE`, `PayoutAccountReadyIE`.

### 5. A provider adds a helper or a supervisor

**Goal:** only verified people enter a customer's home. Aina joins the Kemas team as a helper.

1. **Nur Aina** fills in the *add form* in the provider app: the role **helper**, the document type
   **identity card**, and Aina's *profile photo*. For a business, she also chooses the *issuing
   country*.
2. **Nur Aina** ticks the *lawful-employment confirmation*: Aina is lawfully employed by or engaged with
   Kemas and allowed to work in Malaysia, and Kemas is responsible for it.
3. **Homa** records the *confirmation* with its time and the version of the terms, and shows Aina as
   *waiting for verification*.
4. **Aina**, with Nur Aina, scans her own *MyKad* on Nur Aina's phone and takes her own live *selfie*.
5. The **eKYC provider** reports a *verified result* with the *registered name* "Aina binti Ahmad".
6. **Homa** adds Aina to the *team* as a *helper*, with her *due date* 3 months ahead.
7. The **eKYC provider** matches Aina's *profile photo* against her eKYC once.

**Variants**
- 2a. **Nur Aina** does not tick the confirmation: the form cannot be sent.
- 1a. An independent provider chooses a foreign document, or anyone chooses a document outside the
  *accepted documents* list: **Homa** refuses the form.
- 5a. The **eKYC provider** reports a *rejected result*, or the session times out or is abandoned:
  nothing is added, and **Nur Aina** starts again with a new form.
- 5b. The document read is not the type or country chosen on the form: **Homa** refuses the result.
- 6a. **Nur Aina** adds **Siti** the same way, with the role **supervisor**.
- 6b. Later, **Nur Aina** removes a *team member*, or **Homa founders** remove one who is not allowed to
  work. A removed person cannot come back.

```mermaid
flowchart LR
    P([Nur Aina, provider]) -- "1 fills in, 2 confirms" --> F[add form]
    F -- "3 waiting for verification" --> H[[Homa]]
    A([Aina]) -- "4 scans MyKad, takes selfie on Nur Aina's phone" --> K([eKYC provider])
    K -- "5 verified, registered name" --> H
    H -- "6 adds as helper" --> T[team]
```

**Rules**
- There is no Homa time limit; the eKYC provider ends an unfinished session.
- A waiting person is not on the team, not shown to customers, and cannot be named for jobs.
- Teams have no size limit.

**In the code:** `account` (`MemberVerification.start`, `recordEkycResult`; `TeamMember.add`,
`recordPhotoMatch`, `remove`). Events: `MemberVerificationStartedIE`, `MemberVerificationRejectedIE`,
`TeamMemberAddedIE`, `TeamMemberRemovedIE`.

### 6. A provider sets up the listing and goes live

**Goal:** Kemas appears in search and can take requests.

1. **Nur Aina** drafts a *listing* for Kemas.
2. **Nur Aina** sets the *profile*: the display name "Kemas Home Care" and a picture.
3. **Nur Aina** chooses *jobs* from Homa's *job list* (kitchen care, laundry folding) and *tools* from
   the *tool list* (a steam mop).
4. **Nur Aina** sets the *pricing*: RM30 per helper per hour and a separate RM20 travel fee.
5. **Nur Aina** sets the *hours*, 2 to 4, within Homa's 1 to 6.
6. **Nur Aina** sets the *helpers range*, 1 to 3, at most the helpers on her team.
7. **Nur Aina** sets the *supervisor rule*: a supervisor comes from 3 helpers or from 4 hours.
8. **Nur Aina** chooses the *cities* she covers (Cyberjaya) and the *Standard* cancellation tier.
9. **Nur Aina** takes the *listing* live.
10. **Homa** checks the *listing* and Nur Aina's *provider account*, and makes the listing live.

**Variants**
- 10a. Something is missing: **Homa** lists everything still missing, such as the profile picture or
  the cities, and the listing stays a draft.
- 7a. Kemas has no supervisor on the team: **Homa** refuses the supervisor rule.
- 6a. A helper leaves the team: **Homa** shrinks the *helpers range* to the helpers left. With no helper
  left, or with a supervisor rule and no supervisor left, **Homa** takes the listing offline.
- 10b. **Nur Aina** changes the *listing* later: the change applies to new requests only.

**Rules**
- Go-live needs a complete listing; a verified, not lapsed provider (eKYC, or eKYB for a business) with
  a ready payout account; enough helpers; and a supervisor when a rule is set.
- People still waiting for verification never block going live. Supervisors never count towards the
  helpers range, and are not charged.

**In the code:** `offering` (`ProviderListing.draft`, `setProfile`, `offerJobs`, `bringTools`,
`setPricing`, `setHours`, `setHelpers`, `setSupervisorRule`, `coverCities`, `chooseCancellationTier`,
`updateRoster`, `goLive`, `missingForGoLive`). Events: `ListingWentLiveIE`, `ListingChangedIE`,
`ListingWentOfflineIE`.

### 7. A provider manages availability

**Goal:** search shows Kemas only when the team can actually work.

1. **Nur Aina** sets *availability windows* in her *calendar*.
2. **Nur Aina** connects her **calendar service**.
3. The **calendar service** syncs her events into the *calendar* as *busy blocks*.
4. **Nur Aina** sets a *buffer* of 30 minutes between jobs, for travel.
5. When a request is accepted, **Homa** holds the *slot* in the *calendar*.
6. When an accepted job is cancelled, **Homa** releases the *slot*.

**Variants**
- 5a. A slot overlaps another hold, or falls inside the buffer around one: **Homa** refuses to hold it.

**Rules**
- A slot is free when it is inside a window, with no busy block or hold over it.
- Back-to-back windows count as one stretch, and an event ending exactly at a slot's start does not
  block it.
- The buffer applies around Homa jobs only, not synced events, and changing it does not affect jobs
  already accepted.

**In the code:** `availability` (`ProviderCalendar.setWindows`, `replaceBusyBlocks`,
`setBufferBetweenJobs`, `hold`, `release`, `isAvailable`, `nearestAvailableStart`). It listens to
`ReservationAcceptedIE` and `ReservationCancelledIE`.

### 8. A customer searches and browses

**Goal:** Sarah finds a provider for Saturday.

1. **Sarah** picks the *jobs* (kitchen care, laundry folding), a *start* (Saturday 10:00), the *hours*
   (3) and the *helpers* (2).
2. **Homa** checks the *search*: at least 1 helper, 1 to 6 hours, a quarter-hour start between 2 hours
   and 30 days ahead.
3. **Homa** asks each provider's *calendar* whether they are free, or free near the time.
4. **Homa** sorts providers in Sarah's city into a *match list*: exact matches and close matches.
5. **Sarah** opens the *provider page* for Kemas: jobs, tools it brings and doesn't, price, cancellation
   policy in words, provider type, team (registered names, roles, photos, average stars), when a
   supervisor is included, ratings, and the Verified ID badge.

**Variants**
- 4a. A **close match** offers only some of the jobs, or is free for the same duration at a start up to
  2 hours before or after.
- 4b. A provider is **no match** when they don't cover the city, the helpers or hours are outside their
  range, they offer none of the jobs, they aren't free at or near the time, or the job would end after
  their re-verification due date.
- 4c. A *paused* listing is left out of search.
- 1a. **Sarah** starts from a homepage *shelf* instead, with its preset search.

**In the code:** `discovery` (`SearchCriteria.of`, `MatchClassifier.classify`, `ProviderCandidate`),
with `availability` answering free times. It keeps listings from the listing events,
`HomaPolicyChangedIE`, and providers' due dates from `ProviderVerifiedIE`, `ProviderReverifiedIE` and
`RecheckOpenedIE`.

### 9. A customer enquires in chat and reserves from it

**Goal:** Sarah asks Kemas a question before she commits.

1. **Sarah** picks the booking details on the Kemas page and taps **Chat**.
2. **Homa** creates an *enquiry* with its own *chat*. The enquiry has no payment.
3. **Sarah** asks in the *chat* whether Kemas brings a vacuum.
4. **Nur Aina** replies in the *chat*.
5. **Sarah** taps **Reserve** in the *chat*.
6. **Homa** opens the *review page*, filled in with the enquiry details.
7. **Sarah** edits the details if needed, and submits the *request* (story 10).
8. **Homa** links the *chat* to the new *reservation*; the same chat carries on.

**Variants**
- 7a. **Sarah** never requests: **Homa** closes the *enquiry* when its date passes, and archives the
  *chat*, which stays readable.
- 1a. **Sarah** books again from a past *chat*: **Homa** creates a new *enquiry* with the same details at
  a new start.

**Rules:** only the customer starts a chat; the provider replies. Enquiry details are edited only on
the review page. Every chat reminds both sides that payments and messages outside Homa are not covered.

**In the code:** `reservation` (`Enquiry.create`, `bookAgainFrom`, `isClosed`, `markRequested`),
`conversation` (`Conversation.startByCustomer`, `post`, `linkReservation`, `archive`). Events:
`EnquiryCreatedIE`, `EnquiryRequestedIE`, `EnquiryClosedIE`.

### 10. A customer requests a reservation directly

**Goal:** Sarah books Kemas for Saturday.

1. **Sarah** checks the *review page*: the *price* (RM230.00 in total) and the *cancellation terms*
   ("Cancel after Fri 10:00 am: you pay RM18. Before that: free").
2. **Sarah** taps **Reserve** and pays by **card**.
3. **Stripe** authorises RM230.00 on Sarah's card. Nothing is charged yet.
4. **Homa** creates the *request*, in **Requested**, keeping the *offering* and the *Homa-set values*
   in force now.
5. **Homa** notifies **Nur Aina** of the *request*.

**Variants**
- 2a. **Sarah** pays by **FPX**: **Stripe** takes RM230.00 upfront, and Homa holds it.
- 4a. **Homa** refuses the request when: the start is not on a quarter hour, is under 2 hours away or
  beyond 30 days; the hours or helpers are outside Kemas's range; a job isn't offered; the city isn't
  covered; the listing is paused; or the job would end after Kemas's re-verification due date.
- 6. **Sarah** edits the *request* before it is accepted: the card authorisation is updated to the new
  total, or for FPX she pays the increase or is refunded the decrease. The expiry still runs from the
  first request.
- 7. **Sarah** cancels the *request* before Kemas answers: it is free, the authorisation is released or
  the FPX payment refunded in full.

**In the code:** `reservation` (`Reservation.request`, `price`, `cancellationTerms`, `edit`,
`cancelByCustomer`), `payment` (`Payment.authoriseCard`, `payUpfrontByFpx`, `adjustTotal`,
`cancelBeforeAcceptance`). Events: `ReservationRequestedIE`, `ReservationEditedIE`,
`ReservationCancelledIE`.

### 11. The provider answers the request

**Goal:** Kemas confirms Sarah's booking.

1. **Nur Aina** opens the *request*.
2. **Nur Aina** accepts it.
3. **Homa** checks that the request has not expired, that the listing is not paused, and that the job
   ends by Kemas's re-verification due date as it is now.
4. **Homa** makes the *reservation* **Reserved**.
5. **Stripe** charges Sarah's card the RM230.00 and Homa holds it.
6. **Homa** holds the *slot* in Kemas's *calendar*.

**Variants**
- 2a. **Nur Aina** declines: the reservation is **Declined**, and the authorisation is released or the
  FPX payment refunded in full.
- 2b. **Nur Aina** doesn't answer: at the **request expiry time** (24 hours after the request, or the
  booked start if sooner), **Homa** expires it with a full release or refund.
- 3a. Kemas's listing is paused because its re-verification lapsed: Nur Aina cannot accept. The request
  stays Requested and expires normally, unless Kemas re-verifies in time.
- 3b. An early re-check moved Kemas's due date before the job ends: Nur Aina cannot accept until she
  passes the re-check.

**In the code:** `reservation` (`Reservation.accept`, `decline`, `expire`), `payment`
(`Payment.capture`, `cancelBeforeAcceptance`), `availability` (`hold`). Events:
`ReservationAcceptedIE`, `ReservationDeclinedIE`, `ReservationExpiredIE`, `PaymentCapturedIE`,
`PaymentRefundedIE`. The paused-listing check is required usecase rule R2.

### 12. The customer cancels a reserved booking

**Goal:** Sarah's plans changed.

1. **Sarah** cancels the *reservation* on Thursday.
2. **Homa** applies Kemas's *cancellation tier*: before the **fee window** opens (24 hours before the
   start), it is free.
3. **Homa** settles the *payment*: a full refund of RM230.00.
4. **Homa** releases the *slot* in Kemas's *calendar*.

**Variants**
- 1a. **Sarah** cancels on Friday at 11:00, inside the fee window: on Standard, Kemas receives 10% of
  the hours fee, RM18.00, released straight away; the travel fee is refunded, and the service fee
  follows what Kemas receives.
- 1b. On **Flexible**, cancelling before the start is always free. On **Firm**, the fee is 20% of the
  hours fee.
- 1c. With the travel fee included in the price, the percentage applies to the full provider price.
- 1d. **Sarah** tries to cancel during the check-in period before check-in: **Homa** refuses until the
  team checks in or the period ends.
- 1e. **Sarah** cancels after the team checked in: it counts as a customer no-show (story 16).

**In the code:** `reservation` (`Reservation.cancelByCustomer`, `cancellationTerms`), `payment`
(`Payment.settle`). Events: `ReservationCancelledIE`, `SettlementDecidedIE`, `PaymentRefundedIE`.

### 13. The provider cancels a reserved booking

**Goal:** Kemas cannot do the job.

1. **Nur Aina** cancels the *reservation*.
2. **Homa** refunds Sarah in full: RM230.00.
3. **Homa** works out Kemas's *fine* from the timing alone.
4. **Homa** records the *fine*, to be deducted from Kemas's next payouts (story 17).

**Variants**
- 3a. More than 24 hours before the start: no fine.
- 3b. Within 24 hours before the start: a *late cancellation fine* of 20% of the hours fee, RM36.00.
- 3c. After the booked start, or after checking in: a *no-show fine* of 30%, RM54.00, the highest fine.
- 3d. The fine is the same on every tier. With travel included, the percentage applies to the provider
  price.

**In the code:** `reservation` (`Reservation.cancelByProvider` returns the settlement and the fine
charge), `payment` (`Fine.charge`). Events: `ReservationCancelledIE`, `SettlementDecidedIE`,
`FineChargedIE`.

### 14. The provider names who is coming

**Goal:** Sarah knows who will enter her home before they arrive.

1. At the **team reminder time** (6 hours before the start), **Homa** reminds **Nur Aina** to name the
   team, if she hasn't yet.
2. **Nur Aina** names *Aina* and *Ben* as the *helpers* for the reservation.
3. **Homa** checks the *team assignment* against the *team*.
4. **Homa** shows **Sarah** each person's registered name, role, photo and average stars.

**Variants**
- 3a. The number of helpers named doesn't match the 2 booked: **Homa** refuses.
- 3b. A helper isn't a helper on the team, or a supervisor isn't a supervisor on it: **Homa** refuses.
- 3c. The booking reaches the *supervisor rule* (3 helpers, or 4 hours): **Nur Aina** must also name
  *Siti*, or **Homa** refuses.
- 3d. The same person is named twice: **Homa** refuses.
- 2a. **Nur Aina** changes the team before the start: **Homa** notifies Sarah. From the booked start,
  the team is locked.
- 2b. A named team member lapses: **Nur Aina** must name someone else before the start.

**In the code:** `reservation` (`Reservation.nameTeam`, `isTeamReminderDue`; `TeamAssignment`,
`TeamRoster`, `SupervisorRule`). Event: `TeamNamedIE`.

### 15. Job day: check-in with location and face checks

**Goal:** only the verified people Sarah was shown work in her home.

1. At 09:58, **Aina** opens the *check-in* on the provider app at Sarah's door.
2. **Homa** asks the **geoservice** for the distance: 120 metres, inside the 300-metre radius.
3. **Homa** records the *location check*. Check-in is not complete yet.
4. **Aina** takes a live *selfie*.
5. The **eKYC provider** matches it against Aina's eKYC: **passed**.
6. **Homa** records Aina's *face check* and completes the *check-in* at 09:59.
7. **Homa** shows **Sarah** the check-in time, and each person's result and time.
8. **Ben** takes his *selfie*, and the **eKYC provider** passes it.
9. **Aina** and **Ben** start the job.

```mermaid
flowchart LR
    A([Aina, helper]) -- "1 opens check-in" --> H[[Homa]]
    H -- "2 distance?" --> G([Geoservice])
    H -- "3 location check" --> R[reservation]
    A -- "4 selfie" --> K([eKYC provider])
    K -- "5 passed" --> H
    H -- "6 checked in, 7 shows results" --> S([Sarah, customer])
    B([Ben, helper]) -- "8 selfie" --> K
```

**Variants**
- 1a. Before 09:45 (the **earliest check-in**) or after 10:15 (the end of the **check-in period**):
  **Homa** refuses.
- 2a. More than 300 metres away: **Homa** refuses.
- 1b. The team isn't named: check-in isn't enabled.
- 5a. A face check **fails**: the person tries again. After 3 failed attempts they cannot work on this
  job, and **Homa** tells Sarah.
- 5b. The face check service is unavailable: **Homa** completes check-in on location only, marked "face
  check unavailable" for Sarah. Everyone named can work, except someone who already failed every
  attempt, and Kemas is not fined.
- 6a. No face check passes, and the service is not down, by 10:15: check-in is not complete, and at
  10:16 **Homa** marks a **provider no-show**. Sarah is refunded in full, and Kemas pays a fine of 30% of
  the hours fee.

**Rules:** Homa uses the device's location only at check-in, never continuously, and keeps only each
face check's result and time, never the selfie.

**In the code:** `reservation` (`Reservation.checkIn`, `recordFaceCheck`, `recordFaceCheckUnavailable`,
`isCheckedIn`, `canWork`, `markProviderNoShow`). Events: `ProviderCheckedInIE`, `FaceCheckRecordedIE`,
`CheckInCompletedIE`, `ReservationCancelledIE`, `FineChargedIE`.

### 16. The customer is not there: a customer no-show

**Goal:** Kemas is paid for coming when Sarah isn't home.

1. **Aina** checks in at 10:00 (story 15). Nobody answers the door.
2. After the **customer no-show wait** (15 minutes from the later of the start and the check-in),
   **Nur Aina** reports a *customer no-show* at 10:15.
3. **Homa** cancels the *reservation* and settles it: Kemas receives the *customer no-show fee*, the
   travel fee plus 30% of the hours fee, RM74.00; Sarah is refunded the rest.
4. **Homa** notifies **Sarah**.
5. The **no-show report window** (1 hour) ends without a challenge.
6. **Homa** releases the fee to Kemas and the refund to Sarah.

**Variants**
- 2a. **Nur Aina** reports before the wait ends: **Homa** refuses.
- 5a. **Sarah** challenges the no-show within the hour: the reservation becomes **Disputed**, and the
  money waits for Homa's decision (story 18). After the decision, it ends **Cancelled**.
- 1a. **Sarah** cancels after the team checked in: it counts as a customer no-show, even before the
  start.

**In the code:** `reservation` (`Reservation.reportCustomerNoShow`, `challengeNoShow`), `payment`
(`Payment.settle`). Events: `ReservationCancelledIE`, `SettlementDecidedIE`, `NoShowChallengedIE`.

### 17. The job is completed and the provider is paid

**Goal:** Sarah gets her home cared for, and Kemas gets paid.

1. **Aina** and **Ben** finish at 13:00.
2. **Nur Aina** submits the *completion* with *photo evidence*.
3. **Homa** notifies **Sarah** that the job is completed.
4. The *dispute window* runs until 1 hour after the later of the booked end and the completion: 14:00.
5. No *report* is filed. **Homa** closes the *dispute window* and the reservation becomes **Completed**.
6. **Homa** settles the *payment*: Kemas earns the provider price, RM200.00, and Homa keeps the service
   fee.
7. **Homa** creates a *payout* for this reservation, deducting any unpaid *fine* first.
8. **Stripe** pays out to Kemas's bank account.

**Variants**
- 2a. **Nur Aina** hasn't submitted by 13:30 (the **completion reminder time**): **Homa** reminds her.
- 2b. Still nothing by the **completion deadline** (24 hours after the end): **Homa** moves the
  reservation to **Disputed** and reviews it (story 18).
- 7a. Kemas has a RM54.00 fine outstanding: at most 50% of the payout, rounded in Kemas's favour, is
  deducted, oldest fine first, so Kemas always takes home part of every job. Fines come off payouts for
  completed jobs only, never off a cancellation fee or no-show fee payout.

**Rules:** there is no automatic completion; after completion, nobody can cancel, and any problem goes
through a report. Evidence is kept for 7 days after the reservation is resolved.

**In the code:** `reservation` (`Reservation.submitCompletion`, `isCompletionReminderDue`,
`missCompletionDeadline`, `closeDisputeWindow`), `payment` (`Payment.settle`, `Payout.create`,
`FineDeductionPolicy.apply`). Events: `CompletionSubmittedIE`, `ReservationCompletedIE`,
`SettlementDecidedIE`, `PayoutCreatedIE`, `CompletionDeadlineMissedIE`.

### 18. Something went wrong: a report and Homa's decision

**Goal:** Homa decides fairly, on reports only.

1. At 12:00, **Sarah** files a *report*: a problem during the job, "only one helper came", with a photo.
2. **Homa** moves the reservation to **Disputed** and stops the release of the payment.
3. **Homa** opens a *case* in the founders' queue.
4. **Nur Aina** files her own *report* while the case is open.
5. **Homa** adds Nur Aina's report and evidence to the same *case*.
6. **Homa founders** review the *case*: the reservation details, evidence, check-in record and chat.
7. **Homa founders** decide a *partial refund*: Kemas receives RM100.00, with a short reason.
8. **Homa** resolves the reservation as **Completed** and settles the *payment*: Kemas receives RM100.00,
   the service fee follows, and Sarah is refunded the rest.
9. **Homa** notifies **Sarah** and **Nur Aina** of the outcome.

**Variants**
- 7a. **Homa founders** decide a *release* (Kemas receives the provider price) or a *full refund*.
- 1a. A *report* about the finished work instead of during the job.
- 1b. A missed completion deadline (story 17) or a no-show challenge (story 16) opens the case instead.
  A challenged no-show is reviewed on the challenge alone, takes no reports, and ends **Cancelled**; the
  provider can receive up to the no-show fee.
- 1c. A report outside the dispute window, or a second report by the same party: **Homa** refuses.

**Rules:** each party files one report per reservation, from the booked start to the end of the
dispute window. A partial refund gives the provider any amount from RM0 up to what was at stake.

**In the code:** `reservation` (`Reservation.fileReport`, `resolve`), `resolution` (`Case.open`,
`addEvidence`, `decide`), `payment` (`Payment.settle`). Events: `ReportFiledIE`, `CaseDecidedIE`,
`SettlementDecidedIE`.

### 19. The customer rates the provider and the team

**Goal:** the next customer knows how Kemas and its team work.

1. **Homa** asks **Sarah** to rate the *reservation* once it is **Completed**.
2. **Sarah** gives Kemas *overall stars* (5), optional *category stars* (quality of work, punctuality,
   communication, care for the home) and optional *written feedback*.
3. **Sarah** rates *Aina* (overall stars, and optional helper categories: friendliness, hard work, care
   with belongings, cooperation, thoroughness), and skips *Ben*.
4. **Homa** adds the *rating* to Kemas's and Aina's *rating summaries*.
5. Once Kemas has 3 ratings, **Homa** shows its *average stars*; each category's average shows once it
   has 3 ratings of its own.

**Variants**
- 2a. **Sarah** rates after the 14-day **rating window**: **Homa** refuses.
- 2b. The reservation was cancelled or a no-show: it cannot be rated.
- 3a. A named *supervisor* is rated with the helper categories.
- 6. **Homa founders** hide a *rating* that breaks the rules (abuse, personal details, paying outside
  Homa), with a reason. It no longer counts.

**Rules:** a rating is an opinion and never changes a payout, a fine or a decision; there is no written
feedback about an individual worker; Homa never edits a rating.

**In the code:** `feedback` (`Rating.submit`, `hide`; `RatingSummary.record`, `withdraw`,
`isOverallShown`, `isCategoryShown`). Events: `RatingSubmittedIE`, `RatingHiddenIE`.

### 20. Re-verification every three months

**Goal:** everyone who enters a home stays verified.

1. Aina was added on 1 January, so her **due date** is 1 April.
2. On 18 March, the **re-verification reminder** (14 days before), **Homa** opens Aina's *re-check* and
   reminds **Nur Aina**.
3. **Aina**, with Nur Aina, re-verifies on the spot on Nur Aina's phone.
4. The **eKYC provider** reports a *verified result* on 20 March.
5. **Homa** closes the *re-check*, and Aina's next due date is 20 June.

**Variants**
- 4a. The result is *rejected*, or the session times out: nothing changes yet, the re-check stays open,
  and Aina can retry until the due date.
- 5a. Aina isn't re-verified by 1 April: **Homa** lapses her. She leaves the team, can't be named for new
  jobs, and Nur Aina must name someone else for her upcoming jobs. When she passes, she rejoins.
- 2a. After a safety report, **Homa founders** ask for an *early re-check* with a reason: the due date
  moves to 14 days from then, if that is sooner.
- 1a. **Kemas** itself re-verifies the same way: eKYC for an independent provider, eKYB for a business.
  A provider who misses the due date **lapses**: **Homa** pauses the *listing*, which takes no new
  requests and leaves search, while accepted reservations go ahead. When Kemas passes, **Homa** resumes
  the listing.
- 1b. Kemas can be requested, and can accept, only for jobs that end by its due date.

**Rules:** due dates are counted in calendar months on Malaysian dates; document expiry does not change
them. A repeated pass with no re-check open changes nothing.

**In the code:** `account` (`TeamMember.openRecheck`, `requestEarlyRecheck`, `recordEkycResult`,
`lapse`; the same on `ProviderAccount`, with `recordEkybResult` for a business), `offering`
(`ProviderListing.pause`, `resume`). Events: `RecheckOpenedIE`, `RecheckFailedIE`,
`TeamMemberReverifiedIE`, `TeamMemberLapsedIE`, `ProviderReverifiedIE`, `ProviderVerificationLapsedIE`,
`ListingPausedIE`, `ListingResumedIE`.

### 21. Homa waives a fine

**Goal:** a genuine emergency doesn't cost the provider.

1. **Nur Aina** asks Homa to waive the RM36.00 *late cancellation fine*: her helper was in an accident.
2. **Homa founders** review the request.
3. **Homa founders** waive what is left of the *fine*, with a reason.
4. **Homa** stops deducting the *fine* from Kemas's payouts.

**Variants**
- 3a. Part of the fine was already deducted: the waiver covers what is left.
- 3b. The fine was already fully deducted, or already waived: there is nothing to waive.

**In the code:** `payment` (`Fine.waive`, `outstanding`). Event: `FineWaivedIE`.

## Story map: where each story lives in the code

| # | Story | Contexts | Main events |
|---|---|---|---|
| 1 | Lists and values | catalogue, policy | `JobAddedIE`, `HomaPolicyChangedIE` |
| 2 | Customer registers | account | `CustomerRegisteredIE` |
| 3 | Independent provider onboards | account | `ProviderRegisteredIE`, `ProviderVerifiedIE` |
| 4 | Business provider onboards | account | `ProviderRegisteredIE`, `ProviderVerifiedIE` |
| 5 | Adding a team member | account | `MemberVerificationStartedIE`, `TeamMemberAddedIE` |
| 6 | Listing and go-live | offering | `ListingWentLiveIE`, `ListingChangedIE` |
| 7 | Availability | availability | (listens to `ReservationAcceptedIE`) |
| 8 | Search | discovery, availability | (listens to listing events) |
| 9 | Enquiry and chat | reservation, conversation | `EnquiryCreatedIE`, `EnquiryRequestedIE` |
| 10 | Request | reservation, payment | `ReservationRequestedIE` |
| 11 | Accept, decline, expire | reservation, payment, availability | `ReservationAcceptedIE`, `PaymentCapturedIE` |
| 12 | Customer cancels | reservation, payment | `ReservationCancelledIE`, `SettlementDecidedIE` |
| 13 | Provider cancels | reservation, payment | `FineChargedIE` |
| 14 | Naming the team | reservation | `TeamNamedIE` |
| 15 | Check-in and face checks | reservation | `CheckInCompletedIE`, `FaceCheckRecordedIE` |
| 16 | Customer no-show | reservation, payment, resolution | `NoShowChallengedIE` |
| 17 | Completion and payout | reservation, payment | `ReservationCompletedIE`, `PayoutCreatedIE` |
| 18 | Report and decision | reservation, resolution, payment | `ReportFiledIE`, `CaseDecidedIE` |
| 19 | Ratings | feedback | `RatingSubmittedIE`, `RatingHiddenIE` |
| 20 | Re-verification | account, offering | `RecheckOpenedIE`, `ListingPausedIE` |
| 21 | Fine waiver | payment | `FineWaivedIE` |

## Not told here

- **Usecases and infrastructure:** how the app calls the domain, the scheduled jobs, and the adapters
  to Kenal, Stripe, the SMS provider, the calendar services and the geoservice. See
  `docs/homa-usecase-plan.md`.
- **Data requests:** customers, providers and team members asking to see, correct, copy or delete their
  data, which the founders handle and record in the admin console.
- **Admin tracking:** the overview, reservation history, payments, providers, customers, fines and
  ratings views.
- **Things outside the MVP:** two-way ratings, consequences of violations and suspension, Homa credit,
  rescheduling, accounts for team members, showing nationality, checking work permits, re-verifying on
  document expiry, and skilled trade jobs.
