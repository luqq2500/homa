package reservation.usecase;

import ie.events.TeamMemberRoleChangedIE;
import reservation.domain.TeamRole;
import reservation.domain.TeamRoster;
import reservation.usecase.port.in.TeamMemberRoleChangedIEHandler;
import reservation.usecase.port.out.TeamRosterRepository;

public final class HandleTeamMemberRoleChangedIE implements TeamMemberRoleChangedIEHandler {

    private final TeamRosterRepository rosters;

    public HandleTeamMemberRoleChangedIE(TeamRosterRepository rosters) {
        this.rosters = rosters;
    }

    @Override
    public void handle(TeamMemberRoleChangedIE event) {
        TeamRoster roster = rosters.find(event.businessId()).orElseGet(() -> TeamRoster.empty(event.businessId()));
        rosters.save(roster.with(event.memberId(), TeamRole.valueOf(event.role())));
    }
}
