package reservation.usecase;

import ie.events.TeamMemberJoinedIE;
import reservation.domain.TeamRole;
import reservation.domain.TeamRoster;
import reservation.usecase.port.in.TeamMemberJoinedIEHandler;
import reservation.usecase.port.out.TeamRosterRepository;

public final class HandleTeamMemberJoinedIE implements TeamMemberJoinedIEHandler {

    private final TeamRosterRepository rosters;

    public HandleTeamMemberJoinedIE(TeamRosterRepository rosters) {
        this.rosters = rosters;
    }

    @Override
    public void handle(TeamMemberJoinedIE event) {
        TeamRoster roster = rosters.find(event.businessId()).orElseGet(() -> TeamRoster.empty(event.businessId()));
        rosters.save(roster.with(event.memberId(), TeamRole.valueOf(event.role())));
    }
}
