package reservation.domain.exception;

/** After the check-in period without a check-in, the reservation ends as a provider no-show, not a customer cancellation. */
public class ProviderMissedCheckInException extends ReservationRuleViolationException {

    public ProviderMissedCheckInException() {
        super("The provider did not check in; the reservation ends as a provider no-show");
    }
}
