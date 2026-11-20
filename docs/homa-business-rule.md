# Homa Business Rule

> Ingested from Notion on 2026-11-20: https://app.notion.com/p/41e8436ade4081e3b127ca594a99f2d9
> The Notion page is the source of truth; refresh this copy when it changes.

Homa is a marketplace that helps homeowners get the best homecare service providers.

Each rule has an ID so it can be referenced in specs, tests and support decisions. "Must" is mandatory, "can" is allowed, and "is" states a fact or value. Every rule has a **case**: a short story showing the rule in action, using the example setup below.

---

## 0. Example setup

All cases use these people and numbers unless the case says otherwise. Names and businesses are fictional.

|  |  |
| --- | --- |
| **Customer** | Aisyah, a working mother in Salak Tinggi. |
| **Provider** | Kemas Home Sdn Bhd, covering Salak Tinggi, Cyberjaya and Putrajaya. |
| **Team** | Helpers Siti and Nurul (citizens), helper Rina (foreign), supervisor Farid (citizen). |
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
| **Provider** | A verified, registered homecare business that offers services on Homa. |
| **Helper** | A verified team member who performs the jobs. Billed per hour. |
| **Supervisor** | An optional verified team member who oversees helpers. Not billed. |
| **Support** | Homa's support team, which verifies, confirms and resolves reports. |
| **Active** | A provider, helper or supervisor that is verified, not suspended by Homa, and (for helpers and supervisors) not deactivated by the provider. |
| **Requested** | The customer has paid and the payment is held. |
| **Reserved** | The reservation is confirmed, instantly or by provider acceptance. |
| **Reservation statuses** | Requested → Reserved → Completed. A reservation can also end as Declined, Expired or Cancelled (any cancellation, no roster, no show or suspension). A Completed reservation becomes Cancelled if a Provider No Show is later confirmed (NSH-6). |
| **Checkout hold** | A short hold on a provider's time while the customer pays. It is not a reservation status. |
| **Enquiry** | A chat a customer starts with a provider before making a reservation. |
| **Job time** | The booked period from job start time to job end time. Billing counts from the job start time. |
| **Check-in Period** | The 15 minutes before the job start time, in which the rostered team arrives at the customer's address. It is an agreed arrival time; there is no check-in step in the app. |
| **Notice** | The time between when the customer makes the reservation and the start of the Check-in Period. |
| **Protection Window** | From the start of the Check-in Period until 24 hours after the job end time. |
| **Hours Fee** | Per-helper hourly rate × hours × helpers, after discount. |
| **Total Reservation Fee** | Hours Fee + travel fee. |
| **Base Cancellation Fee** | Per-helper hourly rate × helpers × 1 hour × (1 − discount rate, if a discount applies). |
| **Fee** | An amount one party pays to the other. |
| **Fine** | A penalty on the provider, deducted by Homa from future payouts. |

---

## 2. Customer

| ID | Rule | Case |
| --- | --- | --- |
| CUS-1 | A customer must register with a verified phone number, a user name, a home address with city, and an arrival note. | Aisyah verifies her phone by code, enters "Aisyah", her Salak Tinggi address, and the arrival note "Guardhouse: say unit B-12-3". Her account is created. |
| CUS-2 | A customer must agree to the Terms of Service to register. | Aisyah fills in every field but doesn't tick the Terms of Service. She can't finish registering until she does. |
| CUS-3 | Only registered customers can make reservations. | Aisyah's husband browses Kemas Home without an account. When he taps Reserve, he's asked to register first. |
| CUS-4 | Support can suspend a customer, for example after a confirmed Safety & Misconduct report or repeated confirmed no-shows. | Kemas Home reports that a customer, Daniel, shouted at and threatened its helpers. Support confirms the report and suspends Daniel. |
| CUS-5 | A suspended customer can't make reservations or start enquiries. Their Requested and Reserved reservations whose Check-in Period hasn't started are cancelled with a full refund. No fees or fines apply. A reservation whose Check-in Period has started continues, and the suspension takes effect after the job ends. | Daniel had a Requested reservation with another provider for next week. It's cancelled and he gets a full refund. A job of his already running when he's suspended finishes as booked. |
| CUS-6 | Reactivating a suspended customer requires Support confirmation. | A month later Support reviews Daniel's appeal and reactivates his account. |

---

## 3. Provider

### 3.1 Onboarding and verification

| ID | Rule | Case |
| --- | --- | --- |
| PRV-1 | A provider must be a verified, registered and legal business entity in the country. | Kemas Home submits its company registration. Once it's verified, Kemas Home can continue onboarding. A sole trader without registration can't join. |
| PRV-2 | A provider must have at least one active helper. | Kemas Home finishes its business verification but hasn't added a helper. It can't go live until Siti passes verification. |
| PRV-3 | Supervisors are optional. | Kemas Home goes live with Siti, Nurul and Rina only. Farid is added as a supervisor a month later. |
| PRV-4 | A provider is tagged **Insurance Covered** when Support confirms a valid Public Liability Insurance policy. The tag is removed when the policy expires. | Kemas Home uploads a policy valid until 31 March. Support confirms it, and the tag shows on Kemas Home's page. On 1 April the tag disappears until a renewed policy is confirmed. |
| PRV-5 | A provider can't close its account while it has Requested or Reserved reservations. It must decline or cancel them first, and the normal cancellation rules and fines apply. | Kemas Home wants to leave Homa on Wednesday but has Aisyah's Saturday reservation. It cancels first and is fined RM45 (PCX-1), then closes its account. |
| PRV-6 | A provider with no active helpers can't be booked, and is notified. Its existing reservations follow ROS-5 and ROS-7. | Siti, Nurul and Rina are all deactivated for a holiday week. Kemas Home disappears from search and is told why. Its Saturday reservation still needs an active roster before 9:45, or the No Roster Policy applies. |

### 3.2 Helper and supervisor verification

