package reservation.domain.exception;

/** The provider can't decline an instant-book request; it is accepted once it is paid. */
public class InstantBookCannotBeDeclinedException extends ReservationRuleViolationException {

    public InstantBookCannotBeDeclinedException(String reservationId) {
        super("Reservation " + reservationId + " uses Instant book and cannot be declined");
    }
}
