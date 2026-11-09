package reservation.domain.exception;

/** The location check is made once, and check-in completes once. */
public class AlreadyCheckedInException extends ReservationRuleViolationException {

    public AlreadyCheckedInException(Object at) {
        super("The provider already checked in at " + at);
    }
}
