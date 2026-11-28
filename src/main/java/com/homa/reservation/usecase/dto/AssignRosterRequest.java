package com.homa.reservation.usecase.dto;

import java.util.List;

/**
 * The provider sets or changes the roster (ROS-1).
 *
 * @param personId  who signed in: the provider's owner, or a member rostered on the reservation (PRV-11)
 * @param memberIds the helpers and supervisors, as Provider's team-member ids
 */
public record AssignRosterRequest(String reservationId, String personId, List<String> memberIds) {
    public AssignRosterRequest {
        memberIds = List.copyOf(memberIds);
    }
}
