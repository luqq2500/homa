package com.homa.reservation.usecase;

import com.homa.reservation.domain.Cancellation;
import com.homa.reservation.domain.Reservation;
import com.homa.reservation.usecase.dto.CustomerCancelReservationRequest;
import com.homa.reservation.usecase.dto.CustomerCancelReservationResponse;
import com.homa.reservation.usecase.port.in.CustomerReservationCanceller;
import com.homa.reservation.usecase.port.out.CustomerScheduleRepository;
import com.homa.reservation.usecase.port.out.ProviderScheduleRepository;
import com.homa.reservation.usecase.port.out.ReservationIEPublisher;
import com.homa.reservation.usecase.port.out.ReservationRepository;
import com.homa.reservation.usecase.port.out.TimeSource;

/**
 * REQ-1, CCX-1 to CCX-6, LMC-1, LMC-2: the customer cancels a Reserved reservation (a Requested one is withdrawn
 * instead). The reservation works out the cause and the settlement, both schedules are released in the same save so
 * the time opens again, and Cancelled is published with the settlement lines for Payment to execute. A cancellation
 * racing another change resolves to whichever is saved first: the loser reloads and is refused.
 */
public final class CustomerCancelReservation implements CustomerReservationCanceller {

    private final ReservationRepository reservations;
    private final CalendarRelease calendars;
    private final ReservationIEPublisher publisher;
    private final ReservationEvents events;
    private final TimeSource time;

    public CustomerCancelReservation(ReservationRepository reservations,
            ProviderScheduleRepository providerSchedules, CustomerScheduleRepository customerSchedules,
            ReservationIEPublisher publisher, ReservationEvents events, TimeSource time) {
        this.reservations = reservations;
        this.calendars = new CalendarRelease(reservations, providerSchedules, customerSchedules);
        this.publisher = publisher;
        this.events = events;
        this.time = time;
    }

    @Override
    public CustomerCancelReservationResponse cancel(CustomerCancelReservationRequest request) {
        Reservation cancelled = ScheduleRetry.retried(() -> cancelled(request));
        publisher.publish(events.cancelled(cancelled));
        Cancellation cancellation = cancelled.cancellation().orElseThrow();
        return new CustomerCancelReservationResponse(cancelled.id().value(), cancelled.status().name(),
                cancellation.cause().name(), cancellation.at(), cancellation.settlement().customerFee(),
                cancellation.settlement().customerRefund());
    }

    private Reservation cancelled(CustomerCancelReservationRequest request) {
        Reservation reservation = ReservationParties.requireCustomer(
                ReservationParties.load(reservations, request.reservationId()), request.customerId());
        reservation.cancelByCustomer(time.now());
        calendars.saveReleased(reservation);
        return reservation;
    }
}
