package com.homa.reservation.usecase;

import com.homa.reservation.domain.Reservation;
import com.homa.reservation.usecase.dto.AcceptReservationRequest;
import com.homa.reservation.usecase.dto.AcceptReservationResponse;
import com.homa.reservation.usecase.port.in.ReservationAccepter;
import com.homa.reservation.usecase.port.out.ReservationIEPublisher;
import com.homa.reservation.usecase.port.out.ReservationRepository;
import com.homa.reservation.usecase.port.out.TimeSource;

/** ACC-2: the provider accepts a Requested reservation before its acceptance deadline, and it is Reserved. */
public final class ProviderAcceptReservation implements ReservationAccepter {

    private final ReservationRepository reservations;
    private final ReservationIEPublisher publisher;
    private final ReservationEvents events;
    private final TimeSource time;

    public ProviderAcceptReservation(ReservationRepository reservations, ReservationIEPublisher publisher,
            ReservationEvents events, TimeSource time) {
        this.reservations = reservations;
        this.publisher = publisher;
        this.events = events;
        this.time = time;
    }

    @Override
    public AcceptReservationResponse accept(AcceptReservationRequest request) {
        Reservation reservation = ReservationParties.requireProvider(
                ReservationParties.load(reservations, request.reservationId()), request.providerId());
        reservation.accept(time.now());
        reservations.save(reservation);
        publisher.publish(events.reserved(reservation));
        return new AcceptReservationResponse(reservation.id().value(), reservation.status().name(),
                reservation.reservedAt().orElseThrow());
    }
}
