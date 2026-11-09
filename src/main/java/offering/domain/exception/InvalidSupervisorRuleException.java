package offering.domain.exception;

/** A supervisor rule sends one from at least 1 helper, at least 1 hour, or both. */
public class InvalidSupervisorRuleException extends OfferingRuleViolationException {

    public InvalidSupervisorRuleException(String problem) {
        super("A supervisor rule " + problem);
    }
}
