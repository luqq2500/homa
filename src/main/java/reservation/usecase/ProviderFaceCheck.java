package reservation.usecase;

import ie.events.FaceCheckFailedIE;
import java.time.Clock;
import java.time.Instant;
import reservation.domain.FaceCheckResult;
import reservation.domain.Reservation;
import reservation.usecase.dto.FaceCheckRequest;
import reservation.usecase.dto.FaceCheckResponse;
import reservation.usecase.port.in.ProviderFaceChecker;
import reservation.usecase.port.out.FaceChecker;
import reservation.usecase.port.out.ReservationIEPublisher;
import reservation.usecase.port.out.ReservationRepository;

public final class ProviderFaceCheck implements ProviderFaceChecker {

    private final ReservationRepository reservations;
    private final FaceChecker faces;
    private final ReservationIEPublisher publisher;
    private final Clock clock;

    public ProviderFaceCheck(ReservationRepository reservations, FaceChecker faces, ReservationIEPublisher publisher, Clock clock) {
        this.reservations = reservations;
        this.faces = faces;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public FaceCheckResponse check(FaceCheckRequest request) {
        Instant now = clock.instant();
        Reservation reservation =
                OwnedReservations.ofProvider(reservations, request.providerId(), request.reservationId());
        FaceCheckResult result = faces.check(request.memberId(), request.selfie());
        reservation.faceCheck(request.memberId(), result, now);
        reservations.save(reservation);
        if (result == FaceCheckResult.FAILED) {
            publisher.publish(new FaceCheckFailedIE(reservation.id(), reservation.customerId(),
                    reservation.providerId(), request.memberId(), now));
        }
        return new FaceCheckResponse(reservation.id(), result.name(), reservation.canWork(request.memberId()));
    }
}
