package com.homa.reservation.domain.roster;

import com.homa.reservation.domain.exception.IncompleteRosterException;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The team rostered on a reservation, an entity inside the Reservation aggregate. It has exactly the booked number
 * of helpers; supervisors are optional, extra to that number and not billed (ROS-2). Each member appears once.
 *
 * <p>The roster is also what the provider still has to fix and when it stopped changing. A member who stopped being
 * active before the Check-in Period is flagged to be replaced (ROS-5), and until the provider sets a new roster the
 * roster isn't complete (ROS-7). It locks when the Check-in Period starts (ROS-1). A roster is never edited: each
 * change gives a new one.
 */
public final class Roster {

    private final List<RosterMember> members;
    private final Set<String> membersToReplace;
    private final Instant lockedAt;

    private Roster(List<RosterMember> members, Set<String> membersToReplace, Instant lockedAt) {
        this.members = members;
        this.membersToReplace = membersToReplace;
        this.lockedAt = lockedAt;
    }

    /** A roster the provider sets: the booked number of helpers, each member once, nobody to replace, not locked. */
    public static Roster of(List<RosterMember> members, int bookedHelpers) {
        List<RosterMember> copy = List.copyOf(members);
        if (!eachMemberOnce(copy)) {
            throw new IncompleteRosterException("each member can be rostered once");
        }
        if (copy.stream().filter(RosterMember::isHelper).count() != bookedHelpers) {
            throw new IncompleteRosterException("it must include exactly " + bookedHelpers + " helpers");
        }
        return new Roster(copy, Set.of(), null);
    }

    /**
     * Rebuilds a roster from what was stored. Only the shape is checked: each member once, those to replace are on
     * the roster, and a locked roster has nobody left to replace, because it locked complete.
     *
     * @param lockedAt when the Check-in Period locked it; {@code null} while it can still change
     */
    public static Roster restore(List<RosterMember> members, Set<String> membersToReplace, Instant lockedAt) {
        List<RosterMember> copy = List.copyOf(members);
        Set<String> toReplace = Set.copyOf(membersToReplace);
        if (!eachMemberOnce(copy)) {
            throw new IllegalArgumentException("A stored roster needs each member once");
        }
        if (!copy.stream().map(RosterMember::memberId).toList().containsAll(toReplace)) {
            throw new IllegalArgumentException("A stored roster needs the members to replace on the roster");
        }
        if (lockedAt != null && !toReplace.isEmpty()) {
            throw new IllegalArgumentException("A stored roster needs nobody to replace once it is locked");
        }
        return new Roster(copy, toReplace, lockedAt);
    }

    private static boolean eachMemberOnce(List<RosterMember> members) {
        return members.stream().map(RosterMember::memberId).distinct().count() == members.size();
    }

    /** ROS-6: everyone the customer is shown, with name and verified photo. */
    public List<RosterMember> members() {
        return members;
    }

    public List<RosterMember> helpers() {
        return members.stream().filter(RosterMember::isHelper).toList();
    }

    /** PRV-11: this person is rostered, so they act for the provider on the reservation. */
    public boolean includesPerson(String personId) {
        return members.stream().anyMatch(member -> member.personId().equals(personId));
    }

    public boolean includes(String memberId) {
        return members.stream().anyMatch(member -> member.memberId().equals(memberId));
    }

    /**
     * ROS-5: this member is on the roster and hasn't been flagged yet, so their no longer being active is news to
     * the roster.
     */
    public boolean canFlagForReplacement(String memberId) {
        return includes(memberId) && !membersToReplace.contains(memberId);
    }

    /** ROS-5: the roster with this member flagged to be replaced before the Check-in Period starts. */
    public Roster flaggedForReplacement(String memberId) {
        Set<String> flagged = new HashSet<>(membersToReplace);
        flagged.add(memberId);
        return new Roster(members, flagged, lockedAt);
    }

    /** ROS-5: the members who stopped being active and aren't replaced yet. */
    public Set<String> membersToReplace() {
        return membersToReplace;
    }

    /** ROS-7: complete once nobody is left to replace. Its helper count is the booked one, by construction. */
    public boolean isComplete() {
        return membersToReplace.isEmpty();
    }

    /** ROS-1: the roster as the Check-in Period locked it, at {@code at}. */
    public Roster locked(Instant at) {
        return new Roster(members, membersToReplace, at);
    }

    public boolean isLocked() {
        return lockedAt != null;
    }

    /** ROS-1: when the Check-in Period locked it; empty while the provider can still change it. */
    public Optional<Instant> lockedAt() {
        return Optional.ofNullable(lockedAt);
    }
}
