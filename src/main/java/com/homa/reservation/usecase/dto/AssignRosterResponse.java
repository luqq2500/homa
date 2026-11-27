package com.homa.reservation.usecase.dto;

import java.util.List;

/** The roster as the customer is shown it (ROS-6). */
public record AssignRosterResponse(String reservationId, List<RosteredMember> roster) {
    public AssignRosterResponse {
        roster = List.copyOf(roster);
    }

    /** A rostered member, with the name and verified photo the customer sees (ROS-6). */
    public record RosteredMember(String memberId, String role, String name, String photoRef) {
    }
}
