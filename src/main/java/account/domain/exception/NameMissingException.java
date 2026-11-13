package account.domain.exception;

public class NameMissingException extends RuntimeException {

    public NameMissingException() {
        super("A customer gives their name");
    }
}
