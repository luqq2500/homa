package reservation.domain.exception;

/** The dispute window has no end until completion is submitted. */
public class CompletionNotSubmittedException extends ReservationRuleViolationException {

    public CompletionNotSubmittedException() {
        super("Completion has not been submitted");
    }
}
