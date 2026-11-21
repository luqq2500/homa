package com.homa.reservation.domain;

/**
 * The stored statuses (§1 "Reservation statuses"): Requested → Reserved → Completed, or Withdrawn, Declined, Expired
 * or Cancelled. Withdrawn is a status of its own, not a cancellation: no fee and no reliability count (REQ-2,
 * RTG-10). Why a reservation was cancelled is a {@link CancellationCause}, not a status. Completed is not final: a
 * Provider No Show confirmed later makes it Cancelled (NSH-6). A checkout is not a status, and holds nothing (RES-11).
 */
public enum ReservationStatus {
    REQUESTED(true),
    RESERVED(true),
    COMPLETED(false),
    WITHDRAWN(false),
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
