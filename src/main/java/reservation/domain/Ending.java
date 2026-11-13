package reservation.domain;

/** How a request or reservation came to its end. */
public enum Ending {
    PAYMENT_FAILED,
    DECLINED_BY_PROVIDER,
    /** Declined at once because the provider accepted another booking that takes the same time. Not counted against the provider. */
    DECLINED_FOR_CLASH,
    /** An instant-book request whose time was no longer free when its payment was secured. */
    TIME_NO_LONGER_FREE,
    EXPIRED,
    CANCELLED_BY_CUSTOMER,
    CANCELLED_BY_PROVIDER,
    PROVIDER_NO_SHOW,
    CUSTOMER_NO_SHOW,
    CANCELLED_FOR_SERIOUS_FLAG,
    DISPUTE_WINDOW_ENDED,
    DECIDED_BY_HOMA
}
