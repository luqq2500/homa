package account.usecase.exception;

/** An account usecase could not be carried out. Every account usecase exception is one of these. */
public abstract class AccountUsecaseException extends RuntimeException {

    protected AccountUsecaseException(String message) {
        super(message);
    }
}
