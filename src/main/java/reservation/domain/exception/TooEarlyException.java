package reservation.domain.exception;

public class TooEarlyException extends RuntimeException {

    public TooEarlyException(String action) {
        super("It is too early to " + action);
    }
}
