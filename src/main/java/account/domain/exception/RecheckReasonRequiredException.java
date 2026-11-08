package account.domain.exception;

/** Homa records why it asks for an early re-check. */
public class RecheckReasonRequiredException extends AccountRuleViolationException {

    public RecheckReasonRequiredException() {
        super("An early re-check needs a reason");
    }
}
