package policy.domain.exception;

public class ChangeReasonMissingException extends RuntimeException {

    public ChangeReasonMissingException() {
        super("A Homa-set value is changed with a reason");
    }
}
