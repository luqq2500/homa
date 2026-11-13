package offering.usecase.port.in;

import ie.events.TeamMemberJoinedIE;

/** A new team member counts towards the helpers or supervisors on the team. */
public interface TeamMemberJoinedIEHandler {

    void handle(TeamMemberJoinedIE event);
}
