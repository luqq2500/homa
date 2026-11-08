package reservation.domain.exception;

/** This needs the provider to have checked in. */
public class NotCheckedInException extends ReservationRuleViolationException {

    public NotCheckedInException() {
        super("The provider has not checked in");
    }
}
