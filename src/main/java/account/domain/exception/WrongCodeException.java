package account.domain.exception;

public class WrongCodeException extends RuntimeException {

    public WrongCodeException() {
        super("The SMS code is wrong");
    }
}
