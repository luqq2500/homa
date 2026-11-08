package availability.domain.exception;

/** Calendars and holds belong to someone. */
public class AvailabilityDetailRequiredException extends AvailabilityRuleViolationException {

    public AvailabilityDetailRequiredException(String what) {
        super(what + " is required");
    }
}
