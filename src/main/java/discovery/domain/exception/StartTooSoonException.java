package discovery.domain.exception;

/** A start time is at least the minimum notice from now. */
public class StartTooSoonException extends DiscoveryRuleViolationException {

    public StartTooSoonException(Object start, Object earliest) {
        super("The start " + start + " is too soon; the earliest start is " + earliest);
    }
}
