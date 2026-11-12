package reservation.domain.exception;

/** A request cannot be accepted until its payment is secured: the card authorised, or the FPX payment paid. */
public class PaymentNotSecuredException extends ReservationRuleViolationException {

    public PaymentNotSecuredException(String reservationId) {
        super("Reservation " + reservationId + " cannot be accepted before its payment is secured");
    }
}
