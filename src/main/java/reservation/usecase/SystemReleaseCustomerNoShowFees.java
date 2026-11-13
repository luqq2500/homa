package reservation.usecase;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import reservation.domain.Reservation;
import reservation.usecase.dto.ReleaseCustomerNoShowFeesResponse;
import reservation.usecase.port.in.SystemCustomerNoShowFeeReleaser;
import reservation.usecase.port.out.ReservationIEPublisher;
import reservation.usecase.port.out.ReservationRepository;

public final class SystemReleaseCustomerNoShowFees implements SystemCustomerNoShowFeeReleaser {

    private final ReservationRepository reservations;
    private final ReservationIEPublisher publisher;
    private final Clock clock;
    private final ReservationAnnouncer announcer;

    public SystemReleaseCustomerNoShowFees(ReservationRepository reservations, ReservationIEPublisher publisher, Clock clock) {
        this.reservations = reservations;
        this.publisher = publisher;
        this.clock = clock;
        this.announcer = new ReservationAnnouncer(reservations, publisher);
    }

    @Override
    public ReleaseCustomerNoShowFeesResponse releaseDue() {
        Instant now = clock.instant();
        List<String> ids = new ArrayList<>();
        for (Reservation reservation : reservations.all()) {
            if (reservation.releaseCustomerNoShowFee(now)) {
                reservations.save(reservation);
                announcer.settled(reservation, now);
                ids.add(reservation.id());
            }
        }
        return new ReleaseCustomerNoShowFeesResponse(ids);
    }
}
