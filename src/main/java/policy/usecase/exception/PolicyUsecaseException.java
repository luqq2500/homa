package policy.usecase.exception;

/** A policy usecase could not be carried out. Every policy usecase exception is one of these. */
public abstract class PolicyUsecaseException extends RuntimeException {

    protected PolicyUsecaseException(String message) {
        super(message);
    }
}
