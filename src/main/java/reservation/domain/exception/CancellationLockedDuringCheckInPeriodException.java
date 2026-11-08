package reservation.domain.exception;

/** During the check-in period the customer cannot cancel until the provider checks in or the period ends. */
public class CancellationLockedDuringCheckInPeriodException extends ReservationRuleViolationException {

    public CancellationLockedDuringCheckInPeriodException() {
        super("The customer cannot cancel during the check-in period before the provider checks in");
    }
}
