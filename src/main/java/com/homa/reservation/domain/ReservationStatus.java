package com.homa.reservation.domain;

/**
 * The stored statuses (§1 "Reservation statuses"): Requested → Reserved → Completed, or Declined, Expired or
 * Cancelled. Why a reservation was cancelled is a {@link CancellationCause}, not a status. Completed is not final:
 * a Provider No Show confirmed later makes it Cancelled (NSH-6). A checkout hold is not a status (RES-11).
 */
public enum ReservationStatus {
    REQUESTED(true),
    RESERVED(true),
    COMPLETED(false),
    DECLINED(false),
    EXPIRED(false),
    CANCELLED(false);

    private final boolean holdsCalendar;

    ReservationStatus(boolean holdsCalendar) {
        this.holdsCalendar = holdsCalendar;
    }

    /** RES-9, RES-10: only a Requested or Reserved reservation blocks the provider's and the customer's time. */
    public boolean holdsCalendar() {
        return holdsCalendar;
    }
}
