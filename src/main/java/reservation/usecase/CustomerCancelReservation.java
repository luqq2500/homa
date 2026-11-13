package reservation.usecase;

import java.time.Clock;
import java.time.Instant;
import reservation.domain.Reservation;
import reservation.usecase.dto.CancelByCustomerRequest;
import reservation.usecase.dto.CancelByCustomerResponse;
import reservation.usecase.port.in.CustomerReservationCanceller;
import reservation.usecase.port.out.ReservationIEPublisher;
import reservation.usecase.port.out.ReservationRepository;

public final class CustomerCancelReservation implements CustomerReservationCanceller {

    private final ReservationRepository reservations;
    private final ReservationIEPublisher publisher;
    private final Clock clock;
    private final ReservationAnnouncer announcer;

    public CustomerCancelReservation(ReservationRepository reservations, ReservationIEPublisher publisher, Clock clock) {
        this.reservations = reservations;
        this.publisher = publisher;
        this.clock = clock;
        this.announcer = new ReservationAnnouncer(reservations, publisher);
    }

    @Override
    public CancelByCustomerResponse cancel(CancelByCustomerRequest request) {
        Instant now = clock.instant();
        Reservation reservation =
                OwnedReservations.ofCustomer(reservations, request.customerId(), request.reservationId());
        reservation.cancelByCustomer(now);
        reservations.save(reservation);
        announcer.ended(reservation, now);
        return new CancelByCustomerResponse(reservation.id(), reservation.settlement().orElseThrow().providerReceives());
    }
}
