package reservation.domain.exception;

public class CannotCancelDuringCheckInPeriodException extends RuntimeException {

    public CannotCancelDuringCheckInPeriodException() {
        super("During the check-in period the customer cannot cancel until the provider checks in or the period ends");
    }
}
