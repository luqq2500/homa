package policy.domain.exception;

/** A Homa-set range starts at 1 or more and its minimum is not above its maximum. */
public class InvalidPolicyRangeException extends PolicyRuleViolationException {

    public InvalidPolicyRangeException(String value, int min, int max) {
        super(value + " must be a range from at least 1 with the minimum not above the maximum, was " + min + " to " + max);
    }
}
