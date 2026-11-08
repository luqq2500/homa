package reservation.domain.exception;

/** A start time is no later than the booking horizon. */
public class StartBeyondHorizonException extends ReservationRuleViolationException {

    public StartBeyondHorizonException(Object start, Object latest) {
        super("The start " + start + " is beyond the booking horizon of " + latest);
    }
}
