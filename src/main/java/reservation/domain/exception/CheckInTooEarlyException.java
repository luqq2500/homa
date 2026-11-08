package reservation.domain.exception;

/** The provider can check in from the earliest check-in time. */
public class CheckInTooEarlyException extends ReservationRuleViolationException {

    public CheckInTooEarlyException(Object opensAt) {
        super("Check-in opens at " + opensAt);
    }
}
