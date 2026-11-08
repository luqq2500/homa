package policy.domain.exception;

/** Re-verification is due at least one month after the last verification. */
public class ReverificationPeriodNotPositiveException extends PolicyRuleViolationException {

    public ReverificationPeriodNotPositiveException(int months) {
        super("The re-verification period must be at least 1 month, was " + months);
    }
}
