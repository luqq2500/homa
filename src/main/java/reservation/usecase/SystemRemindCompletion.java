package reservation.usecase;

import ie.events.CompletionReminderIE;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import reservation.domain.Reservation;
import reservation.usecase.dto.RemindCompletionResponse;
import reservation.usecase.port.in.SystemCompletionReminder;
import reservation.usecase.port.out.ReservationIEPublisher;
import reservation.usecase.port.out.ReservationRepository;

public final class SystemRemindCompletion implements SystemCompletionReminder {

    private final ReservationRepository reservations;
    private final ReservationIEPublisher publisher;
    private final Clock clock;

    public SystemRemindCompletion(ReservationRepository reservations, ReservationIEPublisher publisher, Clock clock) {
        this.reservations = reservations;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public RemindCompletionResponse remindDue() {
        Instant now = clock.instant();
        List<String> ids = new ArrayList<>();
        for (Reservation reservation : reservations.all()) {
            if (reservation.remindCompletion(now)) {
                reservations.save(reservation);
                publisher.publish(new CompletionReminderIE(reservation.id(), reservation.providerId(), now));
                ids.add(reservation.id());
            }
        }
        return new RemindCompletionResponse(ids);
    }
}
