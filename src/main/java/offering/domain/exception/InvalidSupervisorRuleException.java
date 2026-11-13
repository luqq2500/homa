package offering.domain.exception;

public class InvalidSupervisorRuleException extends RuntimeException {

    public InvalidSupervisorRuleException() {
        super("A supervisor is sent from a number of helpers or hours of at least 1");
    }
}
