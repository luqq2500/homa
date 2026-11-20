package com.homa.reservation.domain.roster;

import com.homa.reservation.domain.exception.IncompleteRosterException;
import java.util.List;

/**
 * The team rostered on a reservation, an entity inside the Reservation aggregate. It has exactly the booked number
 * of helpers; supervisors are optional, extra to that number and not billed (ROS-2). Each member appears once.
 */
public final class Roster {

    private final List<RosterMember> members;

    private Roster(List<RosterMember> members) {
        this.members = members;
    }

    public static Roster of(List<RosterMember> members, int bookedHelpers) {
        List<RosterMember> copy = List.copyOf(members);
        if (copy.stream().map(RosterMember::memberId).distinct().count() != copy.size()) {
            throw new IncompleteRosterException("each member can be rostered once");
        }
        if (copy.stream().filter(RosterMember::isHelper).count() != bookedHelpers) {
            throw new IncompleteRosterException("it must include exactly " + bookedHelpers + " helpers");
        }
        return new Roster(copy);
    }

    /** ROS-6: everyone the customer is shown, with name and photo. */
    public List<RosterMember> members() {
        return members;
    }

    public List<RosterMember> helpers() {
        return members.stream().filter(RosterMember::isHelper).toList();
    }
}
