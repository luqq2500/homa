package availability.domain.exception;

/** A slot, window or busy block ends after it starts. */
public class TimeRangeEndsBeforeItStartsException extends AvailabilityRuleViolationException {

    public TimeRangeEndsBeforeItStartsException(Object start, Object end) {
        super("A time range must end after it starts, was " + start + " to " + end);
    }
}
