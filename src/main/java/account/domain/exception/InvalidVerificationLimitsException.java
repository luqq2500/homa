package account.domain.exception;

/** Re-verification needs a period of at least a month, and a reminder and early re-check deadline longer than zero. */
public class InvalidVerificationLimitsException extends AccountRuleViolationException {

    public InvalidVerificationLimitsException(String what, Object value) {
        super("Invalid " + what + ": " + value);
    }
}
