package feedback.domain.exception;

/** A reservation is rated once. */
public class AlreadyRatedException extends FeedbackRuleViolationException {

    public AlreadyRatedException(String reservationId) {
        super("Reservation " + reservationId + " has already been rated");
    }
}
