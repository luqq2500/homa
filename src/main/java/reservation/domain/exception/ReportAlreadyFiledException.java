package reservation.domain.exception;

/** The customer and the provider each file at most one report. */
public class ReportAlreadyFiledException extends ReservationRuleViolationException {

    public ReportAlreadyFiledException(Object party) {
        super("The " + party + " already filed a report");
    }
}
