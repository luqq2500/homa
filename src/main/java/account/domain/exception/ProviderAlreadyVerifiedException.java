package account.domain.exception;

/** A verified provider with no re-check open is not verified again; a rejection then changes nothing. */
public class ProviderAlreadyVerifiedException extends AccountRuleViolationException {

    public ProviderAlreadyVerifiedException(String providerId) {
        super("Provider " + providerId + " is already verified");
    }
}
