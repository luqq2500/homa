package offering.domain.exception;

/** A live listing does not go live again. */
public class ListingAlreadyLiveException extends OfferingRuleViolationException {

    public ListingAlreadyLiveException() {
        super("The listing is already live");
    }
}
