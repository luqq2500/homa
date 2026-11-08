package policy.domain;

import policy.domain.exception.InvalidPolicyRangeException;
import policy.domain.exception.PolicyPeriodNotPositiveException;
import java.time.Duration;

/** Shape rules shared by Homa-set periods and ranges. */
final class PolicyShape {

    private PolicyShape() {
    }

    static void requirePeriod(String value, Duration period) {
        if (period.isNegative() || period.isZero()) {
            throw new PolicyPeriodNotPositiveException(value, period);
        }
    }

    static void requireRange(String value, int min, int max) {
        if (min < 1 || min > max) {
            throw new InvalidPolicyRangeException(value, min, max);
        }
    }
}
