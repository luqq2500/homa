package reservation.usecase;

import ie.events.JobTeamNamedIE;
import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import reservation.domain.Reservation;
import reservation.domain.TeamRoster;
import reservation.usecase.dto.NameJobTeamRequest;
import reservation.usecase.dto.NameJobTeamResponse;
import reservation.usecase.port.in.ProviderJobTeamNamer;
import reservation.usecase.port.out.ReservationIEPublisher;
import reservation.usecase.port.out.ReservationRepository;
import reservation.usecase.port.out.TeamRosterRepository;

public final class ProviderNameJobTeam implements ProviderJobTeamNamer {

    private final ReservationRepository reservations;
    private final TeamRosterRepository rosters;
    private final ReservationIEPublisher publisher;
    private final Clock clock;

    public ProviderNameJobTeam(ReservationRepository reservations, TeamRosterRepository rosters, ReservationIEPublisher publisher, Clock clock) {
        this.reservations = reservations;
        this.rosters = rosters;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public NameJobTeamResponse name(NameJobTeamRequest request) {
        Instant now = clock.instant();
        Reservation reservation =
                OwnedReservations.ofProvider(reservations, request.providerId(), request.reservationId());
        TeamRoster roster = rosters.find(reservation.providerId())
                .orElseGet(() -> TeamRoster.empty(reservation.providerId()));
        reservation.nameJobTeam(request.memberIds(), roster, now);
        reservations.save(reservation);
        Set<String> named = reservation.jobTeam().orElseThrow().memberIds();
        publisher.publish(new JobTeamNamedIE(reservation.id(), reservation.customerId(), reservation.providerId(),
                named, now));
        return new NameJobTeamResponse(reservation.id(), named);
    }
}
