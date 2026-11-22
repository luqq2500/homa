package com.homa.customer.domain;

import com.homa.customer.domain.exception.InconsistentSuspensionEndException;
import com.homa.customer.domain.exception.MissingSuspensionLengthException;
import com.homa.customer.domain.exception.MissingSuspensionReasonException;
import com.homa.customer.domain.exception.SuspensionEndsOnItsOwnException;
import java.time.Instant;
import java.util.Optional;

/**
 * A suspension Support decided for a customer (CUS-4): its length, when it ends, its reason and when it started.
 * Support owns the lengths and works out the end, so the suspension holds both as Support gave them. A 7- or 30-day
 * suspension ends on its own once {@link #endsAt()} passes, with nothing stored changing; a suspension until lifted
 * has no end of its own and ends only when Support lifts it (CUS-6).
 */
public final class Suspension {

    private final SuspensionLength length;
    private final String reason;
    private final Instant suspendedAt;
    private final Instant endsAt;
    private final Instant liftedAt;

    private Suspension(SuspensionLength length, String reason, Instant suspendedAt, Instant endsAt, Instant liftedAt) {
        this.length = length;
        this.reason = reason;
        this.suspendedAt = suspendedAt;
        this.endsAt = endsAt;
        this.liftedAt = liftedAt;
    }

    /**
     * CUS-4: Support suspends the customer from {@code suspendedAt} for {@code length}, giving the reason.
     * {@code endsAt} is the end Support worked out: after {@code suspendedAt} for a 7- or 30-day suspension, and
     * {@code null} for one until lifted (CUS-6).
     */
    public static Suspension decided(SuspensionLength length, Instant endsAt, String reason, Instant suspendedAt) {
        if (length == null) {
            throw new MissingSuspensionLengthException();
        }
        if (reason == null || reason.isBlank()) {
            throw new MissingSuspensionReasonException();
        }
        requireEndFitting(length, endsAt, suspendedAt);
        return new Suspension(length, reason, suspendedAt, endsAt, null);
    }

    private static void requireEndFitting(SuspensionLength length, Instant endsAt, Instant suspendedAt) {
        boolean fits = length.endsOnItsOwn() ? endsAfter(endsAt, suspendedAt) : endsAt == null;
        if (!fits) {
            throw new InconsistentSuspensionEndException(length, endsAt);
        }
    }

    private static boolean endsAfter(Instant endsAt, Instant suspendedAt) {
        return endsAt != null && endsAt.isAfter(suspendedAt);
    }

    /** CUS-6: Support lifts a suspension until lifted at {@code now}. A 7- or 30-day one ends only on its own. */
    Suspension liftedAt(Instant now) {
        if (length.endsOnItsOwn()) {
            throw new SuspensionEndsOnItsOwnException(length, endsAt);
        }
        return new Suspension(length, reason, suspendedAt, endsAt, now);
    }

    /**
     * True from the moment the suspension starts until it ends: at {@link #endsAt()} for a 7- or 30-day suspension,
     * or when Support lifts a suspension until lifted (CUS-6).
     */
    public boolean isInForceAt(Instant now) {
        return !now.isBefore(suspendedAt) && notYetPassed(endsAt, now) && notYetPassed(liftedAt, now);
    }

    private static boolean notYetPassed(Instant end, Instant now) {
        return end == null || now.isBefore(end);
    }

    public SuspensionLength length() {
        return length;
    }

    /** Why Support suspended the customer, for example a confirmed Safety &amp; Misconduct report. */
    public String reason() {
        return reason;
    }

    public Instant suspendedAt() {
        return suspendedAt;
    }

    /** When a 7- or 30-day suspension ends on its own, as Support decided (CUS-6). Empty for one until lifted. */
    public Optional<Instant> endsAt() {
        return Optional.ofNullable(endsAt);
    }

    /** When Support lifted a suspension until lifted. Empty until then, and always for a 7- or 30-day one. */
    public Optional<Instant> liftedAt() {
        return Optional.ofNullable(liftedAt);
    }
}
