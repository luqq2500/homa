package reservation.usecase;

import ie.events.FaceCheckFailedIE;
import ie.events.ProviderCheckedInIE;
import java.time.Clock;
import java.time.Instant;
import reservation.domain.FaceCheckResult;
import reservation.domain.Reservation;
import reservation.domain.ReservationState;
import reservation.usecase.dto.CheckInRequest;
import reservation.usecase.dto.CheckInResponse;
import reservation.usecase.port.in.ProviderCheckerIn;
import reservation.usecase.port.out.FaceChecker;
import reservation.usecase.port.out.LocationChecker;
import reservation.usecase.port.out.ReservationIEPublisher;
import reservation.usecase.port.out.ReservationRepository;

public final class ProviderCheckIn implements ProviderCheckerIn {

    private final ReservationRepository reservations;
    private final LocationChecker locations;
    private final FaceChecker faces;
    private final ReservationIEPublisher publisher;
    private final Clock clock;

    public ProviderCheckIn(ReservationRepository reservations, LocationChecker locations, FaceChecker faces, ReservationIEPublisher publisher, Clock clock) {
        this.reservations = reservations;
        this.locations = locations;
        this.faces = faces;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public CheckInResponse checkIn(CheckInRequest request) {
        Instant now = clock.instant();
        Reservation reservation =
                OwnedReservations.ofProvider(reservations, request.providerId(), request.reservationId());
        ReservationState booked = reservation.state();
        boolean withinRadius = locations.isWithin(request.deviceLocation(), booked.address().pin(),
                booked.values().checkInRadiusMetres());
        FaceCheckResult result = faces.check(request.memberId(), request.selfie());
        reservation.checkIn(request.memberId(), result, withinRadius, now);
        reservations.save(reservation);
        if (reservation.isCheckedIn()) {
            publisher.publish(new ProviderCheckedInIE(reservation.id(), reservation.customerId(),
                    reservation.providerId(), now, result == FaceCheckResult.UNAVAILABLE, now));
        }
        if (result == FaceCheckResult.FAILED) {
            publisher.publish(new FaceCheckFailedIE(reservation.id(), reservation.customerId(),
                    reservation.providerId(), request.memberId(), now));
        }
        return new CheckInResponse(reservation.id(), reservation.isCheckedIn(), result.name());
    }
}
