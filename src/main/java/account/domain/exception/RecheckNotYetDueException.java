package account.domain.exception;

/** The regular re-check opens only at the re-verification reminder before the due date. */
public class RecheckNotYetDueException extends AccountRuleViolationException {

    public RecheckNotYetDueException(Object opensAt) {
        super("The re-check opens at " + opensAt);
    }
}
