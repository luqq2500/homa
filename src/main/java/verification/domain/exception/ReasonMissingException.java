package verification.domain.exception;

public class ReasonMissingException extends RuntimeException {

    public ReasonMissingException() {
        super("A reason is needed");
    }
}
