package com.homa.reservation.usecase.port.in;

import com.homa.ie.events.TeamMemberActivatedIE;

/** ROS-3, ROS-4, ROS-6: a member who can be rostered. */
public interface TeamMemberActivatedIEHandler {

    void handle(TeamMemberActivatedIE event);
}
