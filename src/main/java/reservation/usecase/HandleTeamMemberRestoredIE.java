package reservation.usecase;

import ie.events.TeamMemberRestoredIE;
import reservation.domain.TeamRole;
import reservation.domain.TeamRoster;
import reservation.usecase.port.in.TeamMemberRestoredIEHandler;
import reservation.usecase.port.out.TeamRosterRepository;

public final class HandleTeamMemberRestoredIE implements TeamMemberRestoredIEHandler {

    private final TeamRosterRepository rosters;

    public HandleTeamMemberRestoredIE(TeamRosterRepository rosters) {
        this.rosters = rosters;
    }

    @Override
    public void handle(TeamMemberRestoredIE event) {
        TeamRoster roster = rosters.find(event.businessId()).orElseGet(() -> TeamRoster.empty(event.businessId()));
        rosters.save(roster.with(event.memberId(), TeamRole.valueOf(event.role())));
    }
}
