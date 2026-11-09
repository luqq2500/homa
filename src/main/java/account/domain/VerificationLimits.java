package account.domain;

import account.domain.exception.InvalidVerificationLimitsException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/**
 * account's view of the Homa-set verification values: the re-verification period in months, the reminder
 * before the due date, the early re-check deadline, and the verification link expiry for adding a team
 * member. At launch they are 3 months, 14 days, 14 days and 7 days.
 */
public record VerificationLimits(int periodMonths, Duration reminder, Duration earlyRecheckDeadline, Duration linkExpiry) {

    /** Homa operates in Malaysia, so a period of months is counted on Malaysian calendar dates. */
    static final ZoneId HOMA_TIME = ZoneId.of("Asia/Kuala_Lumpur");

    public VerificationLimits {
        if (periodMonths < 1) {
            throw new InvalidVerificationLimitsException("re-verification period in months", periodMonths);
        }
        requirePositive("re-verification reminder", reminder);
        requirePositive("early re-check deadline", earlyRecheckDeadline);
        requirePositive("verification link expiry", linkExpiry);
    }

    /** When re-verification is due for someone verified at {@code verifiedAt}. */
    Instant dueAfter(Instant verifiedAt) {
        return verifiedAt.atZone(HOMA_TIME).plusMonths(periodMonths).toInstant();
    }

    private static void requirePositive(String what, Duration period) {
        if (period == null || period.isNegative() || period.isZero()) {
            throw new InvalidVerificationLimitsException(what, period);
        }
    }
}
