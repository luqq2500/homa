package reservation.domain.exception;

/** Only a request to a provider who uses Instant book is accepted instantly. */
public class NotInstantBookException extends ReservationRuleViolationException {

    public NotInstantBookException(String reservationId) {
        super("Reservation " + reservationId + " waits for the provider to accept it");
    }
}
