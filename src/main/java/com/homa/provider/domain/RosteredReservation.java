package com.homa.provider.domain;

import com.homa.kernel.TimeInterval;
import java.time.Instant;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Provider's replica of one Reserved reservation: its window from the start of the Check-in Period to the job end,
 * and who the provider rostered on it (HLP-9). Reservation's events fill it: {@code ReservationReservedIE} gives the
 * window, {@code RosterChangedIE} the members, and a Completed or Cancelled reservation drops it. Only a member listed
 * on the roster has a window: a reservation nobody is rostered on yet holds nobody.
 *
 * @param window the Check-in Period and the job time as one period, ending at the job end
 * @param roster the team members rostered on it, helpers and supervisors
 */
public record RosteredReservation(ReservationId reservationId, TimeInterval window, Set<TeamMemberId> roster) {

    public RosteredReservation {
        Objects.requireNonNull(reservationId, "reservationId");
        Objects.requireNonNull(window, "window");
        roster = Set.copyOf(roster);
    }

    /** A reservation just Reserved: its window is known and nobody is rostered yet (ROS-1). */
    public static RosteredReservation reserved(ReservationId reservationId, Instant checkInStart, Instant jobEnd) {
        return new RosteredReservation(reservationId, new TimeInterval(checkInStart, jobEnd), Set.of());
    }

    /** The same reservation with this roster in place of the one before: each change sets the whole roster (ROS-1). */
    public RosteredReservation rosteredOn(Collection<TeamMemberId> members) {
        return new RosteredReservation(reservationId, window, Set.copyOf(members));
    }

    /** The member's window on this reservation. Empty unless they are on its roster. */
    public Optional<TimeInterval> windowOf(TeamMemberId member) {
        return roster.contains(member) ? Optional.of(window) : Optional.empty();
    }
}
