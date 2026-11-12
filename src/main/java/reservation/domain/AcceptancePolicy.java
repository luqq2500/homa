package reservation.domain;

/** How the provider's reservations are accepted, as the reservation copied it at request time. */
public enum AcceptancePolicy {
    /** The provider accepts or declines the request by the response deadline. */
    REQUEST_AND_ACCEPT,
    /** The request is accepted automatically the moment its payment is secured; the provider can't decline it. */
    INSTANT_BOOK
}
