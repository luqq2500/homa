package reservation.usecase;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import reservation.domain.Reservation;
import reservation.usecase.dto.ExpireRequestsResponse;
import reservation.usecase.port.in.SystemRequestExpirer;
import reservation.usecase.port.out.ReservationIEPublisher;
import reservation.usecase.port.out.ReservationRepository;

public final class SystemExpireRequests implements SystemRequestExpirer {

    private final ReservationRepository reservations;
    private final ReservationIEPublisher publisher;
    private final Clock clock;
    private final ReservationAnnouncer announcer;

    public SystemExpireRequests(ReservationRepository reservations, ReservationIEPublisher publisher, Clock clock) {
        this.reservations = reservations;
        this.publisher = publisher;
        this.clock = clock;
        this.announcer = new ReservationAnnouncer(reservations, publisher);
    }

    @Override
    public ExpireRequestsResponse expireDue() {
        Instant now = clock.instant();
        List<String> ids = new ArrayList<>();
        for (Reservation reservation : reservations.all()) {
            if (reservation.expire(now)) {
                reservations.save(reservation);
                announcer.ended(reservation, now);
                ids.add(reservation.id());
            }
        }
        return new ExpireRequestsResponse(ids);
    }
}
