package reservation.domain;

/** Why a reservation ended, which decides who gets how much. */
public enum Ending {
    /** The customer cancelled before the provider answered: free on every tier. */
    CUSTOMER_CANCELLED_REQUEST,
    /** The customer cancelled a reserved booking before the fee window, or on the Flexible tier. */
    CUSTOMER_CANCELLED_FREE,
    /** The customer cancelled a reserved booking inside the fee window. */
    CUSTOMER_CANCELLED_IN_FEE_WINDOW,
    /** The customer cancelled after the provider checked in: treated as a customer no-show. */
    CUSTOMER_CANCELLED_AFTER_CHECK_IN,
    /** The provider reported a customer no-show. */
    CUSTOMER_NO_SHOW,
    /** The provider cancelled a reserved booking. */
    PROVIDER_CANCELLED,
    /** The provider did not check in by the end of the check-in period. */
    PROVIDER_NO_SHOW,
    /** The dispute window closed with no report. */
    COMPLETED,
    /** Homa decided a disputed reservation. */
    DECIDED_BY_HOMA,
    /** The KYC provider seriously flagged the provider's business: Homa disputed and cancelled it with a full refund. */
    PROVIDER_FLAGGED
}
