package reservation.usecase;

import ie.events.TeamNamingReminderIE;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import reservation.domain.Reservation;
import reservation.usecase.dto.RemindTeamNamingResponse;
import reservation.usecase.port.in.SystemTeamNamingReminder;
import reservation.usecase.port.out.ReservationIEPublisher;
import reservation.usecase.port.out.ReservationRepository;

public final class SystemRemindTeamNaming implements SystemTeamNamingReminder {

    private final ReservationRepository reservations;
    private final ReservationIEPublisher publisher;
    private final Clock clock;

    public SystemRemindTeamNaming(ReservationRepository reservations, ReservationIEPublisher publisher, Clock clock) {
        this.reservations = reservations;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public RemindTeamNamingResponse remindDue() {
        Instant now = clock.instant();
        List<String> ids = new ArrayList<>();
        for (Reservation reservation : reservations.all()) {
            if (reservation.remindTeamNaming(now)) {
                reservations.save(reservation);
                publisher.publish(new TeamNamingReminderIE(reservation.id(), reservation.providerId(), now));
                ids.add(reservation.id());
            }
        }
        return new RemindTeamNamingResponse(ids);
    }
}
