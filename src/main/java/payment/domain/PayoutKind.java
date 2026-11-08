package payment.domain;

/** What a payout pays the provider for. Fines are deducted only from payouts for completed jobs. */
public enum PayoutKind {
    COMPLETED_JOB,
    CANCELLATION_FEE,
    CUSTOMER_NO_SHOW_FEE
}
