package offering.domain.exception;

/** Only a paused listing can resume taking requests. */
public class ListingNotPausedException extends OfferingRuleViolationException {

    public ListingNotPausedException() {
        super("The listing is not paused");
    }
}
