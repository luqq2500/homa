package reservation.usecase;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import reservation.domain.Reservation;
import reservation.usecase.dto.CompleteAfterDisputeWindowResponse;
import reservation.usecase.port.in.SystemDisputeWindowCloser;
import reservation.usecase.port.out.ReservationIEPublisher;
import reservation.usecase.port.out.ReservationRepository;

public final class SystemCompleteAfterDisputeWindow implements SystemDisputeWindowCloser {

    private final ReservationRepository reservations;
    private final ReservationIEPublisher publisher;
    private final Clock clock;
    private final ReservationAnnouncer announcer;

    public SystemCompleteAfterDisputeWindow(ReservationRepository reservations, ReservationIEPublisher publisher, Clock clock) {
        this.reservations = reservations;
        this.publisher = publisher;
        this.clock = clock;
        this.announcer = new ReservationAnnouncer(reservations, publisher);
    }

    @Override
    public CompleteAfterDisputeWindowResponse completeDue() {
        Instant now = clock.instant();
        List<String> ids = new ArrayList<>();
        for (Reservation reservation : reservations.all()) {
            if (reservation.completeAfterDisputeWindow(now)) {
                reservations.save(reservation);
                announcer.ended(reservation, now);
                ids.add(reservation.id());
            }
        }
        return new CompleteAfterDisputeWindowResponse(ids);
    }
}
