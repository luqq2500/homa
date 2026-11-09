package account.domain.exception;

/** A business provider is verified through eKYB, which includes its owner's or director's eKYC; it does not take a separate eKYC result. */
public class BusinessVerifiesThroughEkybException extends AccountRuleViolationException {

    public BusinessVerifiesThroughEkybException(String providerId) {
        super("Provider " + providerId + " is a business and verifies through eKYB");
    }
}
