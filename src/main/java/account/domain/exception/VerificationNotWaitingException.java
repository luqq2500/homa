package account.domain.exception;

/** Only a verification still waiting takes a new eKYC result or expires; an ended one stays ended. */
public class VerificationNotWaitingException extends AccountRuleViolationException {

    public VerificationNotWaitingException(String verificationId, Object status) {
        super("Verification " + verificationId + " is no longer waiting, but " + status);
    }
}
