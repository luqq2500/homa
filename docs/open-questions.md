# Open questions

Decisions that belong to the product owner and are **not made yet**. Nothing in the code may assume an answer: where a
choice had to be made to keep building, it is noted here with what the code does today, so the answer can change it in
one place. Move an item to `docs/decisions/` when it is answered.

| # | Question | What the code does today | Where it lives |
| --- | --- | --- | --- |
| OQ-1 | Does a provider cancelled by suspension count against its reliability (RTG-10)? | Counts it only when a fine was charged, which leaves out a monitoring-flag suspension (SUS-3). | `rating.domain`: `ReliabilityRecord`, `Cancellation` |
| OQ-2 | May a suspended customer keep writing in chats that already exist? | They can't start a new enquiry (CHT-1, CUS-5). Writing in existing chats stays allowed, but isn't enforced until `CustomerSuspendedIE` exists (slice 6). | `conversation.domain`: `CustomerStanding`, `Conversation` |
| OQ-3 | Is a provider that cancels inside the customer's own Grace Period still fined (CCX-1, PCX-1)? | Yes, the fine applies. | `reservation.domain`: `ProviderCancellationPolicy` |
| OQ-4 | Which payment provider? | None: `Unconfigured` adapters fail as "provider unavailable" (EXT-2). Webhooks use Homa's own neutral envelope. | `payment.infra` (not built yet) |
| OQ-5 | Which verification service, and which account-holder check? | None: `Unconfigured` adapters fail as "service unavailable" (EXT-5). | `verification.infra` (not built yet) |
| OQ-6 | Where do completion photos live? | Nowhere: a completion is refused with "photo store unavailable". S3 is the natural fit on ECS/Fargate but needs an SDK dependency. | `reservation.infra` (not built yet) |

Answered and moved: eKYB retry, the 30-day purge of technical rows, AC (one retry then Support), AD (outages recorded
automatically, Support can correct) and AG (21 days), and Google as the sign-in provider. See `docs/decisions/`.
