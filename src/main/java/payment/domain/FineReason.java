package payment.domain;

/** Why a provider was fined, or owes Homa an amount recovered like a fine. */
public enum FineReason {
    LATE_CANCELLATION,
    NO_SHOW,
    /** The provider covers a card chargeback that Homa decided they were responsible for. */
    CHARGEBACK
}
