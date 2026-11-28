package com.homa.reservation.usecase;

import com.homa.reservation.domain.Reservation;
import com.homa.reservation.usecase.dto.LockRosterRequest;
import com.homa.reservation.usecase.dto.LockRosterResponse;
import com.homa.reservation.usecase.port.in.RosterLocker;
import com.homa.reservation.usecase.port.out.CustomerScheduleRepository;
import com.homa.reservation.usecase.port.out.ProviderScheduleRepository;
import com.homa.reservation.usecase.port.out.ReservationIEPublisher;
import com.homa.reservation.usecase.port.out.ReservationRepository;
import com.homa.reservation.usecase.port.out.TimeSource;
import java.time.Instant;

/**
 * ROS-1, ROS-7, NRS-1 to NRS-3: the Check-in Period has started, so the reservation's roster locks. The reservation
 * decides whether the roster is complete. A complete one is locked and nothing is published, because the customer was
 * told of every change. Otherwise the reservation is cancelled with cause NO_ROSTER, both schedules are released in the
 * same save so the time opens again, and Cancelled is published with the settlement lines for Payment to execute and
 * the customer to be told at once. A reservation that is no longer Reserved, or whose roster is locked already, is
 * refused, so a repeat or a concurrent run changes and publishes nothing.
 */
public final class LockRoster implements RosterLocker {

    private final ReservationRepository reservations;
    private final CalendarRelease calendars;
    private final ReservationIEPublisher publisher;
    private final ReservationEvents events;
    private final TimeSource time;

    public LockRoster(ReservationRepository reservations, ProviderScheduleRepository providerSchedules,
            CustomerScheduleRepository customerSchedules, ReservationIEPublisher publisher, ReservationEvents events,
            TimeSource time) {
        this.reservations = reservations;
        this.calendars = new CalendarRelease(reservations, providerSchedules, customerSchedules);
        this.publisher = publisher;
        this.events = events;
        this.time = time;
    }

    @Override
    public LockRosterResponse lock(LockRosterRequest request) {
        Locked locked = ScheduleRetry.retried(() -> locked(request));
        if (locked.cancelled()) {
            publisher.publish(events.cancelled(locked.reservation()));
        }
        return new LockRosterResponse(locked.reservation().id().value(), locked.cancelled(), locked.at());
    }

    private Locked locked(LockRosterRequest request) {
        Reservation reservation = ReservationParties.load(reservations, request.reservationId());
        Instant now = time.now();
        boolean cancelled = reservation.lockRosterAtCheckIn(now).isPresent();
        if (cancelled) {
            calendars.saveReleased(reservation);
        } else {
            reservations.save(reservation);
        }
        return new Locked(reservation, cancelled, now);
    }

    /** What one attempt decided, once saved. */
    private record Locked(Reservation reservation, boolean cancelled, Instant at) {
    }
}
