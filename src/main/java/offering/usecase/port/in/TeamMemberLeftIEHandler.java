package offering.usecase.port.in;

import ie.events.TeamMemberLeftIE;

/** A team member who left no longer counts; the helper range shrinks to match. */
public interface TeamMemberLeftIEHandler {

    void handle(TeamMemberLeftIE event);
}
