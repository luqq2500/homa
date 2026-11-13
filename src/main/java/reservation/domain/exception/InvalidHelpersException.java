package reservation.domain.exception;

public class InvalidHelpersException extends RuntimeException {

    public InvalidHelpersException(int min, int max) {
        super("The number of helpers is from " + min + " to " + max);
    }
}
