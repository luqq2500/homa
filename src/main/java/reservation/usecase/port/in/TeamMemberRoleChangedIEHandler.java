package reservation.usecase.port.in;

import ie.events.TeamMemberRoleChangedIE;

/** The new role applies to jobs named from now on. */
public interface TeamMemberRoleChangedIEHandler {

    void handle(TeamMemberRoleChangedIE event);
}
