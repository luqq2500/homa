package reservation.domain;

/** The reservation lifecycle. */
public enum ReservationStatus {
    /** The customer has requested; it waits for payment to be secured, then for the provider's answer. */
    REQUESTED,
    /** The provider accepted. It stays Reserved through the job and the dispute window. */
    RESERVED,
    /** The provider declined, or accepted another booking that takes the same time. */
    DECLINED,
    /** The provider did not respond in time. */
    EXPIRED,
    /** Cancelled by the customer or the provider, ended as a no-show, or a request whose payment failed. */
    CANCELLED,
    /** Waiting for Homa's review. */
    DISPUTED,
    /** The final status of a job. */
    COMPLETED
}
