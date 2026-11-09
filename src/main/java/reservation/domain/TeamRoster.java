package reservation.domain;

import java.util.Set;

/**
 * The provider's team when the team is named, by role. The provider is a helper on it when they are on their
 * own team. Only verified team members are ever on it.
 */
public record TeamRoster(Set<String> helperIds, Set<String> supervisorIds) {

    public TeamRoster {
        helperIds = Set.copyOf(helperIds);
        supervisorIds = Set.copyOf(supervisorIds);
    }

    public static TeamRoster ofHelpers(String... helperIds) {
        return new TeamRoster(Set.of(helperIds), Set.of());
    }

    public TeamRoster withSupervisors(String... ids) {
        return new TeamRoster(helperIds, Set.of(ids));
    }
}
