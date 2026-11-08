package offering.domain;

import java.util.Objects;
import java.util.Set;

/**
 * The provider's team: the ids of every team member, including the provider when they are
 * their own helper. It caps the helpers range and is who the provider can name for a reservation.
 */
public record TeamRoster(Set<String> memberIds) {

    public TeamRoster {
        memberIds = Set.copyOf(Objects.requireNonNull(memberIds));
        memberIds.forEach(id -> OfferingText.require(id, "A helper id"));
    }

    public static TeamRoster of(Set<String> memberIds) {
        return new TeamRoster(memberIds);
    }

    public static TeamRoster empty() {
        return new TeamRoster(Set.of());
    }

    public int size() {
        return memberIds.size();
    }

    public boolean includes(String helperId) {
        return memberIds.contains(helperId);
    }
}
