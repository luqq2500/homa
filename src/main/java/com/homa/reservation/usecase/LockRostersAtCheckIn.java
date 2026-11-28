package com.homa.reservation.usecase;

import com.homa.reservation.domain.Reservation;
import com.homa.reservation.domain.exception.ReservationDomainException;
import com.homa.reservation.usecase.dto.LockRosterRequest;
import com.homa.reservation.usecase.dto.LockRosterResponse;
import com.homa.reservation.usecase.dto.LockRostersAtCheckInRequest;
import com.homa.reservation.usecase.dto.LockRostersAtCheckInResponse;
import com.homa.reservation.usecase.exception.ScheduleChangedException;
import com.homa.reservation.usecase.port.in.CheckInRosterLocker;
import com.homa.reservation.usecase.port.in.RosterLocker;
import com.homa.reservation.usecase.port.out.ReservationChangedException;
import com.homa.reservation.usecase.port.out.ReservationRepository;
import com.homa.reservation.usecase.port.out.TimeSource;
import java.util.ArrayList;
import java.util.List;

/**
 * ROS-1, ROS-7, NRS-1 to NRS-3, run by a scheduler every minute: every Reserved reservation whose Check-in Period has
 * started and whose roster isn't locked is locked, or cancelled for having no complete roster, each through
 * {@link RosterLocker} so each is its own unit of work. The run is safe to repeat and to run in several tasks at once:
 * a reservation that no longer needs locking, or that another run, the customer or the provider changed first, is
 * skipped, and it is locked or cancelled, and published, exactly once, by whoever saves it first. Any other failure
 * ends this run and the next minute's run picks the rest up.
 */
public final class LockRostersAtCheckIn implements CheckInRosterLocker {

    private final ReservationRepository reservations;
    private final RosterLocker locker;
    private final TimeSource time;

    public LockRostersAtCheckIn(ReservationRepository reservations, RosterLocker locker, TimeSource time) {
        this.reservations = reservations;
        this.locker = locker;
        this.time = time;
    }

    @Override
    public LockRostersAtCheckInResponse lockRosters(LockRostersAtCheckInRequest request) {
        List<String> locked = new ArrayList<>();
        List<String> cancelled = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        for (Reservation due : reservations.findReservedToLockRosterBy(time.now())) {
            String id = due.id().value();
            try {
                LockRosterResponse done = locker.lock(new LockRosterRequest(id));
                (done.cancelled() ? cancelled : locked).add(id);
            } catch (ReservationDomainException | ReservationChangedException | ScheduleChangedException decided) {
                skipped.add(id);
            }
        }
        return new LockRostersAtCheckInResponse(locked, cancelled, skipped);
    }
}
