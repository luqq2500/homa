package reservation.usecase.port.in;

import ie.events.TeamMemberJoinedIE;

/** A new team member can be named for jobs. */
public interface TeamMemberJoinedIEHandler {

    void handle(TeamMemberJoinedIE event);
}
