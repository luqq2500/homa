package reservation.domain.exception;

/** A customer no-show can be reported only after the customer no-show wait. */
public class NoShowWaitNotOverException extends ReservationRuleViolationException {

    public NoShowWaitNotOverException(Object waitEndsAt) {
        super("The customer no-show wait ends at " + waitEndsAt);
    }
}
