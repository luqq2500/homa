package reservation.domain.exception;

/** A reservation disputed for a flagged provider can be restored only before its booked start. */
public class FlaggedReservationStartedException extends ReservationRuleViolationException {

    public FlaggedReservationStartedException(Object start) {
        super("The booked start " + start + " has passed; the reservation is cancelled with a full refund");
    }
}
