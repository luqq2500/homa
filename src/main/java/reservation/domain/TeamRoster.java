package reservation.domain;

import java.util.Set;

/** The provider's team members when the team is named, including the provider if they are their own helper. Invited helpers are never on it. */
public record TeamRoster(Set<String> memberIds) {

    public TeamRoster {
        memberIds = Set.copyOf(memberIds);
    }

    public static TeamRoster of(String... memberIds) {
        return new TeamRoster(Set.of(memberIds));
    }

    public boolean includes(String helperId) {
        return memberIds.contains(helperId);
    }
}
