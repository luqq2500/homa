package com.homa.reservation.domain.roster;

import java.util.Objects;

/**
 * A team member on a reservation's roster, with the name and photo the customer is shown (ROS-6).
 *
 * @param photoReference a reference to the member's photo in the photo store
 */
public record RosterMember(String memberId, MemberRole role, String name, String photoReference) {

    public RosterMember {
        Objects.requireNonNull(memberId, "memberId");
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(photoReference, "photoReference");
    }

    public boolean isHelper() {
        return role == MemberRole.HELPER;
    }
}
