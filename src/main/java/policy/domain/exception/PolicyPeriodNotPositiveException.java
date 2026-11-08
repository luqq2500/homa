package policy.domain.exception;

/** Every Homa-set period, such as the check-in period, is longer than zero. */
public class PolicyPeriodNotPositiveException extends PolicyRuleViolationException {

    public PolicyPeriodNotPositiveException(String value, Object period) {
        super(value + " must be longer than zero, was " + period);
    }
}
