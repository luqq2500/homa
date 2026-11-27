package com.homa.reservation.usecase.port.out;

import com.homa.reservation.domain.schedule.CustomerSchedule;
import java.util.Optional;

/**
 * Customers' schedules (RES-10), each loaded with the version it was saved at. They are saved only with a
 * reservation, through {@link ReservationRepository#saveWithSchedules}.
 */
public interface CustomerScheduleRepository {

    /** Empty while the customer has never had a reservation. */
    Optional<CustomerSchedule> findByCustomerId(String customerId);
}
