package offering.domain.exception;

/** A provider covers at least one city. */
public class NoCitiesCoveredException extends OfferingRuleViolationException {

    public NoCitiesCoveredException() {
        super("A provider must cover at least one city");
    }
}
