package com.homa.reservation.usecase.port.out;

import com.homa.reservation.domain.Reservation;
import com.homa.reservation.domain.ReservationId;
import com.homa.reservation.domain.schedule.CustomerSchedule;
import com.homa.reservation.domain.schedule.ProviderSchedule;
import com.homa.reservation.usecase.exception.ScheduleChangedException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Reservations, and the one save that changes a reservation together with both parties' schedules.
 *
 * <p>Every save is optimistic. A reservation is loaded at its {@code version()} (a new one is at version 0), and a
 * save moves the stored version on; if the stored version is no longer the one the reservation was loaded at, or a
 * new reservation already exists, nothing is saved and {@link ReservationChangedException} is thrown. That is how a
 * withdrawal and an acceptance racing on the same Requested reservation resolve to whichever is saved first (REQ-5).
 */
public interface ReservationRepository {

    Optional<Reservation> findById(ReservationId id);

    /** The reservations the party is the customer or the provider of, earliest job start first. */
    List<Reservation> findByParty(String partyId);

    /** Reserved reservations whose job ended by this moment and that aren't marked Completed (JOB-5). */
    List<Reservation> findReservedEndedBy(Instant now);

    /**
     * Requested reservations whose acceptance deadline is at or before this moment, so they can no longer be accepted
     * and are due to expire (REQ-4, ACC-3). Several scheduler tasks can ask at once; each reservation is then
     * expired in its own save, and the loser of a race gets {@link ReservationChangedException}.
     */
    List<Reservation> findRequestedPastDeadline(Instant now);

    /**
     * Reserved reservations whose Check-in Period has started by this moment and whose job hasn't ended, with a
     * roster that isn't locked yet (ROS-1, ROS-7): each is due to lock, or to be cancelled for having no complete
     * roster. Several scheduler tasks can ask at once; each reservation is then saved on its own and the loser of a
     * race gets {@link ReservationChangedException}.
     */
    List<Reservation> findReservedToLockRosterBy(Instant now);

    /**
     * Reserved reservations whose roster has this member and whose Check-in Period hasn't started by this moment, so
     * the member can still be replaced (ROS-5). Several handlers can ask at once; each reservation is then saved on
     * its own and the loser of a race gets {@link ReservationChangedException}.
     */
    List<Reservation> findReservedRosteredWith(String memberId, Instant now);

    /**
     * Saves a reservation whose schedules did not change.
     *
     * @throws ReservationChangedException when the reservation was saved by someone else since it was loaded
     */
    void save(Reservation reservation);

    /**
     * Saves the reservation with the provider's and the customer's schedules, all or nothing. Each schedule was
     * loaded at its {@code version()}, or is new at version 0; if either was saved by someone else since, nothing is
     * saved and {@link ScheduleChangedException} is thrown, so the caller reloads and asks again (RES-9 to RES-11).
     * The reservation is checked the same way as in {@link #save}.
     *
     * @throws ReservationChangedException when the reservation was saved by someone else since it was loaded
     */
    void saveWithSchedules(Reservation reservation, ProviderSchedule providerSchedule,
            CustomerSchedule customerSchedule);
}