| ID | Rule | Case |
| --- | --- | --- |
| HLP-1 | Every helper and supervisor must be aged 18 or above. | Kemas Home tries to add a 17-year-old trainee. Verification fails on age, so the trainee can't be added. A new helper who turned 18 last week passes. |
| HLP-2 | A citizen helper or supervisor must be verified as a citizen of the country. | Siti verifies with her identity card and is added as a citizen helper. |
| HLP-3 | A foreign helper or supervisor must be verified as legally allowed to work in the country, with at least 90 days before their work permit expires. | Rina's permit expires in 200 days, so she passes. Another candidate's permit expires in 60 days, so they can't be added until their permit is renewed. |
| HLP-4 | The provider must confirm that each foreign helper or supervisor legally works under the provider's registered business. | When adding Rina, Kemas Home ticks the confirmation that Rina is lawfully employed by Kemas Home. Without the tick, Rina can't be added. |
| HLP-5 | A provider can deactivate and reactivate its helpers and supervisors. | Siti goes on two weeks' leave. Kemas Home deactivates her, so she can't be booked or rostered, and reactivates her when she's back. |
| HLP-6 | A deactivated member can't be rostered. If a rostered member is deactivated, ROS-5 applies. | Kemas Home deactivates Nurul on Thursday while she's rostered for Saturday. It must change Saturday's roster before 9:45. |
| HLP-7 | Reactivation needs a valid verification. If re-verification lapsed while the member was deactivated, they must re-verify first. | Farid was deactivated from May to December, and his re-verification fell due in October. He must re-verify before Kemas Home can reactivate him. |
| HLP-8 | A provider must deactivate a member who no longer works for it. | Rina resigns at the end of the month. Kemas Home deactivates her the same day, so the lawful-employment confirmation (HLP-4) stays true. |

### 3.3 Verification monitoring

| ID | Rule | Case |
| --- | --- | --- |
| MON-1 | Providers, helpers and supervisors must pass re-verification every 6 months to remain active. The next 6 months count from the date re-verification is passed. Reminders start 60 days before expiry. | Siti was last verified on 1 June, so she's due on 1 December. Reminders start on 2 October. She re-verifies on 20 November, stays active, and is next due on 20 May. |
| MON-2 | If ongoing monitoring requires re-verification, it must be passed within 14 days, with reminders, to remain active. | Monitoring flags Nurul for re-verification on 5 November. She's reminded and must pass by 19 November to stay active. |
| MON-3 | Failing MON-1 or MON-2 suspends the provider, helper or supervisor until re-verification is passed. | Nurul doesn't re-verify by 19 November and is suspended. She re-verifies on 22 November and becomes active again. |
| MON-4 | If ongoing monitoring flags a suspension, the provider, helper or supervisor is suspended immediately. Reactivation requires Support confirmation. | Monitoring flags Farid for suspension. He's suspended at once and can't be rostered. Support reviews the case and reactivates him a week later. |

### 3.4 Offerings and settings

| ID | Rule | Case |
| --- | --- | --- |
| OFR-1 | A provider chooses jobs, tools and cities covered from Homa presets. | Kemas Home picks "Kitchen cleaning", "Laundry folding" and "Bathroom cleaning", the tool "HEPA vacuum", and the cities Salak Tinggi, Cyberjaya and Putrajaya. |
| OFR-2 | A provider sets a per-helper hourly rate. Pricing is per helper per hour only. | Kemas Home sets RM25 per helper per hour. A 2-hour job with 2 helpers is priced 25 × 2 × 2 = RM100 before travel. |
| OFR-3 | A provider sets a travel fee: none, flat, or by city. | Kemas Home sets RM20 flat. Another provider sets RM10 for Salak Tinggi and RM25 for Putrajaya. |
| OFR-4 | A provider can set a discount rate that applies when a reservation meets a minimum number of hours and helpers. Default: no discount. | Kemas Home sets 10% for at least 3 hours and 2 helpers. Aisyah's 4-hour, 2-helper booking gets 10% off; a 2-hour, 2-helper booking doesn't. |
| OFR-5 | A provider sets minimum and maximum hours (default minimum 1 hour, default maximum 8 hours) and minimum and maximum helpers (default minimum 1, default maximum: number of active helpers). | Kemas Home sets 2 to 6 hours and leaves helpers on default. With 3 active helpers, customers can book 1 to 3 helpers. A provider that doesn't change hours can be booked for 1 to 8 hours. |
| OFR-6 | A provider sets a minimum notice. Default: 2 hours. The lowest allowed is 1 hour with the Instant policy and 2 hours with the Request policy, so a Request provider always has at least 1 hour to respond (ACC-3). If a provider switches to Request with a notice under 2 hours, the notice is raised to 2 hours automatically and the provider is told. | Kemas Home (Request) keeps the 2-hour default and can't go lower. An Instant provider sets 1 hour; when it later switches to Request, its notice becomes 2 hours and it's notified. A larger team sets 24 hours because it plans the day before. |
| OFR-7 | A provider can add proof-of-work photos from past jobs. | Kemas Home uploads before-and-after photos of a kitchen it cleaned, shown on its provider page. |
| OFR-8 | A provider manages availability (working days and hours) and can sync external calendars. | Kemas Home works Monday–Saturday, 8:00–18:00, and syncs its Google Calendar. A private appointment on Tuesday 14:00–16:00 makes that slot unavailable on Homa. |
| OFR-9 | A provider chooses exactly one Reservation Acceptance Policy (§4.3) and one Customer Cancellation Tier (§6.2). | Kemas Home chooses Request and Standard. These are shown on its page before a customer reserves. |
| OFR-10 | A provider sets a buffer between jobs for travel and rest. Default: 30 minutes. | Kemas Home keeps the 30-minute default. A provider covering a wider area sets 60 minutes. |

---

## 4. Reservation

### 4.1 Making a reservation

