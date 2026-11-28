package com.homa.rating.domain;

import com.homa.kernel.BusinessCalendar;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * One dated event that reliability counts (RTG-10): the reservation it happened on, and for a member who left, which
 * member. It counts for the 12 months after it happened, on the Kuala Lumpur calendar, and is kept for the same time
 * (DAT-4): {@link #stopsCountingAt()} is the one date both use.
 *
 * @param memberId the roster member who left, for {@link IncidentKind#MEMBER_LEFT}; null for every other kind
 */
public record Incident(IncidentKind kind, ReservationId reservationId, String memberId, Instant occurredAt) {

    public Incident {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(reservationId, "reservationId");
        Objects.requireNonNull(occurredAt, "occurredAt");
        if (kind == IncidentKind.MEMBER_LEFT) {
            Ids.requireText(memberId, "Member id");
        } else if (memberId != null) {
            throw new IllegalArgumentException("Only a member who left names a member, not a " + kind);
        }
    }

    /** A cancellation, no roster or no show on a reservation. */
    public static Incident of(IncidentKind kind, ReservationId reservationId, Instant occurredAt) {
        return new Incident(kind, reservationId, null, occurredAt);
    }

    /** A roster member who left the job on a reservation (RPT-8). */
    public static Incident memberLeft(ReservationId reservationId, String memberId, Instant leftAt) {
        return new Incident(IncidentKind.MEMBER_LEFT, reservationId, memberId, leftAt);
    }

    public Optional<String> member() {
        return Optional.ofNullable(memberId);
    }

    /** The moment it stops counting, and may be deleted: 12 months after it happened (RTG-10, DAT-4). */
    public Instant stopsCountingAt() {
        return BusinessCalendar.plusMonths(occurredAt, RatingValues.RELIABILITY_PERIOD_MONTHS);
    }

    /** True while it is inside the last 12 months: an incident exactly 12 months old no longer counts. */
    public boolean countsAt(Instant now) {
        return now.isBefore(stopsCountingAt());
    }

    /** True when both are the same thing happening on the same reservation, whenever it was logged. */
    boolean isSameAs(Incident other) {
        return kind == other.kind && reservationId.equals(other.reservationId)
                && Objects.equals(memberId, other.memberId);
    }
}
