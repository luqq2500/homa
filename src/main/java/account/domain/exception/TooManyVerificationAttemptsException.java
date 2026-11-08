package account.domain.exception;

/** After too many wrong codes, the customer needs a new code. */
public class TooManyVerificationAttemptsException extends AccountRuleViolationException {

    public TooManyVerificationAttemptsException(int attempts) {
        super("The code was entered wrongly " + attempts + " times; a new code is needed");
    }
}
