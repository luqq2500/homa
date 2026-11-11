package account.domain.exception;

/** A result from the KYC provider is VERIFIED or REJECTED; PENDING or LAPSED is not a result. */
public class VerificationResultNotFinalException extends AccountRuleViolationException {

    public VerificationResultNotFinalException() {
        super("A verification result must be VERIFIED or REJECTED");
    }
}
