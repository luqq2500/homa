package reservation.domain.exception;

/** A booking has at least one job. */
public class NoJobsChosenException extends ReservationRuleViolationException {

    public NoJobsChosenException() {
        super("A booking needs at least one job");
    }
}
