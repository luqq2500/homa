package reservation.usecase.port.in;

import ie.events.TeamMemberRestoredIE;

/** A restored team member can be named for jobs again. */
public interface TeamMemberRestoredIEHandler {

    void handle(TeamMemberRestoredIE event);
}
