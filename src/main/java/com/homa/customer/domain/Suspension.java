package com.homa.customer.domain;

import com.homa.customer.domain.exception.MissingSuspensionPeriodException;
import com.homa.customer.domain.exception.MissingSuspensionReasonException;
import com.homa.customer.domain.exception.SuspensionEndsOnItsOwnException;
import java.time.Instant;
import java.util.Optional;

/**
 * A suspension Support puts on a customer (CUS-4): its period, its reason and when it started. A suspension for a
 * set period ends on its own once {@link #endsAt()} passes, with nothing stored changing; a suspension until lifted
 * ends only when Support lifts it (CUS-6).
 */
public final class Suspension {

    private final SuspensionPeriod period;
    private final String reason;
    private final Instant suspendedAt;
    private final Instant endsAt;
    private final Instant liftedAt;

    private Suspension(SuspensionPeriod period, String reason, Instant suspendedAt, Instant endsAt, Instant liftedAt) {
        this.period = period;
        this.reason = reason;
        this.suspendedAt = suspendedAt;
        this.endsAt = endsAt;
        this.liftedAt = liftedAt;
    }

    /** CUS-4: Support suspends for a period, giving the reason, from {@code now}. */
    static Suspension start(SuspensionPeriod period, String reason, Instant now) {
        if (period == null) {
            throw new MissingSuspensionPeriodException();
        }
        if (reason == null || reason.isBlank()) {
            throw new MissingSuspensionReasonException();
        }
        return new Suspension(period, reason, now, period.endsAt(now).orElse(null), null);
    }

    /** CUS-6: Support lifts a suspension until lifted at {@code now}. One for a set period ends only on its own. */
    Suspension liftedAt(Instant now) {
        if (period != SuspensionPeriod.UNTIL_LIFTED) {
            throw new SuspensionEndsOnItsOwnException(period, endsAt);
        }
        return new Suspension(period, reason, suspendedAt, endsAt, now);
    }

    /**
     * True from the moment the suspension starts until it ends: at {@link #endsAt()} for a set period, or when
     * Support lifts a suspension until lifted (CUS-6).
     */
    public boolean isInForceAt(Instant now) {
        return !now.isBefore(suspendedAt) && notYetPassed(endsAt, now) && notYetPassed(liftedAt, now);
    }

    private static boolean notYetPassed(Instant end, Instant now) {
        return end == null || now.isBefore(end);
    }

    public SuspensionPeriod period() {
        return period;
    }

    /** Why Support suspended the customer, for example a confirmed Safety &amp; Misconduct report. */
    public String reason() {
        return reason;
    }

    public Instant suspendedAt() {
        return suspendedAt;
    }

    /** When a suspension for a set period runs out (CUS-6). Empty for a suspension until lifted. */
    public Optional<Instant> endsAt() {
        return Optional.ofNullable(endsAt);
    }

    /** When Support lifted a suspension until lifted. Empty until then, and always for a set period. */
    public Optional<Instant> liftedAt() {
        return Optional.ofNullable(liftedAt);
    }
}
