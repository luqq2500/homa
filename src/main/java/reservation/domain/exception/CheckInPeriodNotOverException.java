package reservation.domain.exception;

/** A provider no-show is marked only after the check-in period ends. */
public class CheckInPeriodNotOverException extends ReservationRuleViolationException {

    public CheckInPeriodNotOverException(Object closesAt) {
        super("The check-in period is open until " + closesAt);
    }
}
