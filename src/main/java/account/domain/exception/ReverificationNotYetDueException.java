package account.domain.exception;

/** Only someone whose re-verification due date has come can lapse. */
public class ReverificationNotYetDueException extends AccountRuleViolationException {

    public ReverificationNotYetDueException(Object dueAt) {
        super("Re-verification is not due until " + dueAt);
    }
}
