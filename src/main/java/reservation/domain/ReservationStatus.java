package reservation.domain;

/** Where a reservation stands in its lifecycle. */
public enum ReservationStatus {
    REQUESTED,
    RESERVED,
    DECLINED,
    EXPIRED,
    CANCELLED,
    DISPUTED,
    COMPLETED
}
