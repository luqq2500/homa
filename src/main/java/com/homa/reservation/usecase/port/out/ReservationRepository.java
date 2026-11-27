package com.homa.reservation.usecase.port.out;

import com.homa.reservation.domain.Reservation;
import com.homa.reservation.domain.ReservationId;
import com.homa.reservation.domain.schedule.CustomerSchedule;
import com.homa.reservation.domain.schedule.ProviderSchedule;
import com.homa.reservation.usecase.exception.ScheduleChangedException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Reservations, and the one save that changes a reservation together with both parties' schedules. */
public interface ReservationRepository {

    Optional<Reservation> findById(ReservationId id);

    /** The reservations the party is the customer or the provider of, earliest job start first. */
    List<Reservation> findByParty(String partyId);

    /** Reserved reservations whose job ended by this moment and that aren't marked Completed (JOB-5). */
    List<Reservation> findReservedEndedBy(Instant now);

    /** Saves a reservation whose schedules did not change. */
    void save(Reservation reservation);

    /**
     * Saves the reservation with the provider's and the customer's schedules, all or nothing. Each schedule was
     * loaded at its {@code version()}, or is new at version 0; if either was saved by someone else since, nothing is
     * saved and {@link ScheduleChangedException} is thrown, so the caller reloads and asks again (RES-9 to RES-11).
     */
    void saveWithSchedules(Reservation reservation, ProviderSchedule providerSchedule,
            CustomerSchedule customerSchedule);
}
