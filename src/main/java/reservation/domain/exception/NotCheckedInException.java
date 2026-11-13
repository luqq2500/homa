package reservation.domain.exception;

public class NotCheckedInException extends RuntimeException {

    public NotCheckedInException() {
        super("The provider has not checked in");
    }
}
