package offering.domain.exception;

/** Codes, cities and helper ids cannot be left empty. */
public class OfferingDetailRequiredException extends OfferingRuleViolationException {

    public OfferingDetailRequiredException(String what) {
        super(what + " is required");
    }
}
