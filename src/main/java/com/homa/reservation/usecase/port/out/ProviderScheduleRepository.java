package com.homa.reservation.usecase.port.out;

import com.homa.reservation.domain.schedule.ProviderSchedule;
import java.util.Optional;

/**
 * Providers' schedules (RES-9), each loaded with the version it was saved at. They are saved only with a reservation,
 * through {@link ReservationRepository#saveWithSchedules}.
 */
public interface ProviderScheduleRepository {

    /** Empty while the provider has never had a reservation. */
    Optional<ProviderSchedule> findByProviderId(String providerId);
}
