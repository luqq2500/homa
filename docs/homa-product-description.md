# Homa

> **Everyone deserves a well-cared home.**

## Contents

**Part 1: Product brief**
- What Homa Is
- The 7-Star Experience
- Product Principles
- Goals and Measures

**Part 2: Product rules**
- Where Homa Operates
- How Homa Works
- Pricing
- Reservation Lifecycle
- Policies
- Homa-Set Values
- Key Experiences
- Provider App
- Homa Admin
- Not in the MVP
- External Services Homa Will Use and Not Develop

# Part 1: Product brief

## What Homa Is

Homa is a platform that helps homeowners find, reserve and get the best home service providers. It gives homeowners back **time they wish they had**: a few hours of a trusted helper to care for the home.

Typical jobs are kitchen care, toilet care, living room tidying, laundry folding and ironing, wardrobe organising, and picking things up.

### Who it's for

Homeowners who don't have the time to finish their home chores, and homecare businesses that want to reach them.

### Where Homa puts its effort

Homa builds what brings value to homeowners and homecare providers: finding the best provider, booking with a clear price, a safe job day, fair payments and disputes, and honest ratings. Signing in with Google or Apple is done by Google and Apple, one time password by OTP provider, identity verification by a KYC provider, and payments by a payment provider. Homa uses their results and does not build those checks itself, and keeps no passwords.

### Safety principle

**Only verified people enter a customer's home, and the customer knows who they are before they arrive.** Every provider is a registered business, and every business and team member is verified by Homa's KYC provider to join (a business through KYB, which includes its owner's or director's KYC), is re-verified regularly, is shown to the customer before the job, and has their face checked at the door. Homa verifies who people are through its KYC provider; it does not inspect how they work, which is judged through ratings and disputes.

## The 7-Star Experience

The core moment is **"I booked help, and it turned up."** Homa aims for 7 stars: magical, but buildable now.

