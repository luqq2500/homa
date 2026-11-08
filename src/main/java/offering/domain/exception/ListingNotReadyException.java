package offering.domain.exception;

import java.util.Set;

/** A listing goes live only once everything is set up and everyone is verified. */
public class ListingNotReadyException extends OfferingRuleViolationException {

    private final Set<?> missing;

    public ListingNotReadyException(Set<?> missing) {
        super("The listing is not ready to go live; missing: " + missing);
        this.missing = Set.copyOf(missing);
    }

    /** What still blocks go-live, as go-live requirements. */
    public Set<?> missing() {
        return missing;
    }
}
