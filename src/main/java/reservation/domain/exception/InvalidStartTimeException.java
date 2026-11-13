package reservation.domain.exception;

public class InvalidStartTimeException extends RuntimeException {

    public InvalidStartTimeException(String problem) {
        super(problem);
    }
}