| ID | Rule | Case |
| --- | --- | --- |
| RES-1 | A customer can only reserve a provider that covers the city of the customer's address. | Aisyah lives in Salak Tinggi, so she can reserve Kemas Home. Her sister in Shah Alam can't, because Kemas Home doesn't cover Shah Alam. |
| RES-2 | A customer must select at least one of the provider's jobs, a date, a job start time, the number of hours and helpers, and their home address. | Aisyah selects "Kitchen cleaning" and "Laundry folding", Saturday 10:00, 4 hours, 2 helpers, and her saved address. She can't continue without choosing a job. |
| RES-3 | A customer can only reserve a time when the provider is available. The job time must fall within the provider's working hours; the Check-in Period and buffer may fall outside them. | Kemas Home has a job Saturday 8:00–10:00 elsewhere. With its 30-minute buffer (RES-9), the earliest start Aisyah can choose is 10:45, with the Check-in Period from 10:30. Kemas Home works 8:00–18:00, so a job can start at 8:00 (team arrives from 7:45) but can't run 17:00–19:00. |
| RES-4 | A reservation's Notice must be at least the provider's minimum notice. | It's Saturday 7:00, Kemas Home has no other jobs that morning, and its notice is 2 hours. The earliest Check-in Period can start at 9:00, so the earliest job start Aisyah can pick is 9:15. |
| RES-5 | Minimum duration is 1 hour. Duration increases in 15-minute steps. | With a provider whose minimum is 1 hour, Aisyah can book 1h, 1h 15m, 1h 30m and so on, but not 50 minutes or 1h 10m. With Kemas Home (minimum 2 hours, OFR-5) she starts at 2h. |
| RES-6 | Total Reservation Fee = (per-helper hourly rate × hours × helpers) × (1 − discount rate, if a discount applies) + travel fee. | Standard reservation: 25 × 4 × 2 × 0.9 + 20 = RM200. |
| RES-7 | When payment is secured, the reservation is Requested and the payment is held. | Aisyah pays RM200 on Monday 20:00. The reservation is Requested and Homa holds the RM200. |
| RES-8 | A reservation keeps the provider's terms from when it was Requested: rate, travel fee, discount, acceptance policy and cancellation tier. Later changes by the provider apply only to new reservations. | On Wednesday Kemas Home raises its rate to RM30 and switches to Firm. Aisyah's Saturday reservation stays at RM200 under Standard. |
| RES-9 | A provider can have only one reservation at a time. A Requested or Reserved reservation blocks the provider's calendar from the start of its Check-in Period until the job end time plus the provider's buffer (OFR-10), until it is Declined, Expired, Cancelled or Completed. | Aisyah's request for Saturday 10:00–14:00 blocks Kemas Home from 9:45 to 14:30, starting Monday 20:00. Another customer can't book Kemas Home at 11:00 on Saturday, even for 1 helper; the earliest start after Aisyah's job is 14:45. If Kemas Home declines Aisyah, or doesn't respond by Tuesday 20:00, the time opens again. |
| RES-10 | A customer can't have two Requested or Reserved reservations with overlapping job times. | Aisyah has requested Kemas Home for Saturday 10:00–14:00. She can't also request another provider for Saturday 12:00. She can book Sunday. |
| RES-11 | While a customer pays, the provider's time is held for up to 15 minutes (checkout hold). If payment isn't secured within 15 minutes, or fails, the hold is released. | Aisyah taps Pay at 19:58 and the slot is held until 20:13. Her online banking payment succeeds at 20:00, and the reservation is Requested. Had the payment failed, another customer could book the slot from then. |
| RES-12 | If a payment succeeds after the checkout hold has expired, the reservation is made only if the time is still free and every booking rule still passes. Otherwise the payment is refunded in full at once and the customer is told. | Aisyah's online banking confirms at 20:20, after the hold ended at 20:13. The slot is still free, so her reservation is Requested. Had another customer booked it at 20:15, Aisyah would get RM200 back straight away. |

### 4.2 While Requested

| ID | Rule | Case |
| --- | --- | --- |
| REQ-1 | A customer can edit a Requested reservation. The original terms are kept (RES-8) and the price is recalculated with them. A higher price needs a top-up payment before the edit applies (REQ-6); a lower price is refunded at once. The acceptance deadline is recalculated from the time of the edit (ACC-3), and the edited time must still meet the provider's minimum notice. | On Monday 21:00 Aisyah changes 4 hours to 3 hours: the price becomes 25 × 3 × 2 × 0.9 + 20 = RM155, RM45 is refunded at once, and Kemas Home now has until Tuesday 21:00 to respond. Had she changed to 5 hours (25 × 5 × 2 × 0.9 + 20 = RM245), she'd pay a RM45 top-up before the edit applied. In a different booking, Aisyah requests on Saturday 7:00 for a 10:00 job. At 7:30 she tries to move the start to 9:00, but the Check-in Period would start at 8:45, only 1h 15m away, so the edit is blocked. |
| REQ-2 | A customer can cancel a Requested reservation for free. | Aisyah cancels on Monday 21:00 before Kemas Home responds. She gets the full RM200 back. |
| REQ-3 | A provider can decline a Requested reservation. The customer gets a full refund. | Kemas Home declines because two helpers are on leave. Aisyah gets RM200 back. |
| REQ-4 | A Request that isn't accepted by the acceptance deadline expires. The customer gets a full refund. | Kemas Home doesn't respond by Tuesday 20:00. The request expires, Aisyah gets RM200 back, and Saturday 10:00–14:00 opens for other customers. |
| REQ-5 | A Reserved reservation can't be edited. To change it, the customer cancels (under §6.2) and makes a new reservation. | After Kemas Home accepts, Aisyah wants Sunday instead of Saturday. She cancels on Wednesday, free under Standard, and books Sunday as a new reservation. |
| REQ-6 | While a top-up is pending (REQ-1), for at most 15 minutes: the original time stays blocked; a changed time, if any, is also held; and the provider can't accept or decline. If the top-up isn't paid within 15 minutes, or fails, the edit is dropped, the reservation stays as it was, and the customer is told. | Aisyah changes 4 hours to 5 at 21:00 and owes a RM45 top-up. Until 21:15 Kemas Home sees the request as being edited and can't respond. Aisyah doesn't pay, so at 21:15 the request stays at 4 hours for RM200 and she's notified. |
| REQ-7 | An edit that raises the price can't be made in the last 15 minutes before the acceptance deadline. Edits that lower the price are always allowed. | Aisyah requests at 7:00 for a 10:00 job, so the deadline is 8:45. At 8:35 she tries to add an hour and is blocked; she can still reduce the hours. |
| REQ-8 | If a top-up succeeds after its 15 minutes, the edit applies only if the reservation is still Requested and every rule still passes, including REQ-7 at that moment. Otherwise the top-up is refunded in full at once. When the edit applies, the acceptance deadline is recalculated from that moment (ACC-3). | Aisyah's RM45 top-up confirms at 21:20 on Monday, after the edit was dropped at 21:15. The request is still Requested and 5 hours is still free, so the edit applies, and Kemas Home now has until Tuesday 21:20 to respond. Had Kemas Home accepted the original at 21:16, her RM45 would be refunded straight away. |
| REQ-9 | If the customer cancels while a top-up is pending, the cancellation is free (REQ-2), the pending edit is dropped, and any top-up that arrives later is refunded at once. | Aisyah cancels at 21:05 with a top-up pending. She gets RM200 back, and when her RM45 top-up confirms at 21:10 it's refunded too. |

