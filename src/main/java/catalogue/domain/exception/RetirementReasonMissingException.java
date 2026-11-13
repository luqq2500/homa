package catalogue.domain.exception;

public class RetirementReasonMissingException extends RuntimeException {

    public RetirementReasonMissingException() {
        super("Retiring a job or tool needs a reason");
    }
}
