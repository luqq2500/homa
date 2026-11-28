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
| OQ-7 | When the customer declines a smaller roster, is the reservation cancelled at once, or may the provider still complete the roster before the Check-in Period (ROS-7, ROS-8)? ROS-8 says "had she declined, ROS-7 would apply", and ROS-7 is timed to the start of the Check-in Period. | The provider can still complete the roster until the Check-in Period starts; the No Roster Policy applies then. A customer who never answers is treated the same way. | `reservation.domain`: `Reservation.declineProposal`, `lockRosterAtCheckIn` |
| OQ-8 | May the plan's event catalog follow ADR 0005? A report Support decides stays DECIDED until Reservation confirms it applied the decision (`ReportEffectAppliedIE`), and the refund moves from `ReportResolvedIE.decidedRefund` to a new `ReportRefundDecidedIE`, so a payout is never freed before a no-show voids it or a decided refund lowers it (RPT-6, NSH-6, RPT-5). The Notion plan still has `decidedRefund` on `ReportResolvedIE`. | Built the ADR 0005 way. | `ie.events`, `support.domain`: `Report`, `reservation.usecase` |

Answered and moved: eKYB retry, the 30-day purge of technical rows, AC (one retry then Support), AD (outages recorded
automatically, Support can correct) and AG (21 days), and Google as the sign-in provider. See `docs/decisions/`.
