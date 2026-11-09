package account.domain.exception;

/** Only a business provider has an SSM registration to check. */
public class NotABusinessProviderException extends AccountRuleViolationException {

    public NotABusinessProviderException(String providerId) {
        super("Provider " + providerId + " is not a business provider");
    }
}
