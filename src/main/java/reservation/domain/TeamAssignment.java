package reservation.domain;

import java.util.HashSet;
import java.util.Set;
import reservation.domain.exception.MemberNamedTwiceException;

/**
 * Who is coming: the helpers named for this reservation, matching the helpers booked, and any supervisors,
 * the people in charge on site. The same person is never named twice.
 */
public record TeamAssignment(Set<String> helpers, Set<String> supervisors) {

    public TeamAssignment {
        helpers = Set.copyOf(helpers);
        supervisors = Set.copyOf(supervisors);
        helpers.forEach(id -> ReservationText.require(id, "A helper id"));
        supervisors.forEach(id -> ReservationText.require(id, "A supervisor id"));
        helpers.stream().filter(supervisors::contains).findFirst().ifPresent(id -> {
            throw new MemberNamedTwiceException(id);
        });
    }

    public static TeamAssignment helpers(String... helperIds) {
        return new TeamAssignment(Set.of(helperIds), Set.of());
    }

    public TeamAssignment withSupervisors(String... supervisorIds) {
        return new TeamAssignment(helpers, Set.of(supervisorIds));
    }

    /** Everyone named: the helpers and the supervisors. */
    public Set<String> members() {
        Set<String> everyone = new HashSet<>(helpers);
        everyone.addAll(supervisors);
        return Set.copyOf(everyone);
    }

    /** The same team without {@code memberId}. */
    TeamAssignment without(String memberId) {
        Set<String> remainingHelpers = new HashSet<>(helpers);
        Set<String> remainingSupervisors = new HashSet<>(supervisors);
        remainingHelpers.remove(memberId);
        remainingSupervisors.remove(memberId);
        return new TeamAssignment(remainingHelpers, remainingSupervisors);
    }

    boolean names(String memberId) {
        return helpers.contains(memberId) || supervisors.contains(memberId);
    }
}
