package reservation.usecase;

import ie.events.JobTeamMemberRemovedIE;
import ie.events.TeamMemberLeftIE;
import java.time.Clock;
import java.time.Instant;
import reservation.domain.Reservation;
import reservation.domain.TeamRoster;
import reservation.usecase.port.in.TeamMemberLeftIEHandler;
import reservation.usecase.port.out.ReservationIEPublisher;
import reservation.usecase.port.out.ReservationRepository;
import reservation.usecase.port.out.TeamRosterRepository;

public final class HandleTeamMemberLeftIE implements TeamMemberLeftIEHandler {

    private final TeamRosterRepository rosters;
    private final ReservationRepository reservations;
    private final ReservationIEPublisher publisher;
    private final Clock clock;

    public HandleTeamMemberLeftIE(TeamRosterRepository rosters, ReservationRepository reservations, ReservationIEPublisher publisher, Clock clock) {
        this.rosters = rosters;
        this.reservations = reservations;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public void handle(TeamMemberLeftIE event) {
        TeamRoster roster = rosters.find(event.businessId()).orElseGet(() -> TeamRoster.empty(event.businessId()));
        rosters.save(roster.without(event.memberId()));
        Instant now = clock.instant();
        for (Reservation reservation : reservations.ofProvider(event.businessId())) {
            if (reservation.removeFromJobTeam(event.memberId())) {
                reservations.save(reservation);
                publisher.publish(new JobTeamMemberRemovedIE(reservation.id(), reservation.customerId(),
                        reservation.providerId(), event.memberId(), now));
            }
        }
    }
}
