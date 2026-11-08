package account.domain.exception;

/** An eKYC result is VERIFIED or REJECTED; PENDING is not a result. */
public class EkycResultNotFinalException extends AccountRuleViolationException {

    public EkycResultNotFinalException() {
        super("An eKYC result must be VERIFIED or REJECTED");
    }
}
