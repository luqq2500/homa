package availability.domain.exception;

/** The buffer a provider keeps between jobs cannot be negative. */
public class NegativeBufferBetweenJobsException extends AvailabilityRuleViolationException {

    public NegativeBufferBetweenJobsException(Object buffer) {
        super("The buffer between jobs cannot be negative, was " + buffer);
    }
}
