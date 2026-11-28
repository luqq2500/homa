# 0005. A Support decision is applied before its report is resolved

Status: accepted, pending the product owner's confirmation that the plan's event catalog may follow (see
`docs/open-questions.md`, OQ-8).

## Context

A payout waits until every report on its reservation is resolved (RPT-6). Payment frees the hold on `ReportResolvedIE`.
Three kinds of decision change the money after Support has made them:

- a confirmed Provider No Show voids the JOB payout, even after the job was marked Completed (NSH-3, NSH-6);
- a refund Support decides lowers the payout and is paid at once (RPT-5, PAY-7);
- a corrected leave time in a Roster Member Left dispute raises the refund (RPT-10).

The plan's catalog has `ReportResolvedIE` carry an optional `decidedRefund` that Reservation settles. Published like
that, Payment frees the hold at the same moment Reservation starts working out the refund, so the release scheduler can
send the payout in between, and a payout that has left cannot be voided or lowered. The same race exists for a
no-show: the report resolves, the hold is freed, and the cancellation that voids the payout is still on its way.

## Decision

A report moves OPEN, then DECIDED when its decision needs another context to apply it, then RESOLVED once that context
confirms it did. Support never publishes `ReportResolvedIE` for a decided report on its own authority.

1. Support publishes the decision: `ProviderNoShowConfirmedIE`, `CustomerNoShowConfirmedIE`, `ReportRefundDecidedIE`
   or `RosterMemberLeftDisputeResolvedIE`. The refund moves out of `ReportResolvedIE` into `ReportRefundDecidedIE`.
2. Reservation applies it (the cancellation, or a `SettlementAdjustedIE`) and then publishes
   `ReportEffectAppliedIE` for the report, with `applied` false when there was nothing to apply.
3. Support resolves the report on `ReportEffectAppliedIE` and publishes `ReportResolvedIE`. Payment frees the hold.

A decision with no effect elsewhere (a rejected no-show, a case resolved without a refund, a Roster Member Left report
that resolves itself at the end of the Protection Window) is resolved at once.

Every event of the flow is about the reservation, so each carries `reservationId` as its aggregate id, right after
`eventId`, and the relay delivers them in the order they were published. Payment therefore handles the cancellation or
the adjustment before it handles `ReportResolvedIE`, even when a delivery has to be retried.

## Consequences

- No payout is released between a Support decision and its effect. If Reservation cannot apply a decision, the report
  stays DECIDED, the payout stays held and the failed delivery is retried and reported (EXT-3), not skipped.
- Support depends on `ReportEffectAppliedIE` from Reservation: one more event, and one state in `Report`.
- The plan's `ReportResolvedIE` loses `decidedRefund`; `ReportRefundDecidedIE` and `ReportEffectAppliedIE` are new.
