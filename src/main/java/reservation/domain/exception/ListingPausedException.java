package reservation.domain.exception;

/** A listing paused because the provider's re-verification lapsed takes no new requests. */
public class ListingPausedException extends ReservationRuleViolationException {

    public ListingPausedException(String providerId) {
        super("Provider " + providerId + " is not taking new requests");
    }
}
