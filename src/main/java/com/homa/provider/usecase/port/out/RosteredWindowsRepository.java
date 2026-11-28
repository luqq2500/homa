package com.homa.provider.usecase.port.out;

import com.homa.provider.domain.ReservationId;
import com.homa.provider.domain.RosteredReservation;
import com.homa.provider.domain.RosteredWindows;
import com.homa.provider.domain.TeamMemberId;
import java.util.Optional;

/**
 * Provider's replica of Reservation's Reserved reservations, with the window of each and who is rostered on it
 * (HLP-9). Handlers keep it from Reservation's events; removing a member asks it for their windows.
 */
public interface RosteredWindowsRepository {

    Optional<RosteredReservation> findByReservationId(ReservationId reservationId);

    /** Inserts a new reservation or replaces the stored one with the same id. */
    void save(RosteredReservation reservation);

    /** Drops the reservation. Does nothing when it isn't stored. */
    void delete(ReservationId reservationId);

    /** The windows of every reservation the member is on the roster of; none while they are rostered on nothing. */
    RosteredWindows windowsOf(TeamMemberId member);
}
