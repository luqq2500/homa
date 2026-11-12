package reservation.domain.exception;

/** A customer no-show is reported by the booked end. */
public class NoShowReportTooLateException extends ReservationRuleViolationException {

    public NoShowReportTooLateException(Object end) {
        super("A customer no-show can be reported only until the booked end at " + end);
    }
}
