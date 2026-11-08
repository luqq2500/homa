package reservation.domain.exception;

/** The provider checks in once. */
public class AlreadyCheckedInException extends ReservationRuleViolationException {

    public AlreadyCheckedInException(Object at) {
        super("The provider already checked in at " + at);
    }
}
