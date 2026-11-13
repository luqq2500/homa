package offering.usecase.port.in;

import ie.events.TeamMemberRoleChangedIE;

/** A changed role moves a person between the helpers and the supervisors. */
public interface TeamMemberRoleChangedIEHandler {

    void handle(TeamMemberRoleChangedIE event);
}
