package account.domain.exception;

/** Only a business provider verifies through eKYB; an independent provider verifies through eKYC. */
public class NotABusinessProviderException extends AccountRuleViolationException {

    public NotABusinessProviderException(String providerId) {
        super("Provider " + providerId + " is not a business provider");
    }
}
