package feedback.domain;

/** Where a reservation stands, as feedback knows it. Only a Completed reservation can be rated. */
public enum ReservationStatus {
    REQUESTED,
    RESERVED,
    DECLINED,
    EXPIRED,
    CANCELLED,
    DISPUTED,
    COMPLETED
}
