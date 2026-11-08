package reservation.domain.exception;

/** A request can be edited only until the provider accepts it. */
public class ReservationNotEditableException extends ReservationRuleViolationException {

    public ReservationNotEditableException(Object status) {
        super("A " + status + " reservation cannot be edited");
    }
}