### 4.3 Acceptance policies

| ID | Rule | Case |
| --- | --- | --- |
| ACC-1 | **Instant:** the reservation becomes Reserved immediately after it is Requested. It must be made before the Check-in Period starts. | An Instant provider: Aisyah pays and the reservation is Reserved at once. |
| ACC-2 | **Request:** the reservation becomes Reserved only when the provider accepts it before the acceptance deadline. | Aisyah pays on Monday 20:00. Kemas Home accepts on Monday 22:00, and the reservation becomes Reserved. |
| ACC-3 | The acceptance deadline is 24 hours after the request, or 1 hour before the Check-in Period starts, whichever comes first. A Request must be made at least 1 hour before the Check-in Period starts. | Aisyah requests on Monday 20:00 for Saturday, so Kemas Home must respond by Tuesday 20:00. A request made on Saturday 7:00 for 10:00 has a deadline of 8:45, because that comes first. A request at 8:00 for 10:00 is blocked, because Kemas Home's 2-hour notice (OFR-6) needs it by 7:45. |

### 4.4 Roster

| ID | Rule | Case |
| --- | --- | --- |
| ROS-1 | The provider must set the roster before the Check-in Period starts and can change it until then. The customer is notified of every change. Once the Check-in Period starts, the roster is locked. | Kemas Home rosters Siti and Nurul on Friday evening. On Saturday at 8:00 it swaps Nurul for Rina, and Aisyah is notified. At 9:45 the roster is locked. |
| ROS-2 | The roster must include the booked number of helpers. Supervisors are optional, extra to that number, and not billed. | Aisyah booked 2 helpers. Kemas Home rosters Siti and Nurul plus supervisor Farid. Aisyah still pays for 2 helpers. Rostering only Siti and Farid is incomplete, because Farid doesn't count as a helper. |
| ROS-3 | Only active helpers and supervisors can be rostered. | Nurul is suspended (MON-3), so Kemas Home can't select her. It rosters Siti and Rina instead. |
| ROS-4 | A foreign helper or supervisor cannot be rostered on a reservation whose job ends within 90 days of their work permit expiry. | Rina's permit expires on 20 March, so she can be rostered on jobs ending up to 20 December. Kemas Home can't roster her on a job on 2 January. |
| ROS-5 | If a rostered helper or supervisor is suspended or deactivated, the provider must replace them with an active member before the Check-in Period starts. | Nurul is rostered for Saturday and suspended on Friday. Kemas Home replaces her with Rina before Saturday 9:45. |
| ROS-6 | The roster is shown to the customer with each member's name and photo. | On Friday evening Aisyah sees the names and photos of Siti, Nurul and supervisor Farid. On Saturday a person she doesn't recognise from the roster arrives, so she reports Safety & Misconduct. |
| ROS-7 | If ROS-1 or ROS-5 isn't met, the No Roster Policy applies (§6.4), unless the customer accepts a smaller roster (ROS-8). | Nurul is suspended on Friday and Kemas Home doesn't replace her by Saturday 9:45. The No Roster Policy applies. |
| ROS-8 | If the provider can't roster the booked number of helpers, it can propose a smaller roster before the Check-in Period starts. If the customer accepts, the reservation continues at the new price, keeping the discount from booking, and the difference is refunded, with no fine. If the customer declines or doesn't respond before the Check-in Period starts, the No Roster Policy applies. | Nurul falls sick on Friday. At 18:00 Kemas Home proposes Siti alone. Aisyah accepts. The new price is 25 × 4 × 1 × 0.9 + 20 = RM110 (she keeps her 10% discount), so she gets RM90 back. Had she declined, the No Roster Policy would apply. |
| ROS-9 | If a rostered helper or supervisor is suspended after the Check-in Period starts for missed re-verification (MON-3), the suspension takes effect after the current job ends. | Nurul's re-verification deadline passes at 11:00 during Aisyah's job. She finishes the job and is suspended from 14:00. |
| ROS-10 | If a rostered helper or supervisor is suspended after the Check-in Period starts by a monitoring flag (MON-4), they must stop work immediately. A Roster Member Left report is created automatically (RPT-8 to RPT-11). | Monitoring flags Rina at 11:00. She stops, a Roster Member Left report is created, and Siti carries on. Aisyah is refunded Rina's remaining 3 hours: 3 × 25 × 0.9 = RM67.50. |

