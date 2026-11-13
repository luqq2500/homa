package reservation.usecase;

import ie.events.CompletionDeadlineMissedIE;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import reservation.domain.Reservation;
import reservation.usecase.dto.DisputeMissedCompletionsResponse;
import reservation.usecase.port.in.SystemMissedCompletionDisputer;
import reservation.usecase.port.out.ReservationIEPublisher;
import reservation.usecase.port.out.ReservationRepository;

public final class SystemDisputeMissedCompletions implements SystemMissedCompletionDisputer {

    private final ReservationRepository reservations;
    private final ReservationIEPublisher publisher;
    private final Clock clock;

    public SystemDisputeMissedCompletions(ReservationRepository reservations, ReservationIEPublisher publisher, Clock clock) {
        this.reservations = reservations;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public DisputeMissedCompletionsResponse disputeDue() {
        Instant now = clock.instant();
        List<String> ids = new ArrayList<>();
        for (Reservation reservation : reservations.all()) {
            if (reservation.missCompletionDeadline(now)) {
                reservations.save(reservation);
                publisher.publish(new CompletionDeadlineMissedIE(reservation.id(), reservation.customerId(),
                        reservation.providerId(), reservation.atStake(), now));
                ids.add(reservation.id());
            }
        }
        return new DisputeMissedCompletionsResponse(ids);
    }
}
