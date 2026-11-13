package reservation.usecase;

import ie.events.NoShowChallengedIE;
import java.time.Clock;
import java.time.Instant;
import reservation.domain.Reservation;
import reservation.usecase.dto.ChallengeNoShowRequest;
import reservation.usecase.dto.ChallengeNoShowResponse;
import reservation.usecase.port.in.CustomerNoShowChallenger;
import reservation.usecase.port.out.ReservationIEPublisher;
import reservation.usecase.port.out.ReservationRepository;

public final class CustomerChallengeNoShow implements CustomerNoShowChallenger {

    private final ReservationRepository reservations;
    private final ReservationIEPublisher publisher;
    private final Clock clock;

    public CustomerChallengeNoShow(ReservationRepository reservations, ReservationIEPublisher publisher, Clock clock) {
        this.reservations = reservations;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public ChallengeNoShowResponse challenge(ChallengeNoShowRequest request) {
        Instant now = clock.instant();
        Reservation reservation =
                OwnedReservations.ofCustomer(reservations, request.customerId(), request.reservationId());
        reservation.challengeNoShow(now);
        reservations.save(reservation);
        publisher.publish(new NoShowChallengedIE(reservation.id(), reservation.customerId(),
                reservation.providerId(), reservation.atStake(), now));
        return new ChallengeNoShowResponse(reservation.id(), reservation.status().name());
    }
}
