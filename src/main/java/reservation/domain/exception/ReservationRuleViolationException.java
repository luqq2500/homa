package reservation.domain.exception;

/** A business rule of enquiries or reservations was broken. Every reservation domain exception is one of these. */
public abstract class ReservationRuleViolationException extends RuntimeException {

    protected ReservationRuleViolationException(String message) {
        super(message);
    }
}
