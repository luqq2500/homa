package account.domain.exception;

/** Homa checks a business's registration once. */
public class BusinessRegistrationAlreadyCheckedException extends AccountRuleViolationException {

    public BusinessRegistrationAlreadyCheckedException(String providerId) {
        super("The business registration of provider " + providerId + " is already checked");
    }
}
