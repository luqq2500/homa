package offering.domain.exception;

/** A provider's minimum helpers are not above their maximum. */
public class InvalidHelpersRangeException extends OfferingRuleViolationException {

    public InvalidHelpersRangeException(int min, int max) {
        super("The minimum helpers " + min + " are above the maximum " + max);
    }
}
