package account.domain.exception;

/** A business is not a person, so it is never a helper on its own team; an owner or director who works on site is added as a team member. */
public class BusinessIsNotAHelperException extends AccountRuleViolationException {

    public BusinessIsNotAHelperException(String providerId) {
        super("Provider " + providerId + " is a business and cannot be a helper on its own team");
    }
}
