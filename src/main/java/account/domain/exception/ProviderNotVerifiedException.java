package account.domain.exception;

/** Re-verification is for verified providers only. */
public class ProviderNotVerifiedException extends AccountRuleViolationException {

    public ProviderNotVerifiedException(String providerId, Object status) {
        super("Provider " + providerId + " is not verified, but " + status);
    }
}
