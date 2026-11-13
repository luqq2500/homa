package reservation.usecase;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import reservation.domain.Reservation;
import reservation.usecase.dto.CancelUndecidedFlaggedResponse;
import reservation.usecase.port.in.SystemUndecidedFlagCanceller;
import reservation.usecase.port.out.ReservationIEPublisher;
import reservation.usecase.port.out.ReservationRepository;

public final class SystemCancelUndecidedFlagged implements SystemUndecidedFlagCanceller {

    private final ReservationRepository reservations;
    private final ReservationIEPublisher publisher;
    private final Clock clock;
    private final ReservationAnnouncer announcer;

    public SystemCancelUndecidedFlagged(ReservationRepository reservations, ReservationIEPublisher publisher, Clock clock) {
        this.reservations = reservations;
        this.publisher = publisher;
        this.clock = clock;
        this.announcer = new ReservationAnnouncer(reservations, publisher);
    }

    @Override
    public CancelUndecidedFlaggedResponse cancelDue() {
        Instant now = clock.instant();
        List<String> ids = new ArrayList<>();
        for (Reservation reservation : reservations.all()) {
            if (reservation.cancelUndecidedFlagAtStart(now)) {
                reservations.save(reservation);
                announcer.ended(reservation, now);
                ids.add(reservation.id());
            }
        }
        return new CancelUndecidedFlaggedResponse(ids);
    }
}
