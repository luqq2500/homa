package account.domain;

import account.domain.exception.RecheckAlreadyOpenException;
import account.domain.exception.RecheckNotYetDueException;
import account.domain.exception.RecheckReasonRequiredException;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * When a provider or team member was last verified, when their re-verification is due, and whether a
 * re-check is open. A passed re-check starts a new period from the new verification.
 *
 * @param earlyRecheckReason why Homa asked for an early re-check; empty for the regular re-check or when
 *     no re-check is open
 */
public record Verification(Instant lastVerified, Instant dueAt, boolean recheckOpen, Optional<String> earlyRecheckReason) {

    public Verification {
        Objects.requireNonNull(lastVerified);
        Objects.requireNonNull(dueAt);
        Objects.requireNonNull(earlyRecheckReason);
    }

    /** A successful verification at {@code verifiedAt}: due one period later, with no re-check open. */
    static Verification passedAt(Instant verifiedAt, VerificationLimits limits) {
        return new Verification(verifiedAt, limits.dueAfter(verifiedAt), false, Optional.empty());
    }

    /** When the regular re-check opens: the reminder before the due date. */
    public Instant recheckOpensAt(VerificationLimits limits) {
        return dueAt.minus(limits.reminder());
    }

    /** Whether the due date has come, including its exact moment. */
    public boolean isDue(Instant now) {
        return !now.isBefore(dueAt);
    }

    Verification withRecheckOpened(String personId, VerificationLimits limits, Instant now) {
        if (recheckOpen) {
            throw new RecheckAlreadyOpenException(personId);
        }
        Instant opensAt = recheckOpensAt(limits);
        if (now.isBefore(opensAt)) {
            throw new RecheckNotYetDueException(opensAt);
        }
        return new Verification(lastVerified, dueAt, true, Optional.empty());
    }

    /** An early re-check opens now, and moves the due date to the early re-check deadline if that is sooner. */
    Verification withEarlyRecheck(String reason, VerificationLimits limits, Instant now) {
        if (reason == null || reason.isBlank()) {
            throw new RecheckReasonRequiredException();
        }
        Instant deadline = now.plus(limits.earlyRecheckDeadline());
        Instant due = deadline.isBefore(dueAt) ? deadline : dueAt;
        return new Verification(lastVerified, due, true, Optional.of(reason.strip()));
    }
}