| Stars | What it looks like |
|---|---|
| 1 | Nobody shows up, and you've paid. |
| 3 | Strangers turn up late, and the price grows on the day. |
| 5 | A verified team you've seen arrives on time and does what you booked. |
| **7 (Homa's target)** | **You know you're booked at once, or by a set time. The team gets in without a phone call, you can leave once they've checked in, and booking the same provider again takes one tap.** |
| 9 | The same trusted team comes every week and knows your home. |
| 11 | Your home is simply cared for, without you thinking about it. |

What makes it 7 stars in this spec:

- **Certainty:** **Instant book**, or an answer from the provider by the **response deadline**.
- **Getting in:** the address is pinned on a map, and **access notes** travel with every booking.
- **Freedom:** be home for check-in, then leave. The dispute window gives time to check the work later.
- **Trust:** a verified named team, a face check at the door, an **Insured** badge where declared, safety reports first in Homa's queue, and a decision date for every case.
- **Coming back:** **Book again** copies a past booking in one tap. Recurring bookings and the same team every week (9 stars) come after launch.

## Product Principles

Each principle names the trade-off Homa accepts for it.

1. **Only verified people work, even if a booking is lost.** Some jobs that might have gone fine are cancelled.
2. **The system adjusts and tells people; it never asks Homa or the customer to sort it out.** There are more automatic rules.
3. **Nobody waits on something that can't happen.** Requests end sooner, sometimes without a person deciding.
4. **Honest records over flattering ones.** Providers' stats sometimes look worse.
5. **One clear rule for each situation.** Edge cases get a firm answer instead of a choice.

## Goals and Measures

The admins review these monthly in the admin console. First targets are set from the first 8 weeks of data, because Homa has none yet.

| Goal | Measure |
|---|---|
| Customers find help | Share of searches with at least one exact match, by city, job and time |
| Newcomers book | Share of first-time searchers who make a first reservation within 7 days |
| Customers know at once | Share of reservations confirmed by Instant book; for requests, the acceptance rate and the median time to an answer |
| Jobs go as booked | Share of reservations completed with no report; provider no-show and late cancellation rates |
| Customers come back | Share of customers who book again within 60 days of their first job |
| Trust holds | Disputes and safety reports per 100 jobs; share of cases decided within 72 hours |
| Providers earn | Active live providers (a completed job in the last 30 days) and their median monthly payout |

**Assumptions to test.** No customer or provider research backs this spec yet, so each of these is an assumption until interviews and launch data confirm it:

- Busy homeowners would rather buy a few hours of a team's time than pay per task.
- Verification and the face check are enough for customers to let a team in and then leave.
- Providers accept Homa's fixed policies and fines in return for the customers Homa brings.
- Enough providers choose Instant book to make knowing at once the normal experience.
- Three neighbouring cities have enough demand and supply to start.
- Providers accept a 15% commission from their payouts in return for the customers and protection Homa brings, rather than finding customers directly.

# Part 2: Product rules

## Where Homa Operates

Homa launches in three neighbouring cities: **Salak Tinggi**, **Cyberjaya** and **Putrajaya**. All prices are in Malaysian ringgit (RM).

Each provider chooses which cities they cover. Search only shows providers who cover the city of the customer's address, or the city a guest picks.

The customer app and the provider app are in Bahasa Melayu and English, and each person chooses their language.

## How Homa Works

Homa sells **a provider's time**. The customer chooses what they want done and for how long, and the price is clear upfront.

1. **Search:** pick the jobs you need, a date and time, a duration (1 to 6 hours) and the number of helpers (at least 1). You don't need an account to search: a guest picks a city or drops a pin. Homa asks you to sign up when you tap **Reserve** or **Chat**.
2. **Browse:** see providers who match exactly, plus close matches so you can explore alternatives. A close match differs from the search in one way only: either it offers some but not all of the chosen jobs, or it is free for the same duration at a start within 2 hours before or after the chosen time, never both. A close match is still in the customer's city and fits the helpers and duration, and a start it suggests is always one the customer can book: on a 15-minute step, at least the minimum notice from now and within the booking horizon.
3. **Choose:** open a provider page to see their jobs, tools, price, availability, cancellation policy and team. You can tap **Chat** to ask the provider questions first; the chat keeps your chosen details and you can reserve from it.
4. **Review:** check the details on the review page and edit them if needed. The same page is used whether you reserve directly or from a chat.
5. **Reserve:** tap **Reserve**. With a provider who uses **Instant book**, the reservation is confirmed as soon as your payment goes through. With a provider who uses **Request and accept**, it is a reservation request: the provider accepts or declines it by the **response deadline**, and you can edit it until then. You pay by card or online banking (FPX). A card is authorised and charged only when the reservation is accepted; FPX is paid upfront. If the provider declines or doesn't respond, a card authorisation is released and an upfront payment is refunded in full.
6. **Job day:** you can see who is coming. At the door, every person who came verifies their face, the team does the job, and the provider marks it completed with photo or video evidence.
7. **Payout:** after the dispute window ends without a report, Homa releases the payment to the provider.

### A provider's offering

What a provider offers customers is their **offering**: the jobs they offer and the tools they bring, the cities they cover, the range of helpers per booking and when a supervisor is sent, the hourly rate per helper and how the travel fee works, the range of hours, the cancellation policy, the acceptance policy, and any liability insurance. It is shown under the provider's profile name and picture. Each provider has one offering. It is **live** when customers can find and reserve it, **paused** while it takes no new requests, and **offline** until the provider goes live again. Homa calls it the provider's **offering** everywhere, in the apps, the admin console and the code, never a listing.

### Time

start times are in 15-minute steps. A start time must be at least the **minimum notice** from now and no later than the **booking horizon**. Duration is in whole hours, from 1 to 6 hours, within the provider's own minimum and maximum. A request must fit both Homa's duration range and the provider's own hours. Bookable times come from each provider's own calendar.

### Jobs and tools

Homa sets the **job list** and its groups, such as kitchen or living room, and the **tool list**, the same way it sets the policies and the policy options. Each provider chooses which jobs they offer and which tools they bring from these lists. Because every provider uses the same lists, search can match jobs exactly, and the provider page can show which tools from the list a provider does not bring. Homa can rename a job or tool, and move a job to another group; providers keep it, and only what is shown changes. Homa can retire a job or tool so providers can no longer pick it. A retired job or tool comes off every provider's offering, and the provider is told; a provider left with no job is taken offline until they offer another one and go live again. Replacing a job is retiring it and adding a new one, which providers pick themselves. Requests and reservations already made keep the job. A featured search searches only for jobs providers can offer; a retired job comes off the featured searches, and one left with no job is no longer shown. A retired job or tool is never deleted, and its code is never used for anything else.

### Teams, helpers and supervisors

A provider's **team** is everyone verified to work for them. Each team member has one role:

- **Helper:** does the booked jobs. The customer books and pays for helpers.
- **Supervisor:** the person in charge on site. A supervisor is not counted or charged as a helper.

Teams have no size limit. Each provider sets the minimum (at least 1) and maximum number of helpers per booking; Homa sets no upper limit. The maximum cannot be more than the number of helpers on the team, so the provider can always name everyone who is coming. The customer picks any number in that range.

If the team's helpers drop below the maximum, the maximum shrinks to match, and the minimum with it if needed. A growing team does not widen the range on its own; the provider sets it again. A live provider with no helper left stops being live, and must have a helper, set their helpers and go live again. The same applies to a live provider with a supervisor rule and no supervisor left: they stop being live until they add a supervisor or clear the rule, and go live again. Upcoming reservations that need a supervisor still need one named before the booked start.

**When a supervisor comes:** a provider with supervisors can set when one is sent: from a **minimum number of helpers**, from a **minimum number of hours**, or both. A booking that reaches either one includes at least one supervisor, at no extra cost to the customer. The provider page and the review page say when a supervisor is included. A provider who sets neither never has to send one, and can still choose to.

## Pricing

Each provider sets:
- the minimum and maximum number of helpers per booking (no Homa upper limit)
- when a supervisor is sent, if they have supervisors
- a flat hourly rate per helper
- minimum and maximum hours, within Homa's range of 1 to 6 hours
- how the travel fee works: either a **separate flat travel fee**, or **already included in the price**
- the cities they cover
- how reservations are accepted: **Request and accept** (the default) or **Instant book** (see Acceptance Policy)

A provider offers at least one job and covers at least one city. Bringing no tools is allowed. A separate travel fee is more than RM0; a provider who charges nothing for travel includes it in the price.

The **hours fee** is the hourly rate per helper × the number of helpers × the hours. Supervisors are not charged; their cost is part of the provider's hourly rate. The **provider price** is the hours fee plus the travel fee when it is charged separately.

Homa earns a **commission** of 15% of what the provider earns, taken from the provider's payout. It is not a separate fee: the **total** the customer pays is the provider price, and the customer never sees a Homa fee, whether on the provider page, the review page, at payment, in their payments and refunds, or on a receipt. The provider sees it: next to their hourly rate, the provider app shows what they receive after Homa's commission, and each payout statement shows it. The provider's **payout** is what they earn from a reservation, less Homa's commission and any fine deducted.

There are no quotes. Choosing fewer jobs within a booking does not change the price, because you are buying the provider's time. Providers bring their own tools.

Percentages are rounded half up to the nearest sen. When a payment is split, the customer is refunded what they paid less what the provider earns, and what the provider earns is split between Homa's commission and the provider's payout, with the payout worked out last, so the parts always add up to exactly what the customer paid.

## Reservation Lifecycle

| Status | Meaning |
|---|---|
| **Requested** | The customer has requested; once the card is authorised or the upfront payment is paid, it waits for the provider to respond, or is accepted at once with Instant book |
| **Reserved** | The provider accepted the request |
| **Declined** | The provider declined, or accepted another booking that takes the same time |
| **Expired** | The provider did not respond in time |
| **Cancelled** | Cancelled by the customer or the provider, ended as a no-show, or a request whose payment failed |
| **Disputed** | A report was filed, a customer challenged a no-show, the provider missed the completion deadline, or the provider's business was seriously flagged, and it is waiting for Homa's review |
| **Completed** | The final status of a job: the dispute window ended with no report, or Homa resolved the review |

A reservation stays **Reserved** through the job and the dispute window. If a report is filed, or the provider misses the completion deadline, it becomes **Disputed**. **Completed** is the last status, reached when the dispute window ends with no report or when Homa resolves a review. A reservation disputed because the provider's business was seriously flagged ends **Cancelled** with a full refund, unless the KYC provider clears the flag before the booked start and Homa restores it to **Reserved**, so it goes ahead as booked. A challenged customer no-show is the other exception: after Homa's review it ends **Cancelled**, whether Homa upholds the no-show, gives the provider part of the no-show fee, or refunds the customer in full. The provider marking the job completed with evidence is an action and does not change the status.

Payment has its own states, separate from the reservation status: **authorised** (card only: checked and nothing charged yet), **authorisation released** (card only: the request was declined, expired or cancelled before acceptance, so the authorisation was let go and nothing was charged), **held** (paid and held by Homa), **released** and **refunded**.

## Policies

Homa owns the policies that protect both customers and providers. Providers do not set them; the only thing a provider chooses is their cancellation policy, from options Homa offers. The values these policies use are listed under **Homa-Set Values**.

### Payment Protection

- How the customer pays depends on the method:
  - **Card:** authorised when submitting the request and charged only when the reservation is accepted, by the provider or at once with Instant book. A declined, expired or cancelled request releases the authorisation, so nothing is refunded because nothing was charged.
  - **Online banking (FPX):** paid in full when submitting the request, because FPX cannot be authorised first.
  - Card and FPX are the only payment methods, and each reservation is paid with one of them.
  - **Paid before the provider answers:** the provider sees a request, and can accept it, only once the card is authorised or the FPX payment is paid.
  - **A payment that fails:** if the card is declined, or the FPX payment fails or is abandoned, the request ends straight away and nothing is charged. The customer is told and can request again.
- Homa holds every charged payment until it is released or refunded.
- The payment is released to the provider when the dispute window ends without a report. The window runs until after the provider submits job completion, so the customer always has time to review the finished work.
- A report stops the release until Homa's review is done.
- A customer cancellation fee is released to the provider straight away, including the customer no-show fee a customer pays for cancelling after check-in.
- A customer no-show fee is released when the **no-show report window** ends without the customer reporting it, and the customer's refund of the rest is paid at the same time. If the customer reports it, Homa investigates and decides whether to release the fee or refund the customer, and both parts are paid after the decision.
- Released payments are paid out to the provider per reservation.
- **Refunds** always go back to the original payment method. They take as long as the bank needs; for FPX this can be a week or more, so the app shows the customer the refund status and the expected timing.
- Homa's commission follows what the provider finally earns from the reservation: 15% of the provider price for a completed job, of a cancellation fee, or of a customer no-show fee, and nothing when the customer is refunded in full. It comes off the provider's payout; the customer's refund never pays it.
- **Chargebacks:** a customer who disputes a card payment with their bank after the job opens a case for Homa. Homa answers the bank with the reservation's evidence. If Homa decides the provider is responsible, the amount is recovered from the provider's next payouts the same way as a fine (see Provider Cancellation Fines); otherwise the provider keeps their payout and Homa contests the chargeback.
- **Processing fees:** Homa pays the payment provider's fees out of its commission, including on refunds. Customers and providers never pay them.
- **Receipts:** the customer gets a receipt for each payment and refund, and the provider a statement for each payout, showing what they earned, Homa's commission, any fine deducted and what they receive. The customer's receipt shows the total paid, with no Homa fee. **Before launch, an accountant confirms the service tax and e-invoicing rules for Homa's commission.**
- All payments for a reservation must stay on Homa.

### Request Policy

- The customer can edit a request (jobs, date and time, duration or helpers) until the provider accepts it. The address can't be changed on a request: to use another address, the customer cancels the request for free and requests again. For a card, a new authorisation for the new total replaces the old one, which is released. For FPX, the customer pays any increase before the edit is sent, and any decrease is refunded. After an edit that raises the total, the provider can accept only once the new total is authorised or paid; if that payment fails, the request ends.
- A request expires if the provider does not respond within the **request expiry time**, and never later than the **response deadline** before the booked start, so the customer always knows in time. The provider can accept or decline up to that moment; at the expiry time the request has expired.
- Editing a request does not restart its expiry. It still expires the request expiry time after it was first submitted, or at the response deadline if the edit moves the start earlier than that.
- A declined or expired request costs the customer nothing: the card authorisation is released, or an upfront payment is refunded in full.
- Requests don't hold the provider's time; only an accepted reservation does. When the provider accepts a request, any other open request of theirs that can no longer be accepted, because its time now clashes with the accepted booking or the buffer between jobs around it, is declined at once. The customer is told, and the request costs them nothing. These automatic declines don't count against the provider's response or acceptance rates.
- The customer can cancel a request for free at any time before the provider responds.
- If the provider cancels a reserved booking, the customer is refunded in full and the provider may pay a fine (see Provider Cancellation Fines).

### Acceptance Policy (chosen by the provider)

- Each provider chooses how reservations are accepted:
  - **Request and accept** (the default): the provider accepts or declines each request by the response deadline.
  - **Instant book:** a request is accepted automatically the moment its payment is secured. The card is authorised and charged at once, or the FPX payment is paid.
- Instant book never accepts a time the provider isn't free: Homa checks the provider's availability when the request is made and again when it is accepted. If the time is no longer free, the request ends and costs the customer nothing.
- The provider can't decline an instant-book request. Once accepted, it is a normal reservation: the same team, check-in, cancellation and fine rules apply, and a provider who can't make it cancels and pays any fine.
- The provider page and search results show which providers use Instant book.
- A change of acceptance policy applies to new requests only.

### Check-in Policy

- **Who is coming:** before the booked start, the provider names the helpers for the reservation, matching the number of helpers booked, and at least one **supervisor** when the booking reaches the provider's supervisor threshold. The customer does not pick them but sees each person's name, role, photo and average stars. Homa reminds the provider at the **team reminder time**, and check-in is enabled only once the team is named. The provider can change the team until the booked start, and the customer is notified of any change. Only people on the team can work: a named person who lapses or is removed comes off the job's team at once (see Verification Policy).
- **Being home:** the customer, or someone they choose, is home to let the team in at check-in. After check-in they are free to leave; the provider marks completion with evidence, and the customer has the dispute window to check the finished work.
- **Access notes:** each new request copies the access notes from the customer's account, such as the unit, guardhouse registration and parking. The customer can change a reservation's notes until the job is completed, and the provider sees them from acceptance.
- The provider is responsible for check-in and job completion. Someone on site must have the provider app to check in. How a provider runs their team is their own operation; Homa holds the provider accountable either way.
- Check-in is **location-based**. Homa enables the check-in action only when the device is within the **check-in radius** of the customer's location, and the provider then marks check-in.
- Homa uses the device's location only at the moment of check-in, not continuously.
- **Face check:** after the location check, every named person takes a live selfie on the provider app, and Homa's KYC provider matches it against their verification. Check-in counts once the first named person passes. Each other person must pass before working; a person who fails, after the attempts the KYC provider allows, cannot work on that job, and the customer is told. The customer sees each person's result and time. If the face check service is unavailable, check-in goes ahead on location only, marked "face check unavailable" for the customer, and the provider is not fined. Homa keeps only the result and time, never the selfie.
- The customer does not need to confirm check-in. The customer sees the check-in time, and can report a problem (for example a missing helper) through the Dispute Policy.
- The provider can check in from the **earliest check-in** time and must check in by the booked start plus the **check-in period**, including its last minute: for a 10:00 start, from 09:45 up to and including 10:15. Homa records the check-in time, and the customer can see it.
- No check-in by then is a provider no-show. The reservation is cancelled automatically, the customer is refunded in full, and the provider pays a fine (see Provider Cancellation Fines).
- During the check-in period, the customer cannot cancel until the provider checks in or the period ends. This way a customer is never charged for a provider who did not arrive. If the period ends with no check-in, the customer does not need to cancel: the reservation ends as a provider no-show, with a full refund.
- If the provider has checked in and the customer is not there after the **customer no-show wait**, the provider can report a customer no-show, up to and including the booked end. The wait runs from the booked start, or from the check-in if the provider checks in after the start. The provider receives the **customer no-show fee**: the travel fee plus 30% of the hours fee. The customer is refunded the rest. The customer is notified and can report a wrongful no-show within the **no-show report window**, which closes at the moment the fee is released; Homa then investigates and decides. Homa can uphold the no-show (the fee stands), refund the customer in full, or give the provider part of the no-show fee. This is a Homa policy that applies to every provider; providers do not choose it.
- A customer who cancels after the provider has checked in pays the customer no-show fee, even if the provider checked in early and the booked start has not come yet. The fee is released to the provider straight away and can't be challenged, because the customer chose to cancel. If something is wrong with the job, the customer files a report instead of cancelling (see Dispute Policy).
- The job time stays as booked. A provider who arrives within the check-in period but late is tolerated unless the customer reports it.

### Completion Policy

- The provider marks the job completed, and photo or video evidence is required. There is no automatic completion.
- Once completion is submitted, neither the customer nor the provider can cancel, and the provider can no longer report a customer no-show. Any problem goes through a report.
- The provider can still submit completion while the reservation is **Disputed** over the job, after a report or a missed completion deadline. The evidence goes to Homa's review and the status stays **Disputed**. The dispute window then ends the **dispute window extension** after the submission or the booked end, whichever is later, so the other party can still file their report until then.
- If the provider has not submitted completion by the booked end plus the **completion reminder time**, Homa reminds them to submit it.
- If the provider has still not submitted completion by the **completion deadline**, the reservation moves to **Disputed** and Homa reviews it. Homa decides on a release, a partial refund or a full refund.
- Evidence is used only for disputes and kept for the **evidence retention period**.

### Dispute Policy

- There is one dispute window, from the booked start until the **dispute window extension** after the later of the booked end and the moment the provider submits job completion. The customer is notified when the provider marks the job completed.
- Homa acts only on reports. The customer and the provider are each expected to file a report if something goes wrong. If no report is filed before the dispute window ends, Homa is not responsible for the issue, and the payment is released as normal.
- The customer and the provider can each file one report per reservation, with a reason, and evidence where available. Reports open at the booked start and close at the moment the dispute window ends.
- While a reservation is Disputed after a report or a missed completion deadline, the other party can still file their one report. A challenged customer no-show is reviewed on the challenge alone, and takes no reports.
- A report is one of three types: a problem during the job (for example a missing helper or missing tools), a problem with the finished work, or **safety, damage or loss** (for example harassment, an injury, something broken or something missing from the home).
- A safety, damage or loss report goes to the top of Homa's queue. Homa still doesn't compensate damage (see Damage Policy): the report records what happened, lets Homa help, for example with a police report, and can lead Homa to ask the KYC provider for an early re-check. The money outcome is decided the same way as any report.
- Homa aims to decide every case within **3 days (72 hours)** of it opening; every day counts, weekends and public holidays included. The customer and the provider see the exact date and time it is due.
- A report moves the reservation to **Disputed**. Homa reviews it and decides on a release, a partial refund or a full refund. A partial refund gives the provider any amount from RM0 up to what was at stake: the provider price for a job, or the customer no-show fee for a challenged no-show. Homa's commission is taken from what the provider receives.

### Conduct Policy

- A violation is a report that Homa's review upholds.
- Consequences of violations are not part of the MVP. In the MVP, Homa only decides the outcome of each report (a release, a partial refund or a full refund).

### Rating Policy

- **Who rates:** only the customer, only for a **Completed** reservation, and only once per reservation. Cancelled reservations and no-shows cannot be rated; those are handled by reports and the cancellation count. A reservation Homa decided with a full refund is still Completed and can be rated, because the customer's experience counts; it doesn't count as a job done.
- **When:** within the **rating window** after the reservation becomes Completed, including its last minute. A disputed reservation can be rated only after Homa's decision, because it becomes Completed only then.
- **Rating the provider:** overall stars from 1 to 5 (required), stars for any number of **provider categories** (optional), and written feedback (optional, up to 1,000 characters).
- **Rating helpers and supervisors:** rating them is optional. For each person the customer rates, overall stars from 1 to 5 are required, and stars for each **helper category** are optional. There is no written feedback for helpers or supervisors, so no one writes comments about an individual worker. Supervisors are rated with the helper categories.
- **Showing ratings:** a provider's or helper's overall average is shown only once they have at least the **minimum ratings to show**. Until then, the count is shown with "New". Each category's average is shown once that category has its own minimum ratings to show, because customers may skip categories.
- **Ratings and reports are separate.** A rating is an opinion and never changes a payout, a fine or a decision. A problem that needs Homa's decision is a report.
- **Hiding:** Homa can hide a rating that breaks the rules (abuse, personal details, or talk of paying outside Homa), with a recorded reason. A hidden rating no longer counts. Homa never edits a rating.

#### Rating categories

Every category is rated from 1 to 5 stars, and every category is optional. A customer can rate any number of them, or none: only the overall stars are required, for the provider and for each helper they rate.

| For | Category | What it rates |
|---|---|---|
| Provider | Quality of work | How well the booked jobs were done |
| Provider | Punctuality | Whether the team arrived on time |
| Provider | Communication | How clear and timely the replies in chat were |
| Provider | Care for the home | How carefully the home and belongings were treated |
| Helper | Friendliness | How pleasant and polite the helper was |
| Helper | Hard work | How steadily the helper worked through the booked time |
| Helper | Care with belongings | How carefully the helper handled the home's things |
| Helper | Cooperation | How well the helper followed requests within the booked jobs |
| Helper | Thoroughness | How much attention to detail the helper paid |

### Verification Policy

**Business providers only.** Every provider on Homa is a business registered with SSM. Homa has no independent providers.

**A KYC provider verifies everyone.** Homa does not build identity checks. A KYC provider runs every check on its own pages and by its own rules: the business's KYB, with its owner's or director's KYC in the same flow; each team member's KYC; which identity documents are accepted; face matching; re-verification schedules and reminders; early re-checks; and session timeouts. Homa records each result and relies on it, and does no check of its own. Homa's requirements for the KYC provider are listed under **External Services**.

**What Homa uses from each result:** whether the business or person is verified, the date they are verified until, their registered name, and for a business its SSM number and registered business name.

**Registered name.** Each person's name is the name on their identity document, from the KYC result. The provider cannot type or change it. Customers see the name as registered.

**Adding a team member.** The provider starts a short form, the person verifies on the spot, and the provider confirms; nothing is on the team until all three are done. A business can add team members only once its KYB has passed and while it hasn't lapsed, so nobody's identity is checked for a business that may never be verified.
1. **Start:** the person must be with the provider. The provider chooses the role (helper or supervisor) and takes the person's **profile photo with the phone's camera**; a photo from the gallery isn't accepted. This is the photo customers see.
2. **Verify on the spot:** on the provider's phone, the person completes the KYC provider's check, scanning their own identity document and taking their own live selfie; team members have no accounts and need no phone of their own. The KYC provider also checks that the profile photo is the same person as the verified selfie. The check fails if the photo doesn't match, or if the person is **under 18** (from the date of birth on their document).
3. **Confirm:** the provider app shows the verified person's registered name and the country of their document. The provider ticks that **this named person** is lawfully employed by or engaged with them and allowed to work in Malaysia; for a document from another country, that they hold a valid work pass that allows them to work for this provider. The person joins the team only once it is ticked.

Until the person joins, the provider app shows them as **waiting for verification**, or **waiting for your confirmation** once verified: they are not on the team, not shown to customers and cannot be named for jobs. Homa sets no time limit; the KYC provider ends a session that is not finished. If the check is rejected, the photo doesn't match, the person is under 18, or the session times out, nothing is added, the provider is told which, and can start again.

**Changing a role.** The provider can change a team member between helper and supervisor at any time, without verifying them again; their identity hasn't changed. The usual rules still apply: helpers count towards the helper range, and the supervisor rule needs a supervisor on the team. Moving a helper to supervisor lowers the helpers on the team, so the maximum helpers per booking shrinks to match. A job already named keeps the person in the role they were named in, until the provider names that job's team again; the new role applies to jobs named from then on. A business starts with no team members, so an owner or director who works on site is added the same way.

**Lawful employment.** Homa verifies who people are, not whether they may work: checking work permits is outside Homa's scope, so team members verify only their identity with the KYC provider. Each time the provider adds a team member, they confirm, after the person is verified and named, that this person is lawfully employed by or engaged with them and allowed to work in Malaysia (for a document from another country, that they hold a valid work pass to work for this provider), and that they are responsible for it. Each confirmation is recorded with the person, its time and the version of the terms. Team members can be Malaysian or foreign, and are 18 or older. **Before launch, a lawyer confirms the minimum age.** Homa removes a team member if it learns they are not allowed to work, and records what happened. **Before foreign team members are enabled, a lawyer confirms the terms of this confirmation.**

- Customers and businesses register with their phone number, verified by an SMS code, whether they sign up with Google, Apple or the phone number itself (see **Signing up and signing in**); customers also give their name. The phone number is a Malaysian mobile number. The SMS code is 6 digits and valid for 5 minutes; after 5 wrong codes the customer needs a new code.
- A business whose KYB is rejected is told to redo it, and can be verified on a later attempt. The provider can remove a team member at any time. A removed person is no longer on the team, can no longer be named for jobs, and comes off every upcoming job they were named on.
- **Re-verification:** the KYC provider re-verifies every business **every year**, and every team member **every year and when their identity document expires**, and reminds the provider before each is due. Re-verification is done on the spot on the provider's phone, the same way as adding a member, with the person present. A pass moves the verified-until date out; a failed attempt changes nothing.
- **Missing a re-verification:** the KYC provider reports it, and the business or person **lapses**. A lapsed team member leaves the team until they pass: they can't be named for new jobs, and they come off every upcoming job they were named on. A lapsed business has its offering **paused**: it takes no new requests until it passes. Reservations already accepted still go ahead, unless the business was seriously flagged.
- **Ongoing monitoring:** between re-verifications, the KYC provider monitors every business and team member, and tells Homa:
  - **Re-verification needed by a date:** for example after a change of director, owner, name or address in the business's registration, or before a team member's document expires. The business or person keeps working, and their verified-until date moves to that date if it is sooner. If they don't pass in time, they lapse.
  - **A serious flag:** for example the business is struck off, wound up or dissolved, or it or a team member matches a sanctions or watchlist check. The KYC provider reports only flags it has confirmed. The business or person lapses at once.
- **A lapsed or removed person named on a job:** they come off that job's team at once, and the customer is told.
  - Before the booked start, the team counts as not named until the provider names someone in their place: Homa reminds the provider again, and check-in stays locked. If nobody is named by the booked start, the provider can't check in, and it ends as a provider no-show. Homa can waive the fine when a serious flag came too close to the start to replace the person.
  - During the job, the person stops working, the same as after a failed face check. The customer can report a missing helper.
- **A seriously flagged business's bookings and money:**
  - Every accepted reservation whose completion hasn't been submitted moves to **Disputed** for the flag at once. That includes a job under way, whose team stops, and a reservation already disputed over a report, whose report and evidence are kept with it. The customer is told the job is on hold while Homa checks, and the team can't check in. A reservation whose completion was submitted carries on as normal, and its payout is held (below).
  - Homa's admins decide each one: they **cancel it with a full refund**, with no fine and no cancellation fee, which is the normal outcome; or, if the KYC provider has cleared the flag and the booked start hasn't come, they **restore it**, and it goes ahead as booked.
  - If nobody has decided by the booked start, Homa cancels it with a full refund. A customer is never left waiting at the door.
  - The business's payouts are **held**, including those for jobs completed before the flag. The admins review them and either release them or keep them back, with a reason. A payout kept back stays with Homa, recorded as owed to the business and never counted as Homa's revenue. The customer isn't refunded, because the job was done. What happens to it later is decided on legal advice, outside the MVP. **Before launch, a lawyer confirms this.**
- **Verified for the whole job:** a business can be requested, and can accept, only for a job that ends by its verified-until date. Search leaves a business out for any job that would end later. Once it passes its re-verification, the date moves out and those dates open up.
- **Early re-check:** Homa can ask the KYC provider for an early re-verification of a business or team member, for example after a safety, damage or loss report. If it isn't passed in time, the KYC provider reports the lapse.
- The **Verified ID** badge shows when each business and person was last verified.

### Privacy Policy

Homa follows Malaysia's Personal Data Protection Act 2010 (PDPA), as amended in 2024.

- **What Homa collects and why:**
  - Customers: name, phone number, address with its map pin, and access notes, and, if they sign in with Google or Apple, the account id and email address it shares, to sign in, show providers who the booking is for, match providers by city and locate check-in.
  - Businesses: the phone number and, if the owner or director signs in with Google or Apple, the account id and email address it shares, to sign in; the SSM registration number and registered business name from the KYC provider's KYB, for Homa's records; payout details, handled by Stripe.
  - Team members: the registered name, the verified-until and last verification dates, the profile photo, and face check results at check-in (result and time only, never the selfie), to show customers who is coming. Identity documents and face data are collected and held by the KYC provider, used only to verify a person against their own documents.
  - Ratings: customers' ratings of providers and helpers, shown to other customers as overall and category averages, and customers' written feedback about the provider. Team members agree to being rated as part of their consent on the KYC provider's screen.
  - Location: only the device's location at the moment of check-in, never continuously.
  - Job evidence (photos and videos) and chat, to support disputes and Homa's reviews.
  - Payment details are handled by Stripe; Homa does not store card or bank details.
  - Searches: what was searched for (jobs, date and time, duration, helpers and city) and how many providers matched, with nothing that identifies the customer, to see where more providers are needed.
- **Consent:** asked at sign-up and on the KYC provider's screens. Location permission is asked only when checking in. Each team member gives their own consent on the KYC provider's screen, including to face checks at check-in and to being rated.
- **Retention:** evidence is kept for the **evidence retention period**. Other personal data is kept while the account is active and deleted or anonymised after the account is closed, except records Homa must keep by law (such as payment records).
- **User rights:** customers, providers and helpers can see their data, correct it, get a copy, and ask to close their account and delete their data.
- **Sharing:** personal data is shared only with the external services listed below, to run Homa, and with authorities when the law requires. The KYC provider may process data outside Malaysia; a data processing agreement and the PDPA's rules on transfers cover it. Customers see only what they need about providers and helpers (names, photos, check-in time), and providers see only the customer's name, address and reservation details.
- **Data breaches:** Homa has a breach response process and notifies the Personal Data Protection Commissioner and affected users as the law requires.
- **Data protection officer:** Homa names a data protection officer before it reaches the size where the law requires one.
- The exact wording is checked by a lawyer before launch.

### Scope Policy

- The booked jobs are the scope of a reservation.
- Any additions or changes are left to the customer and provider to agree between themselves, and Homa does not review disputes about them.
- A reserved booking cannot be rescheduled. To change the date or time, the customer cancels under the provider's cancellation policy and submits a new request.

### Damage Policy

- Homa does not compensate damage.
- A provider can declare their liability insurance, with the insurer and the date it's valid until. The provider page shows an **Insured** badge until that date. Homa doesn't check the policy; the provider is responsible for the declaration, as for lawful employment.
- Damage, loss or a safety incident is reported with a safety, damage or loss report (see Dispute Policy).
- Homa assists manually when a customer or provider wants to make a police report.

### Cancellation Policy (chosen by the provider)

- Each provider chooses one of three tiers Homa offers: **Flexible**, **Standard** or **Firm**. The tiers are listed under **Homa-Set Values**.
- **Flexible** is free cancellation: the customer is refunded in full when cancelling before the booked start. Homa can change the Standard and Firm percentages, but Flexible always stays free.
- **Standard** and **Firm** have a **fee window**. If the customer cancels within the fee window before the booked start, the provider receives 10% (Standard) or 20% (Firm) of the **hours fee**. The travel fee is refunded. Cancelling before the fee window opens is a full refund.
- The customer can cancel for free at any time before the provider responds to the request.
- The tiers apply only until the booked start. During the check-in period the customer cannot cancel until the provider checks in or the period ends; after check-in, a customer cancellation costs the customer no-show fee (see Check-in Policy).
- When the travel fee is included in the price, the percentage applies to the full provider price.
- No cancellation fee is higher than the customer no-show fee, which is the ceiling. This holds whenever Homa changes a tier's percentage or the customer no-show fee.
- The chosen policy is shown on the provider page, before the customer reserves.
- Customers never see the fee only as a percentage. The provider page explains the tier in words (for example, "Standard: free until 24 hours before; after that, 10% of the hours fee"). The review page and the reservation details show the exact amount and time for that booking (for example, "Cancel after Sat 9:00 am: you pay RM36. Before that: free").

### Provider Cancellation Fines

- When a provider cancels or does not show, the customer is always refunded in full.
- A provider who cancels before the **fee window** (24 hours before the booked start) pays no fine.
- A provider who cancels within the fee window pays Homa a fine of **20% of the hours fee**. The fee window is the same one the customer's Standard and Firm tiers use, so if Homa changes it, both move together.
- A provider who does not check in by the booked start plus the check-in period, who cancels after the booked start, or who cancels after checking in, pays Homa a fine of **30% of the hours fee**. This is the highest provider fine, so not showing up is never cheaper than cancelling late, and Homa keeps it that way when changing either fine.
- The fines are the same for every provider. They do not depend on the cancellation tier the provider chose.
- When the travel fee is included in the price, the percentage applies to the full provider price.
- Homa deducts an unpaid fine from the payouts of the provider's next successful reservations until the fine is cleared. Each deduction is at most the **fine deduction cap** of that payout, after Homa's commission, so the provider always takes home part of every job. Fines come off payouts for completed jobs only, not off payouts of a cancellation fee or a customer no-show fee. When there are several fines, the oldest is deducted first. The cap is rounded in the provider's favour.
- Homa can waive a fine after review, for example for a genuine emergency or when the customer caused the problem. A waiver covers what is left of the fine, is recorded with a reason, and is given once. A fine already fully deducted has nothing left to waive. A waiver covers only the money: the late cancellation or no-show still counts in the provider's cancellation count and stays on their record.
- Homa shows each provider's cancellation count on their provider page (see Provider page).
- There is no suspension in the MVP. Homa tracks each provider's late cancellations and no-shows in the admin console only.

## Homa-Set Values

These are the values Homa sets for the policies above. They are the same for every reservation and provider. Providers only choose which cancellation tier applies to them, and their own hours and helpers within Homa's ranges.

When Homa changes a value, the change applies to new reservations only. Each reservation keeps the values that were in force when it was requested. Each provider's offering keeps to Homa's ranges: when Homa narrows the duration range, each provider's minimum and maximum hours shrink to fit inside it, and a provider left with no hours inside it goes offline until they set their hours and go live again. The provider is told either way. Widening the range doesn't widen any provider's hours; they set them again.

| Value | Used by | Setting |
|---|---|---|
| **Commission** | Pricing | 15% of what the provider earns from a reservation, taken from their payout; never a separate fee for the customer |
| **Duration range** | How Homa Works | 1 to 6 hours; each provider sets their own minimum and maximum within it |
| **Minimum notice** | How Homa Works | The start time must be at least 2 hours from now |
| **Booking horizon** | How Homa Works | Up to 30 days ahead |
| **Team reminder time** | Check-in Policy | 6 hours before the booked start, while the team is not named |
| **Earliest check-in** | Check-in Policy | 15 minutes before the booked start |
| **Check-in period** | Check-in Policy | 15 minutes after the booked start |
| **Check-in radius** | Check-in Policy | 300 metres |
| **Customer no-show wait** | Check-in Policy | 15 minutes |
| **Customer no-show fee** | Check-in Policy | Travel fee plus 30% of the hours fee |
| **No-show report window** | Check-in Policy | 1 hour after the provider reports a customer no-show |
| **Request expiry time** | Request Policy | 24 hours, or the response deadline if that comes sooner |
| **Response deadline** | Request Policy | 1 hour before the booked start: every request is answered by then. It is always shorter than the minimum notice |
| **Completion reminder time** | Completion Policy | 30 minutes after the booked end |
| **Completion deadline** | Completion Policy | 24 hours after the booked end, after which Homa reviews the reservation |
| **Evidence retention period** | Completion Policy | 120 days after the reservation is resolved, so evidence is there for card chargebacks and police reports |
| **Dispute window extension** | Dispute Policy | 24 hours after the later of the booked end and job completion |
| **Fee window** | Cancellation Policy | 24 hours before the booked start |
| **Cancellation tiers** | Cancellation Policy | Flexible: free cancellation. Standard: 10% of the hours fee within the fee window. Firm: 20% of the hours fee within the fee window |
| **Provider late cancellation fine** | Provider Cancellation Fines | 20% of the hours fee, when the provider cancels within the fee window before the booked start |
| **Provider no-show fine** | Provider Cancellation Fines | 30% of the hours fee, for no check-in, or a cancellation after the booked start or after check-in |
| **Fine deduction cap** | Provider Cancellation Fines | 50% of each payout, after Homa's commission |
| **Rating window** | Rating Policy | 14 days after the reservation becomes Completed |
| **Minimum ratings to show** | Rating Policy | 3 ratings before average stars are shown |

## Key Experiences

### Signing up and signing in

Customers and providers sign up and sign in the same way: **Continue with Google**, **Continue with Apple**, or their phone number with an SMS code. Homa keeps no passwords.
- Every account has a verified Malaysian mobile number. Signing up with Google or Apple asks for the phone number once, verified by an SMS code; after that, Google or Apple signs the person in with no code.
- The phone number is always a way in, so losing a Google or Apple account never locks anyone out. In their account, a person can add or remove Google or Apple sign-in at any time.
- A Google or Apple account signs in to at most one customer account and one business account, the same as a phone number. If it is already used for another account of that kind, the person is asked to sign in to that account instead.
- From Google or Apple, Homa takes only the account id and the email address it shares, which can be Apple's private relay address, and uses them only to sign the person in. A customer's name can be filled in from it, and changed, before they register.
- A business account is signed in to by its owner or director. Team members have no accounts.

### Account

Customers register with Google, Apple or their phone number, and give their name and their address. They can change their name and address in their account at any time. A new address is used for new searches and requests; requests and reservations already made keep the address they were made with. Customers search and browse as guests, and sign up the first time they reserve or chat. Providers see the customer's name on the reservation; Homa uses the address to match providers by city and to locate check-in. The customer confirms the address with a pin on the map, so an address the geoservice can't find is still placed correctly. The account also keeps **access notes** for the team, up to 500 characters. A phone number has at most one customer account and one business account; the same person can have one of each. The account shows the customer's payments and refunds with their status.

### Homepage

A greeting and a search bar to start a search. Below it is the **showcase**: searches Homa features, such as "Weekend quick refresh", in the order Homa sets. Tapping one fills in the search with its jobs, duration and helpers; the customer picks the date and time, and the city comes from their address, or the one a guest picks.

### Provider page

- Stats under the provider's name: jobs done, cancellations, average stars with the rating count, and price and minimum hours. **Jobs done** counts Completed reservations where the provider was paid more than RM0. **Cancellations** counts the provider's late cancellations and no-shows, the ones that carry a fine, waived or not, and is shown next to jobs done, for example "2 cancellations in 40 jobs".
- Jobs grouped to be easy to scan, such as kitchen or living room
- Tools the provider brings, and tools they don't, so you know what's missing
- The provider's cancellation policy
- **Instant book**, when the provider uses it
- **Insured**, when the provider has declared liability insurance that is still valid
- **Business · SSM registered**, for every provider
- The provider's team: every team member's registered name, role (helper or supervisor), photo and average stars, so you know who may come. Only added team members are shown. Nationality is never shown.
- When a supervisor is included, if the provider sends one
- Ratings: the average for each provider category once it has the minimum ratings to show, and written feedback from customers, newest first
- A **Verified ID** badge: every business and team member is verified by Homa's KYC provider, re-verified regularly and face-checked at every job, with the date of their last verification


### Ratings

After a reservation becomes Completed, the customer is asked to rate it: overall stars and optional category stars for the provider, optional written feedback, and, for any helper they choose, overall stars and optional category stars. When the provider names who is coming, the customer sees each helper's overall average.

### Availability

Providers manage their availability in the app and sync with their calendar (Google Calendar or Apple Calendar), so results reflect when a provider can actually work. A provider can also set a **buffer between jobs**, time kept free before and after each accepted reservation, for example to travel: a new booking cannot start within the buffer after another job or end within it before one. The buffer applies around Homa jobs only, not around synced calendar events, and changing it does not affect bookings already accepted.

Availability times that run back to back count as one stretch. A calendar event or booking that ends exactly when a slot starts does not block that slot.

### Chat

Every chat belongs to one reservation.
- **Enquiry:** a customer who wants to talk before requesting picks the jobs, date and time, duration and number of helpers on the provider page and taps **Chat**. This creates an **enquiry** with its own chat. An enquiry has no payment and is not a reservation status.
- **Reserve from the chat:** when they have agreed, the customer taps **Reserve** in the chat. This opens the same review page as a direct reservation, filled in with the enquiry details. The customer can edit the details there, then submits. Homa checks the provider's availability again, takes payment as in Payment Protection, and the enquiry becomes a reservation in **Requested**. The same chat carries on.
- Enquiry details are only edited on that review page, never in the chat itself.
- A customer can also request directly without chatting first. Requesting opens the reservation's chat, with no message yet, so both the customer and the provider can write in it from then, for example about access to the home or running late.
- An enquiry that is never requested closes when its date passes. Its chat is archived and stays readable.
- The chat stays open for the life of the reservation, including after the job, for follow-ups.
- **Book again:** from any past enquiry or reservation, the customer can start a new enquiry with the same provider, jobs, duration and helpers, at their current address and a date and time they pick.
- In the inbox, chats are grouped by provider, so a regular customer sees all their chats with one provider together.
- Only the customer starts a chat, by tapping **Chat** or by requesting. Once a chat is open, both can write.
- Every chat shows a reminder that payments and messages outside Homa are not covered by Homa Protection, which includes refunds, disputes and cancellation rules.

## Provider App

Homa has a separate app that helps providers manage their reservations, calendar and offering, and monitor their income.

### Onboarding

1. **Register the business** with Google, Apple or a phone number; the phone number is verified by an SMS code either way.
2. **Verify:** complete KYB on the KYC provider's pages: the business's SSM registration and the owner's or director's KYC, in one flow.
3. **Set up your team:** a business starts with no team members, and can add them once its KYB has passed. Add helpers and supervisors with the person beside you: choose the role and take their profile photo, they verify on the spot with the KYC provider on your phone, scanning their own document and taking their own selfie, and once verified you confirm they are lawfully employed by or engaged with you. They join your team then.
4. **Set up the profile and offering:** profile name and picture, and the offering, including when a supervisor is sent.
5. **Add a payout account:** a business bank account.
6. **Go live** once everything is set up.

Steps 4 and 5 can be done while KYB is being checked. Providers cannot go live until the business is verified through KYB, its profile is complete, its payout account is added, and its team has at least as many helpers as its minimum helpers per booking, plus a supervisor if it set a supervisor threshold. People still waiting for verification do not block going live. When a provider tries to go live too early, Homa lists everything still missing.

### Teams

A provider adds helpers and supervisors and manages the team in Homa. Only people who are verified and confirmed are team members. Team members do not have their own accounts in the MVP; the provider app on site is used for check-in, face checks and completion. For each reservation the provider names the helpers, and a supervisor when one is due, before the booked start.

### After going live

- **Reservations:** accept or decline requests, name the helpers and any supervisor for each reservation, run the face checks at check-in, check in on the job day, and mark jobs completed with photo or video evidence.
- **Calendar:** manage availability, set a buffer between jobs, and sync with Google Calendar or Apple Calendar.
- **Offering:** manage the jobs they offer (from Homa's job list), the tools they bring (from Homa's tool list), the minimum and maximum helpers per booking, when a supervisor is sent, the hourly rate per helper (shown with what they receive after Homa's commission), how the travel fee works (separate or included in the price), minimum and maximum hours, cities covered, cancellation policy, acceptance policy (Request and accept or Instant book) and any liability insurance. A change applies to new requests only; a request already made keeps the offering it was made with.
- **Verification:** see when the business and each team member are next due for re-verification, and run each re-verification on the spot with the person present.
- **Income:** monitor earnings, Homa's commission, payouts and any fines being deducted.

## Homa Admin

Homa has an internal admin console used only by the founders for now. It has two jobs: tracking the important information about the platform, and handling reports.

### Reports

- One queue for everything that needs Homa's decision. **Cases** are customer and provider reports, wrongful no-show reports, missed completion deadlines and card chargebacks, each decided with a release, a partial refund or a full refund. If the other party also reports while a case is open, their report and evidence join the same case. Safety, damage or loss reports come first; the rest are oldest first. Each case shows how long is left of its 72-hour decision target.
- **Seriously flagged businesses** are listed in the same queue but are not cases: each of their disputed reservations is cancelled with a full refund or restored once the flag is cleared, and each held payout is released or kept back. A case already open for a reservation the flag takes over is closed without a decision.
- For each case the admins see the reservation details, evidence, check-in record and chat, and decide on a release, a partial refund or a full refund. Chats and evidence are opened only for a case.
- Each decision is recorded with a short reason and the admin who made it, and both the customer and the provider are notified of the outcome. Every change made in the admin console is recorded with who made it and when.
- Fine waivers are decided here too.

### Tracking

- **Overview:** the measures in Goals and Measures, with reservations, total booking value, commission earned, cancellation and no-show rates, the share of bookings made by Instant book, and how quickly providers respond to requests.
- **Reservations:** search and view any enquiry or reservation with its full history: status changes, payment states, check-in time and location result, and completion evidence.
- **Payments:** authorisations, customer payments, held payments, releases, payouts and refunds, including failed payments, refunds and payouts.
- **Providers:** KYB results (SSM number and registered business name), lawful-employment confirmations for each team member with their time and terms version, verification status of providers, their team members and anyone waiting for verification, face check failures, upcoming and lapsed re-verifications, and the option to ask the KYC provider for an early re-check with a reason, offerings, cancellation tier, cancellation count, fines and the record of late cancellations and no-shows.
- **Customers:** accounts, reservations and reports filed.
- **Fines:** every fine with its reason, amount, what has been deducted so far, and any waiver.
- **Ratings:** every rating, with the option to hide one that breaks the rules, recorded with a reason.

### Settings

- **Catalogue and curation:** manage the job list and its groups, the tool list, and the homepage showcase: feature, rename, move and remove searches. Each featured search has its own place in the showcase. Retiring a job or tool needs a reason, because it changes providers' offerings.
- **Homa-set values:** view and change the values used by the policies, one at a time and with a reason. A change applies to new reservations only.
- **Data requests:** handle requests to see, correct, copy or delete personal data, and record each one.

## Not in the MVP

These are deliberately left out of the MVP:

- Providers rating customers (two-way ratings)
- Ratings on cancelled reservations or no-shows
- Consequences of violations and provider suspension (late cancellations and no-shows are only tracked in the admin console)
- Homa credit, account top-ups and e-wallet payments
- Rescheduling a reserved booking
- Recurring bookings, and asking for the same team again. They are the first step after launch, toward the 9-star experience; until then, **Book again** copies a past booking in one tap.
- Accounts for helpers and supervisors
- Passwords: people sign in with Google, Apple or an SMS code
- Showing nationality
- Checking work permits (the provider confirms lawful employment and is responsible for it)
- Independent providers: every provider is a registered business
- Homa's own identity checks: the KYC provider does them, including document rules, re-verification schedules and face matching
- Skilled trade jobs such as plumbing, air-conditioning or electrical work

## External Services Homa Will Use and Not Develop

Homa will use outside services for these instead of developing them, so its own work goes into value for homeowners and homecare providers. Stripe Connect is chosen for payments, and Google and Apple for signing in; the others are still to be chosen. Homa's rules do not depend on which KYC provider is used.

| Capability | What it does for Homa |
|---|---|
| **KYC provider (to be chosen; Persona and Innov8tif are candidates)** | Verifies businesses through KYB, with the owner's or director's KYC; verifies and re-verifies team members on the spot; matches faces at check-in; monitors businesses and team members, reporting only flags it has confirmed; and reports a final result for every check, including a timed-out session. Homa's requirements: the owner or director is Malaysian, with a MyKad or a Malaysian passport; team members use an identity card or passport from any country; businesses re-verified every year, and team members every year and when their document expires, with a reminder 14 days before; **ongoing monitoring** of registry changes, document expiry and sanctions or watchlists, reported as "re-verification needed by a date" or a serious flag; an early re-check on Homa's request, to be passed within 14 days; up to 3 face check attempts per person at check-in; a face comparison of the provider's profile photo with the verified selfie; and the date of birth from the document |
| **Payment provider (Stripe Connect)** | Authorises and charges cards, takes upfront FPX payments, holds payments until release, onboards providers for payouts, pays out to providers and processes refunds to the original method |
| **Push notification service (to be chosen)** | Sends reminders and alerts to customers and providers, such as new requests, check-in, completion and reports |
| **Geoservice (open source)** | Handles addresses, locations and distance, including the location check at check-in |
| **Sign-in providers (Google and Apple)** | Sign customers and providers in with their Google or Apple account, so Homa keeps no passwords; share only the account id and an email address |
| **SMS provider (to be chosen)** | Sends phone verification codes when customers and businesses register, and when people sign in with their phone number |
| **Calendar services** | Google Calendar and Apple Calendar sync for provider availability |
