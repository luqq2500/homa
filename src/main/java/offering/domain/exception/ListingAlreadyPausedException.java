package offering.domain.exception;

/** A paused listing is already taking no new requests. */
public class ListingAlreadyPausedException extends OfferingRuleViolationException {

    public ListingAlreadyPausedException() {
        super("The listing is already paused");
    }
}
