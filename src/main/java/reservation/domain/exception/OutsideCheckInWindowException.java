package reservation.domain.exception;

public class OutsideCheckInWindowException extends RuntimeException {

    public OutsideCheckInWindowException() {
        super("Check-in is from the earliest check-in time up to and including the end of the check-in period");
    }
}
