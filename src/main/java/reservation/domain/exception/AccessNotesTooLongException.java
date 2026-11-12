package reservation.domain.exception;

/** Access notes are at most 500 characters. */
public class AccessNotesTooLongException extends ReservationRuleViolationException {

    public AccessNotesTooLongException(int length, int max) {
        super("Access notes are at most " + max + " characters, were " + length);
    }
}
