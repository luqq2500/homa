package reservation.domain;

import java.util.Set;

/** The provider's verified team when the team is named, including the provider if they are their own helper. */
public record TeamRoster(Set<String> verifiedHelperIds) {

    public TeamRoster {
        verifiedHelperIds = Set.copyOf(verifiedHelperIds);
    }

    public static TeamRoster of(String... verifiedHelperIds) {
        return new TeamRoster(Set.of(verifiedHelperIds));
    }

    public boolean includes(String helperId) {
        return verifiedHelperIds.contains(helperId);
    }
}
