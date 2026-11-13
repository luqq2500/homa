package verification.domain.exception;

public class WorkPassNotConfirmedException extends RuntimeException {

    public WorkPassNotConfirmedException() {
        super("For a document from another country, the provider confirms a valid work pass for this provider");
    }
}
