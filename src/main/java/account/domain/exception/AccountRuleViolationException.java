package account.domain.exception;

/** A business rule of accounts and verification was broken. Every account domain exception is one of these. */
public abstract class AccountRuleViolationException extends RuntimeException {

    protected AccountRuleViolationException(String message) {
        super(message);
    }
}
