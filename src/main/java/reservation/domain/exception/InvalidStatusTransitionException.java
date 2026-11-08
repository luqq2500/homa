package reservation.domain.exception;

/** Each step of the lifecycle is allowed only from certain statuses. */
public class InvalidStatusTransitionException extends ReservationRuleViolationException {

    public InvalidStatusTransitionException(Object status, String action) {
        super("A " + status + " reservation cannot be " + action);
    }
}
