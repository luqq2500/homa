package reservation.usecase;

import java.time.Clock;
import java.time.Instant;
import reservation.domain.Reservation;
import reservation.usecase.dto.DeclineRequestRequest;
import reservation.usecase.dto.DeclineRequestResponse;
import reservation.usecase.port.in.ProviderRequestDecliner;
import reservation.usecase.port.out.ReservationIEPublisher;
import reservation.usecase.port.out.ReservationRepository;

public final class ProviderDeclineRequest implements ProviderRequestDecliner {

    private final ReservationRepository reservations;
    private final ReservationIEPublisher publisher;
    private final Clock clock;
    private final ReservationAnnouncer announcer;

    public ProviderDeclineRequest(ReservationRepository reservations, ReservationIEPublisher publisher, Clock clock) {
        this.reservations = reservations;
        this.publisher = publisher;
        this.clock = clock;
        this.announcer = new ReservationAnnouncer(reservations, publisher);
    }

    @Override
    public DeclineRequestResponse decline(DeclineRequestRequest request) {
        Instant now = clock.instant();
        Reservation reservation =
                OwnedReservations.ofProvider(reservations, request.providerId(), request.reservationId());
        reservation.decline(now);
        reservations.save(reservation);
        announcer.ended(reservation, now);
        return new DeclineRequestResponse(reservation.id());
    }
}
