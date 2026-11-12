package offering.domain.exception;

/** Insurance that has already expired can't be declared. */
public class InsuranceExpiredException extends OfferingRuleViolationException {

    public InsuranceExpiredException(Object validUntil) {
        super("The insurance was valid only until " + validUntil);
    }
}
