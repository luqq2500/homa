package reservation.domain.exception;

/** Completion is submitted once, and a completed job is no longer cancelled or a no-show. */
public class CompletionAlreadySubmittedException extends ReservationRuleViolationException {

    public CompletionAlreadySubmittedException(Object at) {
        super("Completion was already submitted at " + at);
    }
}