### 4.5 Chat

| ID | Rule | Case |
| --- | --- | --- |
| CHT-1 | A customer can start a chat with a provider as an Enquiry before reserving, or from a Requested or Reserved reservation. The provider can reply but can't start an Enquiry. | Before booking, Aisyah asks Kemas Home "Do you bring your own ladder?". Kemas Home replies. Kemas Home can't message customers who haven't contacted it. |
| CHT-2 | Within a reservation, both the customer and the provider can send messages from when it's Requested until its Protection Window ends, except as limited by CHT-5. | On Saturday at 9:40 Kemas Home messages Aisyah: "We're at the guardhouse." On Sunday morning Aisyah messages about a missed corner, before the Protection Window ends at 14:00. |
| CHT-3 | Every chat shows that payments and arrangements made outside Homa are not covered by Homa's protection. | Kemas Home offers a cash discount for next time. The chat reminder shows that a cash job wouldn't be covered by Homa. |
| CHT-4 | Support can read the chat of a reservation as evidence when resolving a report. | For Aisyah's Roster Member Left disagreement, Support reads the Saturday messages to see when Nurul left. |
| CHT-5 | After the Protection Window ends, a reservation's chat becomes read-only. It becomes read-only straight away if the reservation is Declined, Expires, or is cancelled before the Check-in Period starts. Anything new goes through a new Enquiry. | On Monday Aisyah can read Saturday's chat but can't reply in it. To ask about next week, she starts a new Enquiry with Kemas Home. Had she cancelled on Wednesday, the chat would have become read-only on Wednesday. If she cancels at 9:50 on Saturday, it stays open until Sunday 14:00. |

### 4.6 Arrival and job time

| ID | Rule | Case |
| --- | --- | --- |
| JOB-1 | The rostered team arrives at the customer's address during the Check-in Period, so the job can start at the job start time. There is no check-in step; arrival problems are handled through reports (§7). | The team arrives at Aisyah's home at 9:52 and starts at 10:00. Had they arrived at 10:05, Aisyah could choose to let them start, or report a Provider No Show. |
| JOB-2 | The team works the full booked hours. Billing is for the booked hours, even if the team finishes early. | The team finishes the jobs at 13:30. Aisyah still pays for 4 hours. |
| JOB-3 | The provider marks the job Completed with at least one after-photo, only after the job end time. No customer confirmation is needed. | At 13:30 the Completed button isn't available yet. At 14:00 Kemas Home uploads a photo of the clean kitchen and marks the job Completed. |
| JOB-4 | The customer is notified when the job is marked Completed. | Aisyah gets a notification at 14:01 with the after-photo. |
| JOB-5 | If the job isn't marked Completed, the provider is reminded at the job end time and 2 hours later. The payout is held until the job is marked Completed. No fine applies. | Kemas Home forgets. It's reminded at 14:00 and 16:00, and marks the job Completed on Monday. The payout is released after that, with no fine. |
| JOB-6 | Completion photos are kept as evidence for reports. | Aisyah reports that the bathroom wasn't cleaned. Support checks the after-photos while reviewing the report. |

---

## 5. Payment

