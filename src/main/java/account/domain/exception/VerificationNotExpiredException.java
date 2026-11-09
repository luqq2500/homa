package account.domain.exception;

/** An unfinished eKYC expires only once the verification link expiry has passed. */
public class VerificationNotExpiredException extends AccountRuleViolationException {

    public VerificationNotExpiredException(Object expiresAt) {
        super("The verification does not expire until " + expiresAt);
    }
}
