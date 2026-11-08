package discovery.domain.exception;

/** A start time is no later than the booking horizon. */
public class StartBeyondHorizonException extends DiscoveryRuleViolationException {

    public StartBeyondHorizonException(Object start, Object latest) {
        super("The start " + start + " is beyond the booking horizon of " + latest);
    }
}
