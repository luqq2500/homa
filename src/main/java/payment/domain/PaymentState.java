package payment.domain;

/** Where the customer's money stands, separate from the reservation status. */
public enum PaymentState {
    /** Card only: checked, nothing charged yet. */
    AUTHORISED,
    /** Card only: the request ended before acceptance, so the authorisation was let go and nothing was charged. */
    AUTHORISATION_RELEASED,
    /** Paid and held by Homa. */
    HELD,
    /** Settled with at least part paid to the provider and Homa. */
    RELEASED,
    /** Settled with everything returned to the customer. */
    REFUNDED
}
