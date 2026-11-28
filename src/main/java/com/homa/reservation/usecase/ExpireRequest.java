package com.homa.reservation.usecase;

import com.homa.reservation.domain.Reservation;
import com.homa.reservation.usecase.dto.ExpireRequestRequest;
import com.homa.reservation.usecase.dto.ExpireRequestResponse;
import com.homa.reservation.usecase.port.in.RequestExpirer;
import com.homa.reservation.usecase.port.out.CustomerScheduleRepository;
import com.homa.reservation.usecase.port.out.ProviderScheduleRepository;
import com.homa.reservation.usecase.port.out.ReservationIEPublisher;
import com.homa.reservation.usecase.port.out.ReservationRepository;
import com.homa.reservation.usecase.port.out.TimeSource;

/**
 * REQ-4: a Requested reservation whose acceptance deadline has been reached expires. Both schedules are released in
 * the same save, so the time opens again, and Expired is published for a full refund. The reservation decides
 * whether it can expire; one that was accepted, withdrawn, declined or expired meanwhile is refused, so a repeat or a
 * concurrent run changes and publishes nothing.
 */
public final class ExpireRequest implements RequestExpirer {

    private final ReservationRepository reservations;
    private final CalendarRelease calendars;
    private final ReservationIEPublisher publisher;
    private final ReservationEvents events;
    private final TimeSource time;

    public ExpireRequest(ReservationRepository reservations, ProviderScheduleRepository providerSchedules,
            CustomerScheduleRepository customerSchedules, ReservationIEPublisher publisher, ReservationEvents events,
            TimeSource time) {
        this.reservations = reservations;
        this.calendars = new CalendarRelease(reservations, providerSchedules, customerSchedules);
        this.publisher = publisher;
        this.events = events;
        this.time = time;
    }

    @Override
    public ExpireRequestResponse expire(ExpireRequestRequest request) {
        Reservation expired = ScheduleRetry.retried(() -> expired(request));
        publisher.publish(events.expired(expired));
        return new ExpireRequestResponse(expired.id().value(), expired.status().name(),
                expired.expiredAt().orElseThrow());
    }

    private Reservation expired(ExpireRequestRequest request) {
        Reservation reservation = ReservationParties.load(reservations, request.reservationId());
        reservation.expire(time.now());
        calendars.saveReleased(reservation);
        return reservation;
    }
}
