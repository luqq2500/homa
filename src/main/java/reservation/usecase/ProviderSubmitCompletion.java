package reservation.usecase;

import ie.events.CompletionSubmittedIE;
import java.time.Clock;
import java.time.Instant;
import reservation.domain.Reservation;
import reservation.usecase.dto.SubmitCompletionRequest;
import reservation.usecase.dto.SubmitCompletionResponse;
import reservation.usecase.port.in.ProviderCompletionSubmitter;
import reservation.usecase.port.out.ReservationIEPublisher;
import reservation.usecase.port.out.ReservationRepository;

public final class ProviderSubmitCompletion implements ProviderCompletionSubmitter {

    private final ReservationRepository reservations;
    private final ReservationIEPublisher publisher;
    private final Clock clock;

    public ProviderSubmitCompletion(ReservationRepository reservations, ReservationIEPublisher publisher, Clock clock) {
        this.reservations = reservations;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public SubmitCompletionResponse submit(SubmitCompletionRequest request) {
        Instant now = clock.instant();
        Reservation reservation =
                OwnedReservations.ofProvider(reservations, request.providerId(), request.reservationId());
        reservation.submitCompletion(request.evidence(), now);
        reservations.save(reservation);
        Instant windowEndsAt = reservation.disputeWindowEndsAt().orElseThrow();
        publisher.publish(new CompletionSubmittedIE(reservation.id(), reservation.customerId(),
                reservation.providerId(), request.evidence(), windowEndsAt, now));
        return new SubmitCompletionResponse(reservation.id(), windowEndsAt);
    }
}
