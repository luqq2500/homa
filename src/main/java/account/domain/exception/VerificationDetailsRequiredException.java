package account.domain.exception;

/** A verified result from the KYC provider carries what Homa needs: who was verified, and until when. */
public class VerificationDetailsRequiredException extends AccountRuleViolationException {

    public VerificationDetailsRequiredException(String what) {
        super("A verified result needs " + what);
    }
}
