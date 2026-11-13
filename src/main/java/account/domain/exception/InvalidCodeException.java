package account.domain.exception;

public class InvalidCodeException extends RuntimeException {

    public InvalidCodeException() {
        super("An SMS code is 6 digits");
    }
}
