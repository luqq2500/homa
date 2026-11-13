package reservation.usecase;

import java.time.Clock;
import java.time.Instant;
import kernel.Money;
import reservation.domain.Fine;
import reservation.domain.Reservation;
import reservation.usecase.dto.CancelByProviderRequest;
import reservation.usecase.dto.CancelByProviderResponse;
import reservation.usecase.port.in.ProviderReservationCanceller;
import reservation.usecase.port.out.ReservationIEPublisher;
import reservation.usecase.port.out.ReservationRepository;

public final class ProviderCancelReservation implements ProviderReservationCanceller {

    private final ReservationRepository reservations;
    private final ReservationIEPublisher publisher;
    private final Clock clock;
    private final ReservationAnnouncer announcer;

    public ProviderCancelReservation(ReservationRepository reservations, ReservationIEPublisher publisher, Clock clock) {
        this.reservations = reservations;
        this.publisher = publisher;
        this.clock = clock;
        this.announcer = new ReservationAnnouncer(reservations, publisher);
    }

    @Override
    public CancelByProviderResponse cancel(CancelByProviderRequest request) {
        Instant now = clock.instant();
        Reservation reservation =
                OwnedReservations.ofProvider(reservations, request.providerId(), request.reservationId());
        reservation.cancelByProvider(now);
        reservations.save(reservation);
        announcer.ended(reservation, now);
        return new CancelByProviderResponse(reservation.id(), reservation.fine().map(Fine::amount).orElse(Money.ZERO));
    }
}
