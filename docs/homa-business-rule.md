# Homa Business Rule

> Ingested from Notion on 2026-11-22 (version 1.6, page edited 06:17 UTC): https://app.notion.com/p/41e8436ade4081e3b127ca594a99f2d9
> The Notion page is the source of truth; refresh this copy when it changes.

Homa is a marketplace that helps homeowners get the best homecare service providers.

Each rule has an ID so specs, tests and support decisions can refer to it. "Must" is mandatory, "can" is allowed, and "is" states a fact or value. Every rule has a **case**: a short story showing the rule in action, using the example setup.

**Version 1.6** · 22 November 2026 · Every change is recorded in the [Homa Business Rule Log](https://app.notion.com/p/4208436ade408159bd37faabc34a24c3).

---

## Principles

Every rule follows these principles. When a new question comes up, answer it from the principles first.

| # | Principle | What it means |
| --- | --- | --- |
| P1 | **Paid is committed.** | Nothing changes until the money is confirmed. The first confirmed payment wins, and any other payment is refunded at once. |
| P2 | **A change is a new booking.** | Reservations can't be edited. To change one, the customer withdraws or cancels it and makes a new reservation under the terms in force at that moment. |
| P3 | **A reservation keeps what it was booked with.** | A reservation keeps the provider's terms and Homa's reservation values (§13) from when it was Requested. Account values apply at once. |

## Writing principles

Every rule is written to these principles, and every change is checked against them before it's applied.

| # | Principle | What it means |
| --- | --- | --- |
| W1 | **State what's true.** | A rule describes how Homa works now. It never describes a change, an older rule, or an exception to another rule. |
| W2 | **One rule, one idea.** | If a rule needs "unless", "except" or a list of special cases, split it or rethink it. |
| W3 | **Say it once.** | Each fact lives in one rule. Other rules point to it by ID only when the reader needs it. |
| W4 | **What, not how.** | Rules say what must be true. How it's checked or built belongs in the spec, §11 or §14. |
| W5 | **A change is a rewrite.** | A changed rule is rewritten as if new, every rule that points to it is updated, and the change is recorded in the Log. |
| W6 | **Every rule has a case.** | Cases use the example setup, and every number in them adds up. |

---

## 0. Example setup

All cases use these people and numbers unless the case says otherwise. Names and businesses are fictional.

|  |  |
| --- | --- |
| **Customer** | Aisyah, a working mother in Salak Tinggi. She's identity verified. |
| **Provider** | Kemas Home Sdn Bhd, a Business provider run by its owner Azman, covering Salak Tinggi, Cyberjaya and Putrajaya. |
| **Second provider** | Hana Cleaning, run by its owner Hana, who also works as its helper. It isn't a Business provider. Covers Salak Tinggi. Used only where a case says so. |
| **Team** | Helpers Siti and Nurul (local), helper Rina (foreign), supervisor Farid (local). |
| **Rate** | RM25 per helper per hour. |
| **Travel fee** | RM20 flat. |
| **Discount** | 10% for reservations of at least 3 hours and 2 helpers. |
| **Minimum notice** | 2 hours. |
| **Acceptance policy** | Request. |
| **Cancellation tier** | Standard. |
| **Standard reservation** | Saturday 10:00–14:00 (4 hours), 2 helpers. |

**Standard reservation numbers**

| Item | Calculation | Amount |
| --- | --- | --- |
| Hours Fee | 25 × 4 hours × 2 helpers × (1 − 10%) | RM180 |
| Total Reservation Fee | 180 + 20 | **RM200** |
| Base Cancellation Fee | 25 × 2 helpers × 1 hour × (1 − 10%) | **RM45** |
| Discounted hourly cost (both helpers) | 25 × 2 × 0.9 | RM45 per hour |
| Check-in Period |  | Saturday 9:45–10:00 |
| Acceptance deadline | Requested Monday 20:00 + 24 hours (earlier than Saturday 8:45) | Tuesday 20:00 |
| Protection Window |  | Saturday 9:45 → Sunday 14:00 |

---

## 1. Definitions

| Term | Definition |
| --- | --- |
| **Customer** | A registered homeowner who makes reservations. |
| **Provider** | A homecare provider on Homa, run by one owner (PRV-1). |
| **Owner** | The identity-verified person who runs a provider account. |
| **Business provider** | A provider whose registered business, owner link and business payout account are verified (PRV-7). |
| **Helper** | A verified team member who does the jobs. Billed per hour. |
| **Supervisor** | An optional verified team member who oversees helpers. Not billed. |
| **Member** | A helper or supervisor in a provider's team. |
| **Support** | Homa's support team, which verifies, confirms and resolves reports. |
| **Active** | Verified and not suspended; for a member, also not deactivated by the provider. |
| **Requested** | The customer has paid and the payment is held. |
| **Reserved** | The reservation is confirmed, instantly or by the provider accepting it. |
| **Reservation statuses** | Requested → Reserved → Completed. A reservation can also end as Withdrawn, Declined, Expired or Cancelled. Cancelled covers every cancellation, no roster, no show and suspension. |
| **Enquiry** | A chat a customer starts with a provider before reserving. |
| **Job time** | The booked period from job start to job end. Billing counts from the job start. |
| **Check-in Period** | The 15 minutes before the job start, when the rostered team arrives. It's an agreed arrival time, not a step in the app. |
| **Notice** | The time from making the reservation to the start of the Check-in Period. |
| **Protection Window** | From the start of the Check-in Period until 24 hours after the job end. |
| **Hours Fee** | Per-helper hourly rate × hours × helpers, after discount. |
| **Total Reservation Fee** | Hours Fee + travel fee. |
| **Base Cancellation Fee** | Per-helper hourly rate × helpers × 1 hour, after discount. |
| **Fee** | An amount one party pays the other. |
| **Fine** | A penalty on the provider, deducted from its payouts. |
| **Local** | A person who can work in the country without a work permit, as the country policy sets (§14). |
| **Foreign** | A person who needs a work permit to work in the country (§14). |

---

## 2. Customer

| ID | Rule | Case |
| --- | --- | --- |
| CUS-1 | A customer registers with a name, a home address with its city, and an arrival note. | Aisyah enters her name, her Salak Tinggi address and "Guardhouse: say unit B-12-3". No phone number is asked for. |
| CUS-2 | A customer must agree to the Terms of Service to register. | Aisyah can't finish registering until she ticks the Terms of Service. |
| CUS-3 | Only identity-verified customers can make reservations. A customer verifies at their first reservation, before paying; browsing and enquiries don't need it. | Aisyah's husband registers and asks Kemas Home about a ladder. When he taps Reserve, he's asked to verify first. |
| CUS-4 | Support can suspend a customer after a confirmed report or repeated confirmed no-shows: for 7 days, 30 days, or until Support lifts it. | Daniel threatens Kemas Home's helpers. Support confirms the report and suspends him for 30 days. |
| CUS-5 | A suspended customer can't reserve or start enquiries. Their reservations whose Check-in Period hasn't started are cancelled with a full refund and no fees. A job already in its Check-in Period or job time finishes as booked. | Daniel's request for next week is cancelled and fully refunded. His job running that afternoon finishes as booked. |
| CUS-6 | A 7- or 30-day suspension ends by itself. A suspension until lifted ends when Support lifts it. | Daniel can book again after 30 days. A customer suspended until lifted waits for Support to review an appeal. |
| CUS-7 | A customer signs in through an identity provider, such as Google or Apple. One identity-provider account holds one customer account. | Aisyah signs in with Google. Daniel can't open a second account with the same Google account. |
| CUS-8 | A customer can deactivate their account when they have no Requested or Reserved reservations, no open reports and no reservation in its Protection Window. Signing in again reactivates it. A suspended customer can't deactivate. | Aisyah's Deactivate is blocked on Saturday at 15:00 and works after Sunday 14:00. A month later she signs in and her account is back. |
| CUS-9 | A customer can ask Support to delete their data, under the same conditions as deactivation. Everything is deleted or anonymised at once, except records still in their retention period (§10). Ratings they wrote stay, shown as from a former customer. | Aisyah's name and address are removed, her payment records stay until their period ends, and her rating of Kemas Home shows as from a former customer. |
| CUS-10 | A customer must be aged 18 or above, with a status the country policy allows for customers (§14). One verified identity holds one customer account. | Aisyah verifies with her MyKad. Her 16-year-old son can't book. Daniel can't open a new account with another Google account, because his MyKad is already used. |

---

## 3. Provider

### 3.1 Provider account

| ID | Rule | Case |
| --- | --- | --- |
| PRV-1 | Every provider has one owner: an identity-verified person aged 18 or above, with a status the country policy allows (§14). The owner runs the provider account. | Hana verifies with her MyKad and opens Hana Cleaning. Azman, also verified, opens Kemas Home. |
| PRV-2 | A provider can be booked only when it has a verified payout account in its name and at least one active helper. | Kemas Home can't go live until its payout account is verified. When all its helpers are deactivated for a holiday week, it disappears from search. |
| PRV-3 | Supervisors are optional. | Kemas Home goes live with Siti, Nurul and Rina, and adds Farid as a supervisor a month later. |
| PRV-4 | A provider is tagged **Insurance Covered** while Support has confirmed a valid public liability policy. | Kemas Home's policy runs until 31 March. The tag shows until then and returns when a renewal is confirmed. |
| PRV-5 | A provider can't close its account while it has Requested or Reserved reservations. | Kemas Home must cancel Aisyah's Saturday reservation, and is fined RM45 (PCX-1), before it can close on Wednesday. |
| PRV-6 | The owner can also work as a helper or supervisor in their own team. | Hana is Hana Cleaning's only helper. Azman doesn't clean, so he isn't in Kemas Home's team. |
| PRV-7 | A provider is a **Business provider** while Homa has verified all three: its registered business; a payout account in the business's name; and the owner as the business's owner, director or authorised representative, using the owner's verified identity (IDN-1). It shows a "Business" tag. | Azman verifies Kemas Home Sdn Bhd and adds the company's bank account. His verified MyKad identity confirms him as the company's director. Kemas Home shows "Business". |
| PRV-8 | Only Business providers can have foreign members. | Kemas Home can add Rina. Hana Cleaning can't add a foreign helper. |
| PRV-9 | When a provider stops being a Business provider, its foreign members are suspended. Everyone else, and its ratings, stay. | Azman resigns as director. Kemas Home loses the Business tag and Rina is suspended; Siti and Nurul keep working. |
| PRV-10 | A provider can change its payout account. Payouts wait until the new account is verified, and the owner is told of every change. | Kemas Home changes banks on Thursday. Its Sunday payout is paid after the new account is verified on Monday. |
| PRV-11 | Everyone signs in with their own account. A rostered member acts for the provider on the reservations they're rostered on. | Siti, rostered on Saturday, uploads the after-photo and marks the job Completed. Azman never shares his sign-in. |
| PRV-12 | Support can suspend a provider or a member after a confirmed report: for 7 days, 30 days, or until Support lifts it. | Aisyah's Safety & Misconduct report against a helper is confirmed. Support suspends the helper for 30 days. |

### 3.2 Members

| ID | Rule | Case |
| --- | --- | --- |
| HLP-1 | Every member must be aged 18 or above. | Kemas Home can't add a 17-year-old trainee. A helper who turned 18 last week can be added. |
| HLP-2 | A local member is verified as local (IDN-1). | Siti verifies with her MyKad. |
| HLP-3 | A foreign member must be allowed to work in the country, with at least 90 days left on their work permit when added. | Rina's permit has 200 days left, so she's added. A candidate with 60 days left waits until their permit is renewed. |
| HLP-4 | The provider confirms that each foreign member works lawfully for its business. | Kemas Home confirms Rina is employed by Kemas Home Sdn Bhd. Without it, Rina can't be added. |
| HLP-5 | A provider can deactivate a member and reactivate them. | Siti is deactivated for two weeks' leave and reactivated when she's back. |
| HLP-6 | A deactivated member can't be rostered. | Nurul, deactivated on Thursday, has to be replaced on Saturday's roster before 9:45 (ROS-5). |
| HLP-7 | A member can be reactivated only with a valid verification. | Farid's re-verification fell due while he was deactivated, so he re-verifies before Kemas Home reactivates him. |
| HLP-8 | A provider can remove a member, and must remove one who no longer works for it. A removed member can only come back as a new member, verified again. | Rina resigns and is removed the same day. If she returns a year later, she's added and verified as new. |
| HLP-9 | A member can't be removed while they're in the Check-in Period or job time of a reservation they're rostered on. | Rina quits at 11:00 during a job. Kemas Home reports Roster Member Left and removes her at 14:00. Had she resigned on Wednesday, she could be removed then and replaced (ROS-5). |
| HLP-10 | A person belongs to only one provider at a time, as its owner or a member, active or deactivated. | Siti, a Kemas Home helper, can't open her own provider or join Hana Cleaning until Kemas Home removes her. |

### 3.3 Verification monitoring

| ID | Rule | Case |
| --- | --- | --- |
| MON-1 | Owners and members re-verify every 6 months, counted from their last pass, with reminders from 60 days before. | Siti, last verified on 1 June, is due on 1 December and reminded from 2 October. She re-verifies on 20 November and is next due on 20 May. |
| MON-2 | When monitoring asks for re-verification, it must be passed within 14 days. | Nurul is flagged on 5 November and must pass by 19 November. |
| MON-3 | Missing a re-verification deadline suspends the person until they pass it. | Nurul misses 19 November and is suspended. She re-verifies on 22 November and is active again. |
| MON-4 | When monitoring flags a suspension, the person is suspended at once and only Support can reactivate them. | Farid is flagged and suspended at once. Support reviews and reactivates him a week later. |
| MON-5 | A Business provider's business and the owner's link to it are re-verified every 6 months. Missing it ends Business status (PRV-9). | Kemas Home's business re-verification is due on 1 June. Azman misses it, so Kemas Home loses the Business tag and Rina is suspended until it's passed. |
| MON-6 | When monitoring flags a Business provider's business, the provider is suspended at once, and only Support can reactivate it. | Kemas Home Sdn Bhd is struck off on Wednesday and flagged. Kemas Home is suspended: Aisyah's Saturday booking is cancelled with a full refund and no fine (SUS-3). |

### 3.4 Identity

| ID | Rule | Case |
| --- | --- | --- |
| IDN-1 | Every customer, owner and member is verified with the identity document the country policy sets for their status (§14). Without it, they can't be verified. | Aisyah and Siti verify with their MyKad, and Rina with her i-Kad. A foreign candidate without an i-Kad can't be added. |
| IDN-2 | For each customer, owner and member, Homa stores their name, profile photo (if they have one), local or foreign status, verification results and identity document number. For a Business provider, it also stores the business's registered name and number. Homa doesn't store copies of identity documents or biometric data. | Homa holds Siti's name, photo, "local", "passed, 20 November" and her MyKad number, but not a picture of her card or her face scan. |
| IDN-3 | The identity document number is used only to recognise a person and to check payout accounts. Customers and providers never see it. In Homa, only Support can view it, and every view is recorded. | Hana closes Hana Cleaning owing RM16.28. When she tries to open it again, her MyKad number matches, so she pays the debt first (PAY-9). |
| IDN-4 | Every owner and member has a verified profile photo: a photo of their face, checked by the verification service as matching their identity. A new photo replaces the current one only once it's verified. A customer's profile photo is optional and isn't verified. | Siti's photo is checked against her MyKad verification and shown on the roster. When she uploads a new one, her old photo stays until the new one is verified. Aisyah chooses not to add a photo. |
| IDN-5 | Customers see the photos of providers and rostered members. A customer's photo, if they have one, and address are visible only to the owner and the rostered team, from Reserved until the Protection Window ends. | Siti sees Aisyah's photo and address from Monday 22:00 to Sunday 14:00. Rina, not rostered, never sees them. |

### 3.5 Offerings and settings

| ID | Rule | Case |
| --- | --- | --- |
| OFR-1 | A provider chooses its jobs, tools and cities from Homa's presets. | Kemas Home picks kitchen cleaning, laundry folding and bathroom cleaning, a HEPA vacuum, and Salak Tinggi, Cyberjaya and Putrajaya. |
| OFR-2 | A provider sets one hourly rate per helper. | Kemas Home sets RM25. A 2-hour, 2-helper job is 25 × 2 × 2 = RM100 before travel. |
| OFR-3 | A provider sets a travel fee: none, flat, or by city. | Kemas Home sets RM20 flat. Another provider sets RM10 for Salak Tinggi and RM25 for Putrajaya. |
| OFR-4 | A provider can set a discount of up to 99% for reservations of at least a set number of hours and helpers. | Kemas Home's 10% applies to Aisyah's 4-hour, 2-helper booking, not to a 2-hour one. |
| OFR-5 | A provider sets minimum and maximum hours and helpers. By default: 1 to 8 hours, and 1 helper up to its number of active helpers. | Kemas Home sets 2 to 6 hours and keeps the helper default; with 3 active helpers, customers book 1 to 3. |
| OFR-6 | A provider sets a minimum notice: by default 2 hours, and at least 1 hour for Instant or 2 hours for Request. | Kemas Home (Request) can't go below 2 hours. An Instant provider at 1 hour that switches to Request moves to 2 hours and is told. |
| OFR-7 | A provider can show proof-of-work photos from past jobs. | Kemas Home shows before-and-after photos of a kitchen. |
| OFR-8 | A provider sets its working days and hours, and can sync external calendars. | Kemas Home works Monday–Saturday 8:00–18:00. A synced Tuesday 14:00–16:00 appointment makes that time unavailable. |
| OFR-9 | A provider has one acceptance policy (§4.3) and one cancellation tier (§6.2), shown before a customer reserves. | Kemas Home's page shows Request and Standard. |
| OFR-10 | A provider sets a buffer between jobs: by default 30 minutes. | Kemas Home keeps 30 minutes. A provider covering a wider area sets 60. |
| OFR-11 | The hourly rate after any discount is at least Homa's minimum rate of RM20. | At RM25, Kemas Home's largest discount is 20%. Its 10% gives RM22.50. A provider charging RM40 can offer up to 50%. |

---

## 4. Reservation

### 4.1 Making a reservation

| ID | Rule | Case |
| --- | --- | --- |
| RES-1 | A customer can reserve only a provider that covers the city of their address. | Aisyah in Salak Tinggi can reserve Kemas Home. Her sister in Shah Alam can't. |
| RES-2 | A reservation has at least one of the provider's jobs, a date, a start time, hours, helpers and the customer's address. | Aisyah picks kitchen cleaning and laundry folding, Saturday 10:00, 4 hours, 2 helpers and her saved address. |
| RES-3 | The job time falls within the provider's working hours and free time. The Check-in Period and buffer can fall outside working hours. | With a job elsewhere 8:00–10:00 and a 30-minute buffer, Aisyah's earliest start is 10:45. A job can start at 8:00, but can't run 17:00–19:00. |
| RES-4 | A reservation's Notice is at least the provider's minimum notice. | At Saturday 7:00, with 2 hours' notice, the earliest Check-in Period starts at 9:00, so the earliest start is 9:15. |
| RES-5 | A reservation lasts at least 1 hour, in 15-minute steps. | Aisyah can book 1h 15m but not 1h 10m. With Kemas Home's 2-hour minimum, she starts at 2 hours. |
| RES-6 | Total Reservation Fee = rate × hours × helpers × (1 − discount) + travel fee. | 25 × 4 × 2 × 0.9 + 20 = RM200. |
| RES-7 | When payment is confirmed, the reservation is Requested and the payment is held. | Aisyah pays RM200 on Monday 20:00. Her reservation is Requested. |
| RES-8 | A reservation keeps the provider's terms, Homa's reservation values and the customer's address from when it was Requested (P3). | Kemas Home raises its rate to RM30 and switches to Firm on Wednesday; Aisyah's Saturday booking stays RM200 under Standard. Her new address on Thursday applies only to new bookings. |
| RES-9 | A provider has one reservation at a time. A Requested or Reserved reservation blocks the provider from the start of its Check-in Period until the job end plus buffer. | Aisyah's request blocks Kemas Home on Saturday 9:45–14:30 from Monday 20:00. No one can book it at 11:00; the next start is 14:45. A decline or expiry frees the time. |
| RES-10 | A customer can't have two Requested or Reserved reservations whose job times overlap. | Aisyah can't also request another provider for Saturday 12:00, but can book Sunday. |
| RES-11 | When several customers pay for the same provider time, the first confirmed payment wins. Every other payment for that time is refunded in full at once. | Aisyah's payment confirms at 20:01 and Mei's at 20:02. Aisyah's is Requested; Mei gets RM200 back straight away. |
| RES-12 | When one checkout is paid twice, the first confirmed payment counts and the other is refunded in full at once. | Aisyah's card retry confirms at 20:04 and her slow bank payment at 20:06. The 20:06 payment is refunded. |

### 4.2 While Requested

| ID | Rule | Case |
| --- | --- | --- |
| REQ-1 | Reservations can't be edited (P2). | Aisyah wants 3 hours instead of 4. She withdraws and requests 3 hours (RM155) as a new request, with a new deadline. The slot isn't held in between (P1). |
| REQ-2 | A customer can withdraw a Requested reservation for free. It becomes Withdrawn, the payment is refunded in full, and the time opens. | Aisyah withdraws on Monday 21:00 and gets RM200 back. |
| REQ-3 | A provider can decline a Requested reservation. The customer is refunded in full. | Kemas Home declines because two helpers are on leave. Aisyah gets RM200 back. |
| REQ-4 | A request not accepted by its deadline expires, and the customer is refunded in full. | Kemas Home doesn't respond by Tuesday 20:00. Aisyah gets RM200 back. |
| REQ-5 | When a withdrawal and an acceptance happen together, the first one recorded wins. | Kemas Home accepts at 21:02:10 and Aisyah withdraws at 21:02:12. The reservation is Reserved, and she can cancel for free within her Grace Period (CCX-1). |

### 4.3 Acceptance policies

| ID | Rule | Case |
| --- | --- | --- |
| ACC-1 | **Instant:** a reservation is Reserved as soon as it's Requested. It must be made before the Check-in Period starts. | With an Instant provider, Aisyah's booking is Reserved the moment she pays. |
| ACC-2 | **Request:** a reservation is Reserved when the provider accepts it by the deadline. | Kemas Home accepts on Monday 22:00, and Aisyah's booking is Reserved. |
| ACC-3 | The acceptance deadline is 24 hours after the request or 1 hour before the Check-in Period, whichever is first. | A Monday 20:00 request has until Tuesday 20:00. A Saturday 7:00 request for 10:00 has until 8:45. |

### 4.4 Roster

| ID | Rule | Case |
| --- | --- | --- |
| ROS-1 | The provider sets the roster before the Check-in Period and can change it until then. The customer is told of every change. The roster locks when the Check-in Period starts. | Kemas Home rosters Siti and Nurul on Friday, swaps Nurul for Rina at 8:00 on Saturday, and the roster locks at 9:45. |
| ROS-2 | The roster has the booked number of helpers. Supervisors are extra and not billed. | Aisyah booked 2 helpers. Siti, Nurul and Farid is complete; Siti and Farid isn't. |
| ROS-3 | Only active members can be rostered. | Nurul is suspended, so Kemas Home rosters Siti and Rina. |
| ROS-4 | A foreign member can be rostered only on jobs ending at least 90 days before their work permit expires. | Rina's permit expires on 20 March, so she can work jobs up to 20 December, not 2 January. |
| ROS-5 | When a rostered member stops being active before the Check-in Period, the provider replaces them before it starts. | Nurul is suspended on Friday. Kemas Home rosters Rina before Saturday 9:45. |
| ROS-6 | The customer sees each rostered member's name and photo. | On Friday Aisyah sees Siti, Nurul and Farid. A stranger arrives on Saturday, so she reports Safety & Misconduct. |
| ROS-7 | A roster that isn't complete when the Check-in Period starts triggers the No Roster Policy (§6.4). | Nurul is suspended on Friday and not replaced by 9:45. The No Roster Policy applies. |
| ROS-8 | A provider short of helpers can offer a smaller roster before the Check-in Period. If the customer accepts, the price drops with the discount kept, the difference is refunded, and there's no fine. | Kemas Home offers Siti alone. Aisyah accepts: 25 × 4 × 1 × 0.9 + 20 = RM110, so she gets RM90 back. Had she declined, ROS-7 would apply. |
| ROS-9 | A rostered member suspended during a job for missed re-verification or loss of Business status finishes the job, and is suspended when it ends. | Nurul's deadline passes at 11:00, so she finishes and is suspended from 14:00. Kemas Home loses Business status at 11:00, so Rina also finishes and is suspended from 14:00. |
| ROS-10 | A rostered member suspended during a job by a monitoring flag or by Support stops at once, and a Roster Member Left report is created. | Rina is flagged at 11:00 and stops. Aisyah is refunded her remaining 3 hours: 3 × 25 × 0.9 = RM67.50. |

### 4.5 Chat

| ID | Rule | Case |
| --- | --- | --- |
| CHT-1 | Only a customer can start a chat: as an Enquiry, or from a reservation. | Aisyah asks Kemas Home about a ladder before booking. Kemas Home can't message customers who haven't contacted it. |
| CHT-2 | A reservation's chat is open from Requested until the Protection Window ends. | Kemas Home messages "We're at the guardhouse" at 9:40. Aisyah messages about a missed corner on Sunday morning. |
| CHT-3 | Every chat shows that payments and arrangements outside Homa aren't protected. | Kemas Home offers a cash discount, and the reminder shows a cash job wouldn't be covered. |
| CHT-4 | Support can read a reservation's chat as evidence for a report. | Support reads Saturday's messages to see when Nurul left. |
| CHT-5 | A reservation's chat becomes read-only when its Protection Window ends, or when the reservation ends before its Check-in Period. New questions go in a new Enquiry. | Aisyah can read Saturday's chat on Monday but not reply. Had she cancelled on Wednesday, it would be read-only from then. |

### 4.6 Arrival and job time

| ID | Rule | Case |
| --- | --- | --- |
| JOB-1 | The rostered team arrives during the Check-in Period. Arrival problems are handled through reports (§7, §8). | The team arrives at 9:52 and starts at 10:00. Had it arrived at 10:05, Aisyah could let it start or report a Provider No Show. |
| JOB-2 | The customer pays for the booked hours, even if the team finishes early. | The team finishes at 13:30. Aisyah pays for 4 hours. |
| JOB-3 | The provider marks the job Completed, with at least one after-photo, after the job end. No customer confirmation is needed. | The button appears at 14:00. Siti uploads a photo of the kitchen and marks the job Completed. |
| JOB-4 | The customer is told when the job is Completed. | Aisyah gets a notification at 14:01 with the after-photo. |
| JOB-5 | A job not yet marked Completed triggers reminders at the job end and 2 hours later. The payout waits until it's marked. | Kemas Home is reminded at 14:00 and 16:00, marks it on Monday, and is paid after that, with no fine. |
| JOB-6 | Completion photos are evidence for reports. | Support checks the after-photos when Aisyah reports the bathroom wasn't cleaned. |

---

## 5. Payment

| ID | Rule | Case |
| --- | --- | --- |
| PAY-1 | The customer's payment is held from the moment the reservation is Requested. | Aisyah's RM200 is held from Monday 20:00. |
| PAY-2 | A job's payout is released when the job is Completed, the Protection Window has ended and every report is resolved. | With no report, Kemas Home is paid at Sunday 14:00. |
| PAY-3 | Homa's platform fee is 15.5% of every payout to a provider, including fee payouts. | A RM200 payout pays Kemas Home RM169. A RM45 cancellation fee pays it RM38.02 after a RM6.98 platform fee. |
| PAY-4 | Refunds come from the held payment. Compensation is paid by Homa and recovered from the provider's fine; Homa keeps the rest of the fine. | After a provider no-show, Aisyah gets RM200 back plus RM20 from Homa. Kemas Home's RM105 fine repays the RM20, and Homa keeps RM85. |
| PAY-5 | Fines are deducted from the provider's payouts after the platform fee, at most 20% of each payout, until cleared. | Kemas Home owes RM105. From a RM300 payout (RM253.50 after the fee), RM50.70 is deducted; RM54.30 carries over. |
| PAY-6 | Fees owed to the provider (cancellation, last-minute and no-show) are paid out like a job's payout: when the booked job's Protection Window ends and every report is resolved. | Aisyah's RM45 Friday cancellation fee pays Kemas Home RM38.02 at Sunday 14:00. |
| PAY-7 | A refund is paid as soon as it's decided. | Aisyah's RM155 refund is sent at Friday 22:00, while Kemas Home's RM45 fee waits until Sunday. When Nurul leaves at 12:00, Aisyah's RM45 is sent at once. |
| PAY-8 | Each money line is rounded once to the nearest sen, half up, and a refund is the amount paid minus the rounded fee. | Nurul leaves at 12:05: the refund is 1.75 × 22.50 = RM39.375, rounded to RM39.38, so the booking totals RM160.62. |
| PAY-9 | When a provider closes, its pending payouts are released when due and its fines are deducted from them in full. What's still owed becomes a debt, which blocks rejoining under the same owner or business until it's paid. | Kemas Home closes owing RM54.30. Its RM38.02 pending payout all goes to the fine, leaving a RM16.28 debt. A year later it must pay RM16.28 to rejoin. |
| PAY-10 | Debt, and fines paid early under PAY-11, are paid by direct payment to Homa. | Kemas Home pays its RM16.28 by online banking. |
| PAY-11 | A closed provider can rejoin only once its final debt is known: when its pending payouts are released, or at once by paying its whole outstanding fine. If it pays early, its pending payouts are later paid in full, less the platform fee. | Kemas Home's RM45 payout is held by an open report. It can wait, or pay its RM54.30 fine now, rejoin, and receive RM38.02 when the report closes. |
| PAY-12 | When a customer's bank reverses a payment (a chargeback), Support decides who was at fault. If the provider was, the amount paid to it is recovered like a fine. If no one was, Homa bears the loss. | Aisyah's card payment is reversed after a job done as booked. Homa absorbs the RM200 and reviews her account (CUS-4). |

---

## 6. Cancellation and refund

### 6.1 General

| ID | Rule | Case |
| --- | --- | --- |
| CAN-1 | Cancellation fees and fines are calculated after discount. | The Base Cancellation Fee is RM45, not RM50. |
| CAN-2 | No fee or fine is more than the Total Reservation Fee. | For a RM25 job, a no-show fine of 25 + 30% × 25 = RM32.50 is capped at RM25. |
| CAN-3 | Percentages in this section are of the Total Reservation Fee, unless stated otherwise. | Base + 10% is 45 + 20 = RM65. |

### 6.2 Customer cancels after Reserved, before the Check-in Period

| ID | Rule | Case |
| --- | --- | --- |
| CCX-1 | **Grace Period:** a reservation Reserved at least 24 hours before its Check-in Period can be cancelled for free for 4 hours after it's Reserved, whatever the tier. | Reserved Monday 22:00, Aisyah cancels at 23:30 for free. Reserved with a Firm provider on Friday 8:00, she can still cancel for free at 11:00. |
| CCX-2 | Outside the Grace Period, the provider's cancellation tier sets the free cut-off. | Aisyah cancels on Wednesday, so Kemas Home's Standard tier applies. |
| CCX-3 | **Flexible:** free until 4 hours before the Check-in Period. | Free until Saturday 5:45. |
| CCX-4 | **Standard:** free until 12 hours before the Check-in Period. | Free until Friday 21:45. |
| CCX-5 | **Firm:** free until 24 hours before the Check-in Period. | Free until Friday 9:45. |
| CCX-6 | After the cut-off, the customer pays the provider the Base Cancellation Fee. | Aisyah cancels on Friday 22:00, pays RM45 and gets RM155 back. |

### 6.3 Provider cancels after Reserved, before the Check-in Period

| ID | Rule | Case |
| --- | --- | --- |
| PCX-1 | More than 24 hours before the Check-in Period: the provider is fined the Base Cancellation Fee, and the customer is refunded in full. | Kemas Home cancels on Wednesday: fined RM45, Aisyah gets RM200. |
| PCX-2 | Within 24 hours of the Check-in Period: the provider is fined Base + 10%, and the customer gets a full refund + 3%. | Kemas Home cancels on Friday 20:00: fined RM65, Aisyah gets RM206. |

### 6.4 No Roster

| ID | Rule | Case |
| --- | --- | --- |
| NRS-1 | A reservation without a complete roster when its Check-in Period starts is cancelled. | At 9:45 only Siti is rostered for 2 helpers. The booking is cancelled and Aisyah is told at once. |
| NRS-2 | The provider is fined Base + 12%. | Kemas Home is fined RM69. |
| NRS-3 | The customer gets a full refund + 4%. | Aisyah gets RM208. |

### 6.5 Last-minute cancellation (from the Check-in Period to the job end)

| ID | Rule | Case |
| --- | --- | --- |
| LMC-1 | **Customer cancels:** the customer pays the provider the Base Cancellation Fee + the hours worked after the first hour + the travel fee. | Aisyah cancels at 9:50: she pays 45 + 0 + 20 = RM65 and gets RM135 back. |
| LMC-2 | Hours worked after the first hour are charged in 15-minute steps, rounded up. | Aisyah stops the job at 11:20: 30 minutes are charged (RM22.50), so she pays RM87.50 and gets RM112.50 back. |
| LMC-3 | **Provider cancels:** the provider is fined Base + 20%, and the customer gets a full refund + 7%. | Kemas Home's van breaks down at 9:50: fined RM85, Aisyah gets RM214. |

### 6.6 Provider suspended

| ID | Rule | Case |
| --- | --- | --- |
| SUS-1 | A provider suspended for missed re-verification has its reservations before their Check-in Period cancelled as provider cancellations (PCX). | Kemas Home is suspended on Thursday: fined RM45, Aisyah gets RM200. |
| SUS-2 | A provider suspended for missed re-verification during a job finishes it, and the suspension starts when it ends. | The deadline passes at 11:00. The team finishes at 14:00, and Kemas Home is suspended from then. |
| SUS-3 | A provider suspended by a monitoring flag has its reservations before their Check-in Period cancelled with a full refund and no fine. A job in progress stops at once, and the customer is refunded the unworked time. | Flagged at 12:00, the team stops. The 2 worked hours are paid and Aisyah gets 2 × 45 = RM90 back. |
| SUS-4 | A provider suspended by Support has its reservations before their Check-in Period cancelled as provider cancellations (PCX). A job in progress stops as a Provider Last Minute Cancellation (LMC-3). | Support suspends Kemas Home on Thursday: fined RM45, Aisyah gets RM200. At 11:00 during the job, LMC-3 would apply. |
| SUS-5 | Suspending the owner suspends the provider. | Hana is flagged at 11:00 during her 1-helper job. Hana Cleaning is suspended and SUS-3 applies. |

---

## 7. No show

| ID | Rule | Case |
| --- | --- | --- |
| NSH-1 | **Provider No Show:** the team doesn't arrive, or leaves before the job end without cancelling in the app. | No one arrives by 10:00, or the team leaves at 12:00. Both are a Provider No Show. |
| NSH-2 | Support confirms a Provider No Show from the customer's report, made after the Check-in Period. The provider can answer with a photo taken at the address and its location at the time. | Aisyah reports at 10:05. Kemas Home has no arrival photo, so Support confirms. A report at 9:55 is too early. |
| NSH-3 | For a Provider No Show, the provider is fined Base + 30%, and the customer gets a full refund + 10%. | Kemas Home is fined RM105, and Aisyah gets RM220. |
| NSH-4 | **Customer No Show:** Support confirms it from the provider's report, made after the Check-in Period with a photo at the address and the reporter's location. | No one answers at 9:50. At 10:05 Siti reports with a photo of the door. Support checks it against the address and arrival note. |
| NSH-5 | For a Customer No Show, the customer pays the provider the Base Cancellation Fee + 30% of the Hours Fee after the first hour + the travel fee. | The 3 hours after the first are worth RM135. Aisyah pays 45 + 40.50 + 20 = RM105.50 and gets RM94.50 back. |
| NSH-6 | A Provider No Show reported in the Protection Window can be confirmed even after the job was marked Completed. The reservation becomes Cancelled, and its ratings are removed. | Kemas Home never came but marked the job Completed with an old photo. Aisyah reports on Sunday. Support confirms: NSH-3 applies and both ratings are removed. |

---

## 8. Reports

| ID | Rule | Case |
| --- | --- | --- |
| RPT-1 | Reports can be made during the Protection Window. | Aisyah can report from Saturday 9:45 to Sunday 14:00. |
| RPT-2 | Report categories: Provider No Show (customer), Customer No Show and Roster Member Left (provider), and Property Damage, Safety & Misconduct and Other (both). | Aisyah reports a cracked vase as Property Damage. |
| RPT-3 | A report has one category and a description. Media is optional. | Aisyah writes "Vase cracked during cleaning" and adds a photo. |
| RPT-4 | No-show reports are resolved under §7. | Support confirms Aisyah's Provider No Show, and NSH-3 applies. |
| RPT-5 | Property Damage, Safety & Misconduct and Other reports are resolved by Support case by case. | Support reviews the vase report, the photos and Kemas Home's reply. |
| RPT-6 | A payout waits until every report on the reservation is resolved. | Aisyah's report is still open at Sunday 14:00, so Kemas Home's payout waits. |
| RPT-7 | Homa doesn't pay for property damage. Support helps settle it, and points to the provider's insurance if it's Insurance Covered. | Support points Aisyah to Kemas Home's public liability insurance. |
| RPT-8 | From the Check-in Period to the job end, the provider can report **Roster Member Left**, with a reason: Illness, Emergency or Other. | Nurul feels unwell at 12:00. Kemas Home reports "Roster Member Left: Illness". |
| RPT-9 | When a member leaves, the rest of the team carries on and the customer is told, without the reason. The helper's worked time is paid, rounded up to the next 15-minute step, and the rest is refunded. A supervisor leaving gives no refund. | Nurul leaves at 12:00: Aisyah gets 2 × 25 × 0.9 = RM45 back. At 12:05 it would be 1h 45m, RM39.38. |
| RPT-10 | A Roster Member Left report has no fine but counts in reliability. It resolves itself when the Protection Window ends, unless the customer disputes it first. | Aisyah says Nurul left at 11:00, not 12:00, so Support reviews the report and the chat. |
| RPT-11 | When every rostered helper leaves, it's a Provider Last Minute Cancellation (LMC-3). | Siti and Nurul both leave at 12:00. Kemas Home cancels in the app, and LMC-3 applies. |

---

## 9. Ratings

| ID | Rule | Case |
| --- | --- | --- |
| RTG-1 | The customer rates the provider and the provider rates the customer. | Aisyah rates Kemas Home and Kemas Home rates Aisyah. |
| RTG-2 | The Rating Window opens when the job is Completed and closes 7 days after the Protection Window ends. | Completed Saturday 14:00, the window closes the following Sunday at 14:00. A job marked Completed after that gets no ratings. |
| RTG-3 | Ratings are double-blind: each is revealed when both are in, or when the window closes. | Aisyah rates on Saturday and Kemas Home on Monday; both show on Monday. |
| RTG-4 | A rating has 1–5 stars. A description and category stars are optional. | Aisyah gives 4 stars with no text. |
| RTG-5 | A customer's rating can include media. A provider's rating of a customer is text only. | Aisyah adds a photo of her kitchen. Kemas Home can't add photos of her home. |
| RTG-6 | A provider's ratings are seen by it and by customers. A customer's ratings are seen by them and by providers. | Other customers see Aisyah's 4 stars on Kemas Home's page. |
| RTG-7 | Customers rate providers on **Quality of work**, **Punctuality** and **Professionalism**. | Aisyah gives 5, 4 and 5. |
| RTG-8 | Providers rate customers on **Accurate request**, **Access and readiness** and **Respect**. | Kemas Home gives Access and readiness 3, because the guardhouse code was missing. |
| RTG-9 | Only Completed reservations get ratings. | A Customer No Show gets no ratings; it shows in reliability instead. |
| RTG-10 | Reliability shows counts over the last 12 months. Customers see a provider's cancellations, no-rosters, no-shows and members who left during a job. Providers see a customer's fee-paying cancellations and no-shows. | Kemas Home shows "Cancellations: 1, No-rosters: 0, No-shows: 0". Aisyah's free cancellation isn't counted; her RM45 one is. |
| RTG-11 | Support can remove a rating with abusive content or personal contact or address details. | Support removes a rating that includes a customer's phone number. |

---

## 10. Data protection and retention

| ID | Rule | Case |
| --- | --- | --- |
| DAT-1 | Each record is kept for its period below, then deleted or anonymised, whatever the account's state. A record in an open report, claim or Support case is kept until it closes. | Aisyah's chat is deleted 12 months after its Protection Window, though her account is active. |
| DAT-2 | Payment records are kept for 7 years from the end of the year of the transaction, with only the identity details they need. | Aisyah's November 2026 payment is kept until 31 December 2033. |
| DAT-3 | Property Damage and Safety & Misconduct reports, with their evidence, are kept for 6 years after they're resolved. Other reports are kept for 12 months after. | The vase report, resolved in December 2026, is kept until December 2032. A no-show report from then is kept until December 2027. |
| DAT-4 | Records behind reliability counts are kept for 12 months from the event. | Aisyah's fee-paying cancellation stops counting, and is deleted, 12 months later. |
| DAT-5 | Chats, completion photos and arrival proof not in a report are kept for 12 months after the Protection Window. An Enquiry is kept for 12 months after its last message. | The after-photo is deleted 12 months after Sunday 14:00. A March Enquiry is deleted the next March. |
| DAT-6 | A debt record is kept until the debt is paid, and at least as long as payment records. | Kemas Home's RM16.28 debt stays on record until it's paid. |
| DAT-7 | A suspended customer's identity-provider account and identity number stay blocked until the suspension ends, even after deletion. | Daniel deletes his data while suspended. His Google account and MyKad stay blocked until the 30 days end. |
| DAT-8 | A reservation and its settlement are kept as long as its payment records. Its address and arrival note are kept for 12 months after the Protection Window, or until a deletion request, unless a kept report needs them. | Aisyah's booking is kept until 31 December 2033; its address goes 12 months after Sunday 14:00. |
| DAT-9 | A member's or owner's verification results and identity number are kept until 12 months after they're removed or the provider closes, or longer while a debt or a closed provider's hidden ratings need them. A kept report keeps the names and verified status of the members it names. | Rina is removed on 31 January 2027, and her results and i-Kad number are deleted on 31 January 2028. |
| DAT-10 | Ratings are kept while the rated account exists. A deleted customer's ratings are deleted. A closed provider's ratings are hidden for 7 years and shown again if it rejoins under the same owner or business. | Kemas Home closes in 2027 and rejoins in 2028. Its old 3.1-star ratings show again. |
| DAT-11 | Each person agrees to Homa's privacy notice themselves before their data is used, in the languages the country policy sets. Each agreement is recorded and kept as long as their data. | Nurul agrees herself during her verification on 3 December; Kemas Home can't agree for her. |
| DAT-12 | Anyone can ask Support to see, correct or get a copy of their data. | Siti gets a copy of her profile, results and roster history. Aisyah has her name's spelling corrected. |
| DAT-13 | Payout account details are kept while in use and until the last payout into the account is released, then only as part of payment records. | Kemas Home's old bank details stay on its earlier payouts until their payment records are deleted. |
| DAT-14 | Records of Support viewing an identity number are kept until 12 months after the number is deleted. | A view of Rina's i-Kad number is kept until 31 January 2029. |
| DAT-15 | Insurance policy documents are kept while the policy is valid and for 6 years after it ends. | A policy ending on 31 March 2027 is kept until 31 March 2033. |

---

## 11. External services

Homa relies on outside services for some facts. Rules elsewhere describe only the fact, such as a confirmed payment or a passed verification, and stay the same whichever service provides it. Services are named by role, not vendor.

| Service | What it confirms | Rules that rely on it |
| --- | --- | --- |
| **Payment provider** | Confirmed payments; refunds, compensation and payouts sent; that a payout account belongs to the provider (matched by the holder's identity or registration number, or by name where no number is available) | P1, PRV-2, PRV-7, PRV-10, RES-7, RES-11, RES-12, PAY-1 to PAY-12, DAT-2; and every refund or payment from REQ-2 to REQ-4, CUS-5, ROS-8, ROS-10, RPT-9, CCX-1 to CCX-6, PCX-1, PCX-2, NRS-3, LMC-1, LMC-3, NSH-3, NSH-5 and SUS-1 to SUS-4 |
| **Verification service** | The identity, age and status of customers, owners and members; work permits and their expiry; owners' and members' profile photos; a business's registration and the owner's link to it; re-verification and monitoring flags | CUS-3, CUS-10, PRV-1, PRV-7, PRV-9, HLP-1 to HLP-3, HLP-7, HLP-8, IDN-1, IDN-2, IDN-4, MON-1 to MON-6, ROS-4, ROS-9, ROS-10, SUS-1 to SUS-3, SUS-5, DAT-9 |
| **Identity provider** | That a customer's sign-in belongs to them | CUS-7, CUS-9, DAT-7 |
| **Calendar service** | The provider's external calendar events | OFR-8, RES-3 |

| ID | Rule | Case |
| --- | --- | --- |
| EXT-1 | Homa decides every outcome. External services only confirm facts. | The verification service flags Farid; Homa suspends him, and only Support can reactivate him. |
| EXT-2 | A payment is confirmed only when the payment provider confirms it. | The payment provider is down at 20:00, so nothing is Requested. Aisyah pays at 20:15. |
| EXT-3 | Refunds and payouts are sent when the rules say, and arrival depends on the banks. A failed transfer is retried once; if it fails again, Support takes over. | Aisyah's RM155 is sent on Friday and arrives on Monday. A payout to Kemas Home bounces, fails again on the retry, and Support contacts Kemas Home. |
| EXT-4 | Identity documents and biometric data stay with the verification service. | Homa stores "passed, 20 November" for Siti, not a copy of her card. |
| EXT-5 | When the verification service is down, re-verification deadlines move by the length of the outage. | The service is down 17–19 November, so Nurul's 19 November deadline moves to 21 November. |
| EXT-6 | When the identity provider is down, customers can't sign in, but reservations go ahead. | Google sign-in is down on Saturday morning. The team still arrives at 9:52. |
| EXT-7 | When calendar sync fails, Homa uses the last synced calendar and tells the provider. A clash it causes is the provider's to resolve. | A new private appointment doesn't sync, Aisyah requests that time, and Kemas Home declines. |

---

## 12. Summary of money outcomes

Amounts use the standard reservation (Total RM200, Base RM45).

| Case | Customer pays | Provider fine | Customer receives | Example |
| --- | --- | --- | --- | --- |
| Another payment for the same time confirmed first | — | — | Full refund | Gets RM200 |
| Customer withdraws a request | — | — | Full refund | Gets RM200 |
| Provider declines, or request expires | — | — | Full refund | Gets RM200 |
| Customer cancels in the Grace Period or before the cut-off | — | — | Full refund | Gets RM200 |
| Customer cancels after the cut-off | Base | — | Refund minus fee | Pays RM45, gets RM155 |
| Provider cancels, more than 24h ahead | — | Base | Full refund | Fine RM45; gets RM200 |
| Provider cancels, within 24h | — | Base + 10% | Full refund + 3% | Fine RM65; gets RM206 |
| No roster | — | Base + 12% | Full refund + 4% | Fine RM69; gets RM208 |
| Smaller roster accepted | New price, discount kept | — | Difference | 1 helper: pays RM110, gets RM90 |
| Member leaves during the job | — | — | Their remaining hours | Nurul at 12:00: gets RM45 |
| Customer cancels last minute | Base + hours after the first (15-min steps) + travel | — | Refund minus fee | At 11:20: pays RM87.50, gets RM112.50 |
| Provider cancels last minute | — | Base + 20% | Full refund + 7% | Fine RM85; gets RM214 |
| Provider no show | — | Base + 30% | Full refund + 10% | Fine RM105; gets RM220 |
| Customer no show | Base + 30% of the Hours Fee after the first hour + travel | — | Refund minus fee | Pays RM105.50, gets RM94.50 |
| Customer suspended | — | — | Full refund | Gets RM200 |
| Provider suspended: missed re-verification, or by Support | — | As provider cancellation | As provider cancellation | Fine RM45; gets RM200 |
| Provider suspended by Support during a job | — | Base + 20% | Full refund + 7% | Fine RM85; gets RM214 |
| Provider suspended: monitoring flag | — | — | Full refund | Gets RM200 |
| Provider suspended: monitoring flag during a job | Worked time (15-min steps) | — | Unworked time | Stops at 12:00: gets RM90 |
| Chargeback, provider at fault | — | Amount recovered like a fine | — | — |

*Percentages are of the Total Reservation Fee. No fee or fine is more than the Total Reservation Fee.*

---

## 13. Homa-set values

Every value Homa sets, in one place. Changing a value here changes it in every rule listed.

**Reservation values** are fixed when a reservation is Requested (P3): minimum rate, maximum discount, minimum notice, duration, partial-time charging, Check-in Period, acceptance deadline, Grace Period, cancellation tiers, customer fees, provider fines, compensation, the fee and fine cap, Protection Window, completion reminders, platform fee, fine deduction cap, rounding and Rating Window.

**Account values** apply at once: minimum age, work permit buffer, re-verification cycles, suspension lengths, default hours and helpers, buffer between jobs, ratings, the reliability period and retention periods.

| Value | Amount | Rules |
| --- | --- | --- |
| Minimum age | 18 | CUS-10, PRV-1, HLP-1 |
| Work permit buffer | 90 days before expiry | HLP-3, ROS-4 |
| Re-verification cycle | Every 6 months, reminders from 60 days before | MON-1, MON-5 |
| Re-verification when flagged | Within 14 days | MON-2 |
| Suspension lengths | 7 days, 30 days, or until lifted | CUS-4, CUS-6, PRV-12 |
| Minimum rate | RM20 per helper hour, after discount | OFR-11 |
| Maximum discount | 99% | OFR-4 |
| Default hours | 1 to 8 hours | OFR-5 |
| Default helpers | 1 to the number of active helpers | OFR-5 |
| Minimum notice | Default 2 hours; lowest 1 hour (Instant), 2 hours (Request) | OFR-6, RES-4 |
| Buffer between jobs | Default 30 minutes | OFR-10, RES-9 |
| Duration | At least 1 hour, in 15-minute steps | RES-5 |
| Partial-time charging | 15-minute steps, rounded up | LMC-2, RPT-9, SUS-3 |
| Check-in Period | 15 minutes before the job start | Definitions, JOB-1 |
| Acceptance deadline | 24 hours after the request, or 1 hour before the Check-in Period, whichever is first | ACC-3 |
| Grace Period | 4 hours after Reserved, if Reserved at least 24 hours before the Check-in Period | CCX-1 |
| Cancellation tiers | Flexible 4 hours, Standard 12 hours, Firm 24 hours before the Check-in Period | CCX-3 to CCX-5 |
| Customer cancellation fee | Base Cancellation Fee | CCX-6, LMC-1 |
| Provider fines | Base; + 10% within 24 hours; + 12% no roster; + 20% last minute; + 30% no show | PCX-1, PCX-2, NRS-2, LMC-3, NSH-3 |
| Customer compensation | 3%, 4%, 7%, 10% | PCX-2, NRS-3, LMC-3, NSH-3 |
| Customer no-show fee | Base + 30% of the Hours Fee after the first hour + travel | NSH-5 |
| Fee and fine cap | The Total Reservation Fee | CAN-2 |
| Protection Window | Until 24 hours after the job end | Definitions, RPT-1, PAY-2, PAY-6 |
| Completion reminders | At the job end and 2 hours later | JOB-5 |
| Platform fee | 15.5% of every payout | PAY-3 |
| Fine deduction cap | 20% of each payout | PAY-5 |
| Rounding | Once per line, to the nearest sen, half up | PAY-8 |
| Rating Window | Closes 7 days after the Protection Window | RTG-2 |
| Ratings | 1–5 stars; 3 categories per side | RTG-4, RTG-7, RTG-8 |
| Reliability counts | Last 12 months | RTG-10 |
| Retention: payment records and debt | 7 years from the end of the year | DAT-2, DAT-6 |
| Retention: damage and safety reports | 6 years after resolved | DAT-3 |
| Retention: other reports, reliability records, chats, photos | 12 months | DAT-3 to DAT-5 |
| Retention: reservation address and arrival note | 12 months after the Protection Window | DAT-8 |
| Retention: verification results and identity number | 12 months after removal or closure | DAT-9 |
| Retention: closed provider's ratings | Hidden for 7 years | DAT-10 |
| Retention: identity number views | 12 months after the number is deleted | DAT-14 |
| Retention: insurance documents | 6 years after the policy ends | DAT-15 |

---

## 14. Country policy: Malaysia

Rules elsewhere refer to "the country policy". This table sets what it means in Malaysia. A new country needs only its own table.

| Item | Malaysia | Rules |
| --- | --- | --- |
| Local | Citizens (MyKad) and permanent residents (MyPR) | HLP-2, IDN-1 |
| Who can be an owner | Local only | PRV-1 |
| Who can be a customer | Local only | CUS-10 |
| Foreign | Foreign workers with a work permit, verified with their i-Kad. The i-Kad expires on or before the work permit, so its date is used as the permit's expiry. | PRV-8, HLP-3, HLP-4, ROS-4, IDN-1 |
| Identity documents | MyKad, MyPR, i-Kad | IDN-1, IDN-2 |
| Business registration | Companies Commission of Malaysia (SSM) | PRV-7 |
| Privacy notice languages | Bahasa Malaysia and English | DAT-11 |
| Personal data law | Personal Data Protection Act 2010, as amended in 2024 | §10 |
| Payment record keeping | 7 years | DAT-2 |
| Currency | Ringgit (RM) | All amounts |

---

## 15. Open items

None at the moment.
