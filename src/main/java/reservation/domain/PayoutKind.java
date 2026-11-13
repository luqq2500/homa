package reservation.domain;

/** What a payment to the provider is for. Fines come off payouts for completed jobs only. */
public enum PayoutKind {
    JOB,
    CANCELLATION_FEE,
    CUSTOMER_NO_SHOW_FEE
}
