package com.homa.verification.domain;

import com.homa.kernel.BusinessCalendar;
import java.time.Instant;

/**
 * MON-1: owners and members re-verify every {@code cycleMonths} calendar months on the Kuala Lumpur calendar,
 * counted from their last pass. Siti, last verified on 1 June, is due on 1 December; she re-verifies on 20 November
 * and is next due on 20 May. A pass on 31 August falls due on the last day of February.
 *
 * <p>Hook (slice 6): the reminders from 60 days before (MON-1), the 14-day deadline once flagged (MON-2) and the
 * Business provider's cycle (MON-5) land here, reading {@link VerificationValues}. So does
 * {@code deadlineFor(..., outages)}, which moves a deadline by each verification-service outage it overlaps
 * (EXT-5, from the outage log).
 */
public record VerificationCycle(int cycleMonths) {

    /** The cycle for owners and members: every 6 months. */
    public static final VerificationCycle EVERY_SIX_MONTHS =
            new VerificationCycle(VerificationValues.REVERIFICATION_CYCLE_MONTHS);

    /** When re-verification falls due after a pass at {@code passedAt}: the same Kuala Lumpur time, a cycle later. */
    public Instant nextDueAt(Instant passedAt) {
        return BusinessCalendar.plusMonths(passedAt, cycleMonths);
    }
}
