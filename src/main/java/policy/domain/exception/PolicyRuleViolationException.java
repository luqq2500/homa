package policy.domain.exception;

/** A rule for Homa-set values was broken. Every policy domain exception is one of these. */
public abstract class PolicyRuleViolationException extends RuntimeException {

    protected PolicyRuleViolationException(String message) {
        super(message);
    }
}
