package com.homa.ie.events;

import com.homa.ie.port.SupportIE;
import com.homa.kernel.Money;
import java.time.Instant;

/**
 * Support decided that a Safety & Misconduct or Other report earns the customer a refund (RPT-5). Homa doesn't pay
 * for property damage (RPT-7), so a Property Damage report never carries one. Reservation settles it as a
 * {@code SettlementAdjustedIE} with reason SUPPORT_REFUND, so Payment refunds it at once (PAY-7), and answers with
 * {@code ReportEffectAppliedIE}.
 *
 * @param reportId  the report the refund is for
 * @param decidedAt when Support decided
 * @param refund   what goes back to the customer from the held payment; never above what is still held
 */
public record ReportRefundDecidedIE(
        String eventId,
        String reservationId,
        String reportId,
        String customerId,
        String providerId,
        Money refund,
        Instant decidedAt,
        Instant occurredAt) implements SupportIE {
}
