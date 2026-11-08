package offering.domain.exception;

/** A provider's helpers range sits within Homa's helpers per search. */
public class HelpersOutOfRangeException extends OfferingRuleViolationException {

    public HelpersOutOfRangeException(int min, int max, int homaMin, int homaMax) {
        super("Helpers " + min + " to " + max + " must sit within Homa's range of " + homaMin + " to " + homaMax);
    }
}
