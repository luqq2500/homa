package com.homa.reservation.usecase;

import com.homa.reservation.domain.Cancellation;
import com.homa.reservation.domain.Reservation;
import com.homa.reservation.usecase.dto.ProviderCancelReservationRequest;
import com.homa.reservation.usecase.dto.ProviderCancelReservationResponse;
import com.homa.reservation.usecase.port.in.ProviderReservationCanceller;
import com.homa.reservation.usecase.port.out.CustomerScheduleRepository;
import com.homa.reservation.usecase.port.out.ProviderScheduleRepository;
import com.homa.reservation.usecase.port.out.ReservationIEPublisher;
import com.homa.reservation.usecase.port.out.ReservationRepository;
import com.homa.reservation.usecase.port.out.TimeSource;

/**
 * PCX-1, PCX-2, LMC-3: the provider cancels a Reserved reservation. The reservation works out the cause, the fine and
 * the customer's refund and compensation, both schedules are released in the same save so the time opens again, and
 * Cancelled is published with the settlement lines for Payment to execute. A cancellation racing another change
 * resolves to whichever is saved first: the loser reloads and is refused.
 */
public final class ProviderCancelReservation implements ProviderReservationCanceller {

    private final ReservationRepository reservations;
    private final CalendarRelease calendars;
    private final ReservationIEPublisher publisher;
    private final ReservationEvents events;
    private final TimeSource time;

    public ProviderCancelReservation(ReservationRepository reservations,
            ProviderScheduleRepository providerSchedules, CustomerScheduleRepository customerSchedules,
            ReservationIEPublisher publisher, ReservationEvents events, TimeSource time) {
        this.reservations = reservations;
        this.calendars = new CalendarRelease(reservations, providerSchedules, customerSchedules);
        this.publisher = publisher;
        this.events = events;
        this.time = time;
    }

    @Override
    public ProviderCancelReservationResponse cancel(ProviderCancelReservationRequest request) {
        Reservation cancelled = ScheduleRetry.retried(() -> cancelled(request));
        publisher.publish(events.cancelled(cancelled));
        Cancellation cancellation = cancelled.cancellation().orElseThrow();
        return new ProviderCancelReservationResponse(cancelled.id().value(), cancelled.status().name(),
                cancellation.cause().name(), cancellation.at(), cancellation.settlement().providerFine(),
                cancellation.settlement().customerRefund(), cancellation.settlement().compensation());
    }

    private Reservation cancelled(ProviderCancelReservationRequest request) {
        Reservation reservation = ReservationParties.requireProvider(
                ReservationParties.load(reservations, request.reservationId()), request.providerId());
        reservation.cancelByProvider(time.now());
        calendars.saveReleased(reservation);
        return reservation;
    }
}
