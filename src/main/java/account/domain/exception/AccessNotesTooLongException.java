package account.domain.exception;

public class AccessNotesTooLongException extends RuntimeException {

    public AccessNotesTooLongException(int limit) {
        super("Access notes are up to " + limit + " characters");
    }
}
