package reservation.domain.exception;

/** The payment is released only when the dispute window has ended. */
public class DisputeWindowStillOpenException extends ReservationRuleViolationException {

    public DisputeWindowStillOpenException(Object endsAt) {
        super("The dispute window is open until " + endsAt);
    }
}
