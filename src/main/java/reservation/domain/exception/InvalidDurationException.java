package reservation.domain.exception;

public class InvalidDurationException extends RuntimeException {

    public InvalidDurationException(int min, int max) {
        super("The duration is from " + min + " to " + max + " whole hours");
    }
}
