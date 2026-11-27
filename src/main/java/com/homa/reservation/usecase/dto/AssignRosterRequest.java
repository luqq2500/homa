package com.homa.reservation.usecase.dto;

import java.util.List;

/** The provider sets or changes the roster (ROS-1).
 *
 * @param memberIds the helpers and supervisors, as Provider's team-member ids */
public record AssignRosterRequest(String reservationId, String providerId, List<String> memberIds) {
    public AssignRosterRequest {
        memberIds = List.copyOf(memberIds);
    }
}
