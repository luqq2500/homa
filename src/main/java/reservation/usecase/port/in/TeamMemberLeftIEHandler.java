package reservation.usecase.port.in;

import ie.events.TeamMemberLeftIE;

/** A person who lapsed or was removed comes off every upcoming job they were named on. */
public interface TeamMemberLeftIEHandler {

    void handle(TeamMemberLeftIE event);
}
