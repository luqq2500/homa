package offering.usecase.port.in;

import ie.events.TeamMemberRestoredIE;

/** A restored team member counts on the team again. */
public interface TeamMemberRestoredIEHandler {

    void handle(TeamMemberRestoredIE event);
}
