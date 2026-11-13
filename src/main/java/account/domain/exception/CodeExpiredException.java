package account.domain.exception;

public class CodeExpiredException extends RuntimeException {

    public CodeExpiredException() {
        super("The SMS code has expired; ask for a new one");
    }
}
