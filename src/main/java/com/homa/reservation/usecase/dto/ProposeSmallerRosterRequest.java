package com.homa.reservation.usecase.dto;

import java.util.List;

/**
 * The provider offers the customer a smaller roster (ROS-8).
 *
 * @param personId  who signed in: the provider's owner, or a member rostered on the reservation (PRV-11)
 * @param memberIds the smaller team on offer, as Provider's team-member ids
 */
public record ProposeSmallerRosterRequest(String reservationId, String personId, List<String> memberIds) {
    public ProposeSmallerRosterRequest {
        memberIds = List.copyOf(memberIds);
    }
}
