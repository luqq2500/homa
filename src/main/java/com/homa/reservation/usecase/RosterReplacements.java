package com.homa.reservation.usecase;

import com.homa.reservation.domain.Reservation;
import com.homa.reservation.usecase.port.out.ReservationIEPublisher;
import com.homa.reservation.usecase.port.out.ReservationRepository;
import com.homa.reservation.usecase.port.out.TimeSource;
import java.util.Optional;

/**
 * ROS-5: a rostered member stopped being active, and every upcoming roster with them is flagged to be fixed, each
 * saved on its own and the provider told. Shared by the handlers of every event that ends a member's activity:
 * deactivation now, suspension later. The reservation decides whether its roster needs fixing; one already past the
 * point where the roster can change, or already flagged, is left as it is and nothing is published, so a repeat
 * changes and publishes nothing.
 */
public final class RosterReplacements {

    private final ReservationRepository reservations;
    private final ReservationIEPublisher publisher;
    private final ReservationEvents events;
    private final TimeSource time;

    public RosterReplacements(ReservationRepository reservations, ReservationIEPublisher publisher,
            ReservationEvents events, TimeSource time) {
        this.reservations = reservations;
        this.publisher = publisher;
        this.events = events;
        this.time = time;
    }

    public void flagRostersWith(String memberId) {
        for (Reservation upcoming : reservations.findReservedRosteredWith(memberId, time.now())) {
            ScheduleRetry.retried(() -> flagged(upcoming.id().value(), memberId))
                    .ifPresent(flagged -> publisher.publish(events.rosterNeedsReplacement(flagged, memberId)));
        }
    }

    /** One attempt, from a fresh load: the reservation, once its roster is flagged and saved. */
    private Optional<Reservation> flagged(String reservationId, String memberId) {
        Reservation reservation = ReservationParties.load(reservations, reservationId);
        if (!reservation.rosterMemberStoppedBeingActive(memberId, time.now())) {
            return Optional.empty();
        }
        reservations.save(reservation);
        return Optional.of(reservation);
    }
}
