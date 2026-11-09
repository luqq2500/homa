package offering.domain;

import offering.domain.exception.TeamMemberInTwoRolesException;
import java.util.Objects;
import java.util.Set;

/**
 * The provider's team by role: the helpers, including the provider when they are a helper on their own
 * team, and the supervisors. Only helpers cap the helpers range; supervisors never count towards it. It is
 * who the provider can name for a reservation.
 */
public record TeamRoster(Set<String> helperIds, Set<String> supervisorIds) {

    public TeamRoster {
        helperIds = Set.copyOf(Objects.requireNonNull(helperIds));
        supervisorIds = Set.copyOf(Objects.requireNonNull(supervisorIds));
        helperIds.forEach(id -> OfferingText.require(id, "A helper id"));
        supervisorIds.forEach(id -> OfferingText.require(id, "A supervisor id"));
        helperIds.stream().filter(supervisorIds::contains).findFirst().ifPresent(id -> {
            throw new TeamMemberInTwoRolesException(id);
        });
    }

    public static TeamRoster of(Set<String> helperIds, Set<String> supervisorIds) {
        return new TeamRoster(helperIds, supervisorIds);
    }

    public static TeamRoster ofHelpers(Set<String> helperIds) {
        return new TeamRoster(helperIds, Set.of());
    }

    public static TeamRoster empty() {
        return new TeamRoster(Set.of(), Set.of());
    }

    public int helperCount() {
        return helperIds.size();
    }

    public boolean hasSupervisor() {
        return !supervisorIds.isEmpty();
    }
}
