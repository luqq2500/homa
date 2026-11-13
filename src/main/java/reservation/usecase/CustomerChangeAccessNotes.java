package reservation.usecase;

import java.time.Clock;
import java.time.Instant;
import reservation.domain.Reservation;
import reservation.usecase.dto.ChangeAccessNotesRequest;
import reservation.usecase.dto.ChangeAccessNotesResponse;
import reservation.usecase.port.in.CustomerAccessNotesChanger;
import reservation.usecase.port.out.ReservationIEPublisher;
import reservation.usecase.port.out.ReservationRepository;

public final class CustomerChangeAccessNotes implements CustomerAccessNotesChanger {

    private final ReservationRepository reservations;
    private final ReservationIEPublisher publisher;
    private final Clock clock;

    public CustomerChangeAccessNotes(ReservationRepository reservations, ReservationIEPublisher publisher, Clock clock) {
        this.reservations = reservations;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public ChangeAccessNotesResponse change(ChangeAccessNotesRequest request) {
        Instant now = clock.instant();
        Reservation reservation =
                OwnedReservations.ofCustomer(reservations, request.customerId(), request.reservationId());
        reservation.changeAccessNotes(request.accessNotes());
        reservations.save(reservation);
        return new ChangeAccessNotesResponse(reservation.id());
    }
}
