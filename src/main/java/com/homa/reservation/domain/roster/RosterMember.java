package com.homa.reservation.domain.roster;

import java.util.Objects;

/**
 * A team member on a reservation's roster, with the name and verified photo the customer is shown (ROS-6, IDN-4).
 *
 * @param personId       the person the member is, as Verification knows them: the account they sign in with, so a
 *                       rostered member acts for the provider on this reservation (PRV-11)
 * @param photoReference a reference to the member's verified photo in the photo store
 */
public record RosterMember(String memberId, String personId, MemberRole role, String name, String photoReference) {

    public RosterMember {
        Objects.requireNonNull(memberId, "memberId");
        Objects.requireNonNull(personId, "personId");
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(photoReference, "photoReference");
    }

    public boolean isHelper() {
        return role == MemberRole.HELPER;
    }
}
