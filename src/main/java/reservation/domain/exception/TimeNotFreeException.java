package reservation.domain.exception;

public class TimeNotFreeException extends RuntimeException {

    public TimeNotFreeException() {
        super("The provider is not free at this time");
    }
}
