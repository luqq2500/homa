package reservation.domain;

/** Why a provider is fined. Both count in the provider's cancellation count. */
public enum FineReason {
    /** Cancelled within the fee window before the booked start. */
    LATE_CANCELLATION,
    /** No check-in by the end of the check-in period, or a cancellation after the booked start or after check-in. */
    NO_SHOW
}
