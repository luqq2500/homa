package discovery.domain.exception;

/** Start times are in 15-minute steps. */
public class StartNotOnQuarterHourException extends DiscoveryRuleViolationException {

    public StartNotOnQuarterHourException(Object start) {
        super("A start time must be on a 15-minute step, was " + start);
    }
}