| ID | Rule | Case |
| --- | --- | --- |
| PAY-1 | The customer's payment is held from the moment the reservation is Requested. | Aisyah's RM200 is held from Monday 20:00. Kemas Home can't receive it yet. |
| PAY-2 | The provider's payout is released when the job is marked Completed, the Protection Window has ended, and any report is resolved. | The job is marked Completed on Saturday, no report is filed, and the Protection Window ends Sunday 14:00. The payout is released then. |
| PAY-3 | The platform fee is 15.5% of the provider's released payout for a reservation. It does not apply to cancellation or no-show fees paid to the provider. | Payout RM200: platform fee RM31, Kemas Home receives RM169. If Aisyah had paid a RM45 cancellation fee instead, Kemas Home would receive the full RM45. |
| PAY-4 | Refunds are returned to the customer from the held payment. Compensation (the extra % on top of a full refund) is paid by Homa and recovered through the provider's fine. The rest of the fine is kept by Homa. | Kemas Home doesn't show up. Aisyah gets RM200 back from the held payment, plus RM20 compensation from Homa. Kemas Home's RM105 fine repays the RM20, and Homa keeps RM85. |
| PAY-5 | Provider fines are not charged directly. They are deducted from the provider's next payouts after the platform fee, at most 20% of each payout, carried over until cleared. This applies to every payout, including cancellation and no-show fee payouts. | Kemas Home owes a RM105 fine. Its next job payout is RM300, or RM253.50 after the 15.5% platform fee, so RM50.70 is deducted. The remaining RM54.30 comes off the following payouts. |
| PAY-6 | Customer cancellation, last-minute and no-show fees are released to the provider when the Protection Window ends and any report is resolved, the same as a completed job's payout. The Protection Window is based on the booked job time, even if the reservation was cancelled. | Aisyah cancels on Friday 22:00 and pays RM45. Kemas Home receives the RM45 on Sunday at 14:00, when the Protection Window for the booked Saturday job ends, provided no report is open. |
| PAY-7 | Refunds are paid to the customer as soon as the outcome is decided: at cancellation, decline, expiry, or when Support resolves a report. | Aisyah cancels on Friday 22:00. Her RM155 refund is sent straight away; Kemas Home's RM45 fee waits until the Protection Window ends (PAY-6). |
| PAY-8 | Amounts are calculated exactly, and each money line is rounded once to the nearest sen (half up). A refund is the amount paid minus the rounded fee, so the lines always add up. | Nurul leaves at 12:05 (RPT-9): the refund is 1.75 × 22.50 = RM39.375, rounded once to RM39.38. The reservation total becomes RM200 − RM39.38 = RM160.62, before the platform fee. |
| PAY-9 | When a provider closes its account, payouts still pending are released when due, and any fine not yet cleared is deducted from them in full (the 20% cap doesn't apply). Whatever is still owed becomes a debt. A provider with debt can't rejoin Homa under the same business registration until the debt is paid. During a suspension, fines are not debt; they stay as a balance and are deducted from payouts after reactivation (PAY-5). | Kemas Home closes its account owing a RM54.30 fine, with a RM45 cancellation-fee payout still pending. When that payout is released, all RM45 goes to the fine, and the remaining RM9.30 becomes a debt. A year later it tries to rejoin with the same company registration and must pay the RM9.30 first. Had it only been suspended for missed re-verification, it would reactivate on re-verifying and the RM54.30 would come off its next payouts. |
| PAY-10 | Debt is paid by a direct payment to Homa. Direct payment is used only to clear debt, or an outstanding fine under PAY-11. | To rejoin, Kemas Home pays the RM9.30 by online banking. |
| PAY-11 | A closed provider can't rejoin while any of its payouts is still pending, because its final debt isn't known yet. It can rejoin straight away by paying its whole outstanding fine directly; its pending payouts are then released in full when due. | Kemas Home closed owing a RM54.30 fine, and its RM45 payout is held by an open report. It can't rejoin until the report is resolved, or it can pay the RM54.30 now and rejoin; the RM45 is then released to it in full when the report closes. |

---

## 6. Cancellation and refund

### 6.1 General

| ID | Rule | Case |
| --- | --- | --- |
| CAN-1 | All cancellation fees and fines are calculated after discount. | The standard reservation has a 10% discount, so the Base Cancellation Fee is RM45, not RM50. |
| CAN-2 | No customer fee and no provider fine can exceed 100% of the Total Reservation Fee. | A 1-hour, 1-helper job with no travel fee costs RM25. A provider no-show would come to 25 + 30% × 25 = RM32.50, so the fine is capped at RM25. |
| CAN-3 | All percentages in this section are of the Total Reservation Fee, unless stated otherwise. | "Base + 10%" on the standard reservation is 45 + 10% × 200 = RM65. |

### 6.2 Customer cancels after Reserved, before the Check-in Period

| ID | Rule | Case |
| --- | --- | --- |
| CCX-1 | **Grace Period:** the customer can cancel for free within 4 hours after the reservation is Reserved, only if it was Reserved at least 24 hours before the Check-in Period starts. The grace applies even if the tier's free cut-off has already passed. | Reserved Monday 22:00 for Saturday: Aisyah cancels at 23:30, within the grace, and gets RM200 back. Had it been Reserved Friday 20:00 (under 24 hours before the Check-in Period), there would be no grace. With a Firm provider, a reservation made Friday 8:00 has grace until 12:00, so cancelling at 11:00 is free even though Firm's cut-off (Friday 9:45) has passed. |
| CCX-2 | Outside the Grace Period, the provider's Customer Cancellation Tier applies. | Aisyah cancels on Wednesday, after the grace ended. Kemas Home's Standard tier decides whether she pays. |
| CCX-3 | **Flexible:** free until 4 hours before the Check-in Period starts. | With a Flexible provider, Aisyah can cancel for free until Saturday 5:45. |
| CCX-4 | **Standard:** free until 12 hours before the Check-in Period starts. | With Kemas Home (Standard), Aisyah can cancel for free until Friday 21:45. |
| CCX-5 | **Firm:** free until 24 hours before the Check-in Period starts. | With a Firm provider, Aisyah can cancel for free until Friday 9:45. |
| CCX-6 | After the tier's free cut-off, the customer pays the Base Cancellation Fee to the provider. | Aisyah cancels Friday 22:00, after Standard's 21:45 cut-off. She pays RM45 to Kemas Home and gets RM155 back. |

### 6.3 Provider cancels after Reserved, before the Check-in Period

| ID | Rule | Case |
| --- | --- | --- |
| PCX-1 | Check-in Period starts more than 24 hours away: the provider is fined the Base Cancellation Fee. The customer gets a full refund. | Kemas Home cancels on Wednesday. It's fined RM45, and Aisyah gets RM200 back. |
| PCX-2 | Check-in Period starts within 24 hours: the provider is fined the Base Cancellation Fee + 10%. The customer gets a full refund + 3%. | Kemas Home cancels Friday 20:00. It's fined 45 + 20 = RM65. Aisyah gets RM200 + RM6. |

### 6.4 No Roster

| ID | Rule | Case |
| --- | --- | --- |
| NRS-1 | If the roster isn't set or completed before the Check-in Period starts (ROS-7), the reservation is cancelled. | At Saturday 9:45 only Siti is rostered for a 2-helper booking. The reservation is cancelled automatically, and Aisyah is told straight away. |
| NRS-2 | The provider is fined the Base Cancellation Fee + 12%. | Kemas Home is fined 45 + 24 = RM69. |
| NRS-3 | The customer gets a full refund + 4%. | Aisyah gets RM200 + RM8. |

### 6.5 Last-minute cancellation (from Check-in Period start until job end)

| ID | Rule | Case |
| --- | --- | --- |
| LMC-1 | **Customer cancels:** the customer pays the provider the Base Cancellation Fee + 100% of the hours worked after the first hour + travel fee. | Aisyah cancels at 9:50 when the team is at the door. She pays 45 + 0 + 20 = RM65 and gets RM135 back. |
| LMC-2 | Hours worked after the first hour are charged in 15-minute steps, rounded up to the next started step. | Aisyah stops the job at 11:20. After the first hour (10:00–11:00), 20 minutes were worked, rounded up to 30 minutes = RM22.50. She pays 45 + 22.50 + 20 = RM87.50 and gets RM112.50 back. |
| LMC-3 | **Provider cancels in the app:** the provider is fined the Base Cancellation Fee + 20%. The customer gets a full refund + 7%. | At 9:50 Kemas Home cancels in the app because its van broke down. It's fined 45 + 40 = RM85. Aisyah gets RM200 + RM14. |

### 6.6 Provider suspended

| ID | Rule | Case |
| --- | --- | --- |
| SUS-1 | If a provider is suspended, its Requested and Reserved reservations whose Check-in Period hasn't started are cancelled. If the suspension is for missed re-verification (MON-3), each cancellation is treated as a provider cancellation (PCX-1 or PCX-2, by timing). If it's by a monitoring flag (MON-4), the customer gets a full refund and no fine applies. | Kemas Home misses its re-verification and is suspended on Thursday. Aisyah's Saturday reservation is cancelled as a provider cancellation more than 24 hours ahead: Kemas Home is fined RM45 and Aisyah gets RM200 back. Had it been suspended by a monitoring flag, Aisyah would get RM200 back with no fine. |
| SUS-2 | If a provider is suspended after the Check-in Period starts for missed re-verification (MON-3), the current job continues and the suspension takes effect after the job ends. | Kemas Home's re-verification deadline passes at 11:00 during Aisyah's job. The team finishes at 14:00, and Kemas Home is suspended from then. |
| SUS-3 | If a provider is suspended after the Check-in Period starts by a monitoring flag (MON-4), the job stops immediately. The customer is refunded the unworked time: worked time is rounded up to the next started 15-minute step, and the rest is refunded. No fine applies. | Monitoring flags Kemas Home at 12:00. The team stops. The 2 worked hours are paid and Aisyah is refunded the remaining 2 hours: 2 × 45 = RM90. |

---

## 7. No show

| ID | Rule | Case |
| --- | --- | --- |
| NSH-1 | **Provider No Show:** the provider doesn't arrive, or the team leaves before the job end time without cancelling in the app. | Case A: no one arrives by 10:00. Case B: the team leaves at 12:00 without cancelling. Aisyah can report both as Provider No Show. |
| NSH-2 | A Provider No Show is confirmed by Support from a customer report made after the Check-in Period ends. The provider can respond with evidence that it arrived (a photo taken at the address and its location at that time). | At 10:05 Aisyah reports Provider No Show. Kemas Home has no photo or location showing it arrived, so Support confirms it. A report at 9:55 isn't accepted, because the Check-in Period hasn't ended. |
| NSH-3 | Provider No Show: the provider is fined the Base Cancellation Fee + 30%. The customer gets a full refund + 10%. | Kemas Home is fined 45 + 60 = RM105. Aisyah gets RM200 + RM20. |
| NSH-4 | **Customer No Show:** confirmed by Support from a provider report made after the Check-in Period ends. The report must include a photo taken at the customer's address and the provider's location when the report is made. | The team arrives at 9:50, but no one answers the door. At 10:05 Kemas Home reports Customer No Show with a photo of Aisyah's door and its location. Support confirms it against the address and the arrival note. |
| NSH-5 | Customer No Show: the customer pays the provider the Base Cancellation Fee + 30% of the Hours Fee for the hours after the first hour + travel fee. | The 3 hours after the first hour are worth RM135. Aisyah pays 45 + 40.50 + 20 = RM105.50 and gets RM94.50 back. |
| NSH-6 | A Provider No Show can be confirmed even after the job was marked Completed, if the customer reports it within the Protection Window. The reservation then becomes Cancelled, and its ratings are voided and removed, including any already revealed. Support weighs the completion photo as evidence. | Kemas Home never came, but marked the job Completed at 14:00 with an old photo and rated Aisyah. Aisyah reports on Sunday morning. Support confirms the no-show: the reservation becomes Cancelled, NSH-3 applies, and both ratings are removed. |

---

## 8. Reports

| ID | Rule | Case |
| --- | --- | --- |
| RPT-1 | A report can be made during the Protection Window. | Aisyah can report from Saturday 9:45 until Sunday 14:00. On Monday the report option is closed. |
| RPT-2 | Report categories: Provider No Show (customer only), Customer No Show (provider only), Roster Member Left (provider only), Property Damage, Safety & Misconduct and Other (either side). | Aisyah finds a cracked vase after the job and chooses Property Damage. She can't choose Customer No Show or Roster Member Left, because those are for providers. |
| RPT-3 | A report must have one category and a description. Media attachments are optional. | Aisyah writes "Vase in the living room cracked during cleaning" and attaches a photo. She can't submit with the description empty. |
| RPT-4 | No-show reports are resolved under the No Show rules (§7). | Support confirms Aisyah's Provider No Show report, and NSH-3 applies. |
| RPT-5 | Property Damage, Safety & Misconduct and Other are resolved by Support case by case. | Support reviews Aisyah's vase report, the after-photos and Kemas Home's reply, and decides the outcome. |
| RPT-6 | The provider's payout is held until every report on the reservation is resolved. | Aisyah's report is open on Sunday 14:00, when the Protection Window ends. Kemas Home's payout waits until Support resolves it. |
| RPT-7 | Homa doesn't compensate for property damage. Support helps the customer and provider settle it, and refers to the provider's insurance if the provider is Insurance Covered. | Kemas Home is Insurance Covered. Support points Aisyah to Kemas Home's public liability insurance for the vase. |
| RPT-8 | From the start of the Check-in Period until the job end time, the provider can report **Roster Member Left** when a rostered member has to leave, with a reason: Illness, Emergency or Other. | Nurul feels unwell at 12:00. Kemas Home reports "Roster Member Left: Illness". |
| RPT-9 | The rest of the team continues. The customer is notified that the member left, without the reason. Hours the helper already worked are paid; their worked time is rounded up to the next started 15-minute step, and only the remaining time is refunded automatically. Supervisors aren't billed, so a supervisor leaving gives no refund. | Nurul leaves at 12:00: her 2 worked hours are paid and Aisyah is refunded the remaining 2 hours, 2 × 25 × 0.9 = RM45. If Nurul left at 12:05, the 12:00–12:15 step counts as worked, so Aisyah is refunded 1h 45m = RM39.38. |
| RPT-10 | No fine applies, but the report counts in the provider's reliability (RTG-10). It is resolved automatically when the Protection Window ends, unless the customer disagrees before then, in which case Support reviews it. | Aisyah says Nurul left at 11:00, not 12:00. Support reviews the report time and the messages between Aisyah and Kemas Home before resolving it. |
| RPT-11 | If every rostered helper leaves, it's a Provider Last Minute Cancellation (LMC-3), not a Roster Member Left report. | Both Siti and Nurul leave at 12:00. Kemas Home must cancel in the app, and LMC-3 applies. |

---

## 9. Ratings

| ID | Rule | Case |
| --- | --- | --- |
| RTG-1 | Ratings are two-way: the customer rates the provider and the provider rates the customer. | After the job, Aisyah rates Kemas Home and Kemas Home rates Aisyah. |
| RTG-2 | The Rating Window opens when the job is marked Completed and closes 7 days after the Protection Window ends. If the job is marked Completed after that time, no ratings open. | Kemas Home marks the job Completed at 14:00 on Saturday, so the window opens then. The Protection Window ends Sunday 14:00, so the Rating Window closes the following Sunday at 14:00. Had Kemas Home only marked it Completed two weeks later, no ratings would open. |
| RTG-3 | Ratings are double-blind. Each rating is revealed when both have submitted, or when the Rating Window closes, whichever comes first. | Aisyah rates on Saturday and Kemas Home on Monday, so both are revealed on Monday. If Kemas Home never rates, Aisyah's rating is still revealed when the window closes. |
| RTG-4 | A rating must have 1–5 stars. A description and category ratings (1–5 stars each) are optional. | Aisyah gives 4 stars without a description. That's accepted. She can't submit without choosing stars. |
| RTG-5 | A customer's rating of a provider can include media attachments. A provider's rating of a customer is text only. | Aisyah attaches a photo of her clean kitchen. Kemas Home can only write text about Aisyah, with no photos of her home. |
| RTG-6 | A rating of a provider is visible to that provider and to customers. A rating of a customer is visible to that customer and to providers. | Other customers see Aisyah's 4 stars on Kemas Home's page. Aisyah sees Kemas Home's rating of her, and so do providers she books in future. |
| RTG-7 | Categories when the customer rates the provider: **Quality of work**, **Punctuality**, **Professionalism**. | Aisyah gives Quality 5, Punctuality 4 (the team arrived at 9:58) and Professionalism 5. |
| RTG-8 | Categories when the provider rates the customer: **Accurate request**, **Access and readiness**, **Respect**. | Kemas Home gives Accurate request 5, Access and readiness 3 (the arrival note was missing the guardhouse code) and Respect 5. |
| RTG-9 | Ratings open only for Completed reservations. Cancellations and no-shows are shown through reliability counts (RTG-10) instead. | Aisyah doesn't answer the door and a Customer No Show is confirmed, so no ratings open; the no-show appears in her reliability counts. When the job is Completed, ratings open as usual. |
| RTG-10 | Instead of ratings, reliability is shown as counts over the last 12 months. Customers see a provider's cancellations, no-rosters, no-shows and roster members who left during a job. Providers see a customer's cancellations with a fee and no-shows. Free cancellations aren't counted. | Kemas Home's page shows "Cancellations: 1, No-rosters: 0, No-shows: 0". Aisyah's free cancellation on Monday isn't counted, but her Friday 22:00 cancellation with a RM45 fee is, and providers can see it. |
| RTG-11 | Support can remove a rating that contains abusive content or personal contact or address details. | Kemas Home's rating of a customer includes the customer's phone number. Support removes it. |

---

## 10. Summary of money outcomes

Example amounts use the standard reservation (Total RM200, Base RM45).

| Case | Customer pays | Provider fine | Customer receives | Example |
| --- | --- | --- | --- | --- |
| Cancels a Requested reservation | — | — | Full refund | Gets RM200 |
| Provider declines, or request expires | — | — | Full refund | Gets RM200 |
| Customer cancels within Grace Period | — | — | Full refund | Gets RM200 |
| Customer cancels before tier cut-off | — | — | Full refund | Gets RM200 |
| Customer cancels after tier cut-off | Base | — | Refund minus fee | Pays RM45, gets RM155 |
| Provider cancels, more than 24h away | — | Base | Full refund | Fine RM45; gets RM200 |
| Provider cancels, within 24h | — | Base + 10% | Full refund + 3% | Fine RM65; gets RM206 |
| No roster | — | Base + 12% | Full refund + 4% | Fine RM69; gets RM208 |
| Smaller roster accepted by customer | New price (discount kept) | — | Difference refunded | 1 helper: pays RM110, gets RM90 |
| Roster member left during job (illness, emergency, monitoring flag) | — | — | That member's remaining hours | Nurul leaves at 12:00: gets RM45 |
| Customer cancels last minute | Base + hours worked after first hour (15-min steps) + travel | — | Refund minus fee | At 11:20: pays RM87.50, gets RM112.50 |
| Provider cancels last minute | — | Base + 20% | Full refund + 7% | Fine RM85; gets RM214 |
| Provider no show | — | Base + 30% | Full refund + 10% | Fine RM105; gets RM220 |
| Customer no show | Base + 30% of Hours Fee for hours after first hour + travel | — | Refund minus fee | Pays RM105.50, gets RM94.50 |
| Customer suspended before the Check-in Period | — | — | Full refund | Gets RM200 |
| Provider suspended before the Check-in Period (missed re-verification) | — | As provider cancellation | As provider cancellation | 3 days ahead: fine RM45; gets RM200 |
| Provider suspended before the Check-in Period (monitoring flag) | — | — | Full refund | Gets RM200 |
| Provider suspended mid-job by monitoring flag | Worked time (15-min steps) | — | Unworked time | Stops at 12:00: gets RM90 |

*Percentages are of the Total Reservation Fee. All fees and fines are capped at 100% of the Total Reservation Fee.*

---

## 11. Open items

None at the moment.
