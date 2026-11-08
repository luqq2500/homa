package account.domain.exception;

/** A re-check is already open; it closes when the person passes. */
public class RecheckAlreadyOpenException extends AccountRuleViolationException {

    public RecheckAlreadyOpenException(String personId) {
        super("A re-check is already open for " + personId);
    }
}
