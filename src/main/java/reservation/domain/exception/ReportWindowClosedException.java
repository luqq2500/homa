package reservation.domain.exception;

/** Reports and challenges are accepted only while their window is open. */
public class ReportWindowClosedException extends ReservationRuleViolationException {

    public ReportWindowClosedException(Object window) {
        super("The window for this is closed: " + window);
    }
}
