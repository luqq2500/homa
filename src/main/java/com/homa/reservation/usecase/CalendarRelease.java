package com.homa.reservation.usecase;

import com.homa.reservation.domain.Reservation;
import com.homa.reservation.domain.schedule.CustomerSchedule;
import com.homa.reservation.domain.schedule.ProviderSchedule;
import com.homa.reservation.usecase.port.out.CustomerScheduleRepository;
import com.homa.reservation.usecase.port.out.ProviderScheduleRepository;
import com.homa.reservation.usecase.port.out.ReservationRepository;

/**
 * Saves a reservation that no longer holds the calendar (Withdrawn, Declined, Expired, Cancelled or Completed)
 * together with both parties' schedules, its blocks released in the same save, so the time opens again for others
 * at the moment the reservation ends (RES-9, RES-10).
 */
final class CalendarRelease {

    private final ReservationRepository reservations;
    private final ProviderScheduleRepository providerSchedules;
    private final CustomerScheduleRepository customerSchedules;

    CalendarRelease(ReservationRepository reservations, ProviderScheduleRepository providerSchedules,
            CustomerScheduleRepository customerSchedules) {
        this.reservations = reservations;
        this.providerSchedules = providerSchedules;
        this.customerSchedules = customerSchedules;
    }

    void saveReleased(Reservation ended) {
        // Both schedules were saved with the reservation when its payment was confirmed (RES-9, RES-10).
        ProviderSchedule providerSchedule =
                providerSchedules.findByProviderId(ended.booking().providerId()).orElseThrow();
        CustomerSchedule customerSchedule =
                customerSchedules.findByCustomerId(ended.booking().customerId()).orElseThrow();
        providerSchedule.release(ended);
        customerSchedule.release(ended);
        reservations.saveWithSchedules(ended, providerSchedule, customerSchedule);
    }
}
