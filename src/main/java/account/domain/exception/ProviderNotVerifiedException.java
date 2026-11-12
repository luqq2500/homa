package account.domain.exception;

/** Only a verified provider can lapse, be monitored or add team members. */
public class ProviderNotVerifiedException extends AccountRuleViolationException {

    public ProviderNotVerifiedException(String providerId, Object status) {
        super("Provider " + providerId + " is not verified, but " + status);
    }
}
