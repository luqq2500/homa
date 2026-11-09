package offering.domain.exception;

/** A provider's helpers range starts at 1 or more, and its minimum is not above its maximum. */
public class InvalidHelpersRangeException extends OfferingRuleViolationException {

    public InvalidHelpersRangeException(int min, int max) {
        super("A helpers range of " + min + " to " + max + " must start at 1 or more and not run backwards");
    }
}
