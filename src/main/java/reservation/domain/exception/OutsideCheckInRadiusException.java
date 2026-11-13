package reservation.domain.exception;

public class OutsideCheckInRadiusException extends RuntimeException {

    public OutsideCheckInRadiusException() {
        super("The device is not within the check-in radius of the customer's location");
    }
}
