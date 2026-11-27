package com.homa.reservation.usecase.port.in;

import com.homa.reservation.usecase.dto.AssignRosterRequest;
import com.homa.reservation.usecase.dto.AssignRosterResponse;

/** ROS-1 to ROS-3, ROS-6: the provider sets or changes the roster. */
public interface RosterAssigner {

    AssignRosterResponse assign(AssignRosterRequest request);
}
