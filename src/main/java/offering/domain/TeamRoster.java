package offering.domain;

import java.util.Objects;
import java.util.Set;

/**
 * The provider's verified team: the ids of every verified helper, including the provider when they are
 * their own helper. It caps the helpers range and is who the provider can name for a reservation.
 */
public record TeamRoster(Set<String> verifiedHelperIds) {

    public TeamRoster {
        verifiedHelperIds = Set.copyOf(Objects.requireNonNull(verifiedHelperIds));
        verifiedHelperIds.forEach(id -> OfferingText.require(id, "A helper id"));
    }

    public static TeamRoster of(Set<String> verifiedHelperIds) {
        return new TeamRoster(verifiedHelperIds);
    }

    public static TeamRoster empty() {
        return new TeamRoster(Set.of());
    }

    public int size() {
        return verifiedHelperIds.size();
    }

    public boolean includes(String helperId) {
        return verifiedHelperIds.contains(helperId);
    }
}
