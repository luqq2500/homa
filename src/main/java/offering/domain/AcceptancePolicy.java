package offering.domain;

/** How a provider accepts reservations. */
public enum AcceptancePolicy {
    /** The provider accepts or declines each request by the response deadline. The default. */
    REQUEST_AND_ACCEPT,
    /** A request is accepted automatically the moment its payment is secured. */
    INSTANT_BOOK
}
