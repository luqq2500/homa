package com.homa.reservation.usecase;

import com.homa.reservation.domain.Reservation;
import com.homa.reservation.usecase.dto.DeclineReservationRequest;
import com.homa.reservation.usecase.dto.DeclineReservationResponse;
import com.homa.reservation.usecase.port.in.ReservationDecliner;
import com.homa.reservation.usecase.port.out.CustomerScheduleRepository;
import com.homa.reservation.usecase.port.out.ProviderScheduleRepository;
import com.homa.reservation.usecase.port.out.ReservationIEPublisher;
import com.homa.reservation.usecase.port.out.ReservationRepository;
import com.homa.reservation.usecase.port.out.TimeSource;

/**
 * REQ-3: the provider declines a Requested reservation. Both schedules are released in the same save, so the time
 * opens again, and Declined is published for a full refund. A decline racing an acceptance or a withdrawal resolves
 * to whichever is saved first: the loser reloads and is refused (REQ-5).
 */
public final class ProviderDeclineReservation implements ReservationDecliner {

    private final ReservationRepository reservations;
    private final CalendarRelease calendars;
    private final ReservationIEPublisher publisher;
    private final ReservationEvents events;
    private final TimeSource time;

    public ProviderDeclineReservation(ReservationRepository reservations,
            ProviderScheduleRepository providerSchedules, CustomerScheduleRepository customerSchedules,
            ReservationIEPublisher publisher, ReservationEvents events, TimeSource time) {
        this.reservations = reservations;
        this.calendars = new CalendarRelease(reservations, providerSchedules, customerSchedules);
        this.publisher = publisher;
        this.events = events;
        this.time = time;
    }

    @Override
    public DeclineReservationResponse decline(DeclineReservationRequest request) {
        Reservation declined = ScheduleRetry.retried(() -> declined(request));
        publisher.publish(events.declined(declined));
        return new DeclineReservationResponse(declined.id().value(), declined.status().name(),
                declined.declinedAt().orElseThrow());
    }

    private Reservation declined(DeclineReservationRequest request) {
        Reservation reservation = ReservationParties.requireProvider(
                ReservationParties.load(reservations, request.reservationId()), request.providerId());
        reservation.decline(time.now());
        calendars.saveReleased(reservation);
        return reservation;
    }
}
