package com.homa.reservation.usecase.port.in;

import com.homa.ie.events.TeamMemberDeactivatedIE;

/** HLP-6, ROS-5: a member stopped being active, so they can't be rostered and upcoming rosters with them need fixing. */
public interface TeamMemberDeactivatedIEHandler {

    void handle(TeamMemberDeactivatedIE event);
}
