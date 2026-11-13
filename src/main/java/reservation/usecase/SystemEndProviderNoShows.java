package reservation.usecase;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import reservation.domain.Reservation;
import reservation.usecase.dto.EndProviderNoShowsResponse;
import reservation.usecase.port.in.SystemProviderNoShowEnder;
import reservation.usecase.port.out.ReservationIEPublisher;
import reservation.usecase.port.out.ReservationRepository;

public final class SystemEndProviderNoShows implements SystemProviderNoShowEnder {

    private final ReservationRepository reservations;
    private final ReservationIEPublisher publisher;
    private final Clock clock;
    private final ReservationAnnouncer announcer;

    public SystemEndProviderNoShows(ReservationRepository reservations, ReservationIEPublisher publisher, Clock clock) {
        this.reservations = reservations;
        this.publisher = publisher;
        this.clock = clock;
        this.announcer = new ReservationAnnouncer(reservations, publisher);
    }

    @Override
    public EndProviderNoShowsResponse endDue() {
        Instant now = clock.instant();
        List<String> ids = new ArrayList<>();
        for (Reservation reservation : reservations.all()) {
            if (reservation.endAsProviderNoShow(now)) {
                reservations.save(reservation);
                announcer.ended(reservation, now);
                ids.add(reservation.id());
            }
        }
        return new EndProviderNoShowsResponse(ids);
    }
}
