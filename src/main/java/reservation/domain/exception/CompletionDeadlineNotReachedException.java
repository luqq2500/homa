package reservation.domain.exception;

/** Homa reviews a missing completion only after the completion deadline. */
public class CompletionDeadlineNotReachedException extends ReservationRuleViolationException {

    public CompletionDeadlineNotReachedException(Object deadline) {
        super("The completion deadline is " + deadline);
    }
}
