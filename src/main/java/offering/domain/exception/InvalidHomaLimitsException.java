package offering.domain.exception;

/** Homa's ranges start at 1 or more, with the minimum not above the maximum. */
public class InvalidHomaLimitsException extends OfferingRuleViolationException {

    public InvalidHomaLimitsException(String what, int min, int max) {
        super(what + " must be a range from at least 1, was " + min + " to " + max);
    }
}
