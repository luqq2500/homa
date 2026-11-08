package reservation.domain.exception;

/** The provider must check in by the booked start plus the check-in period. */
public class CheckInPeriodOverException extends ReservationRuleViolationException {

    public CheckInPeriodOverException(Object closedAt) {
        super("The check-in period ended at " + closedAt);
    }
}
