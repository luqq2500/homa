package reservation.domain;

/** Why a provider is fined. */
public enum FineReason {
    /** Cancelled within the fee window before the booked start. */
    LATE_CANCELLATION,
    /** Did not check in, or cancelled after the booked start or after checking in. */
    NO_SHOW
}
