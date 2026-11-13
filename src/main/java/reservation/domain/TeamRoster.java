package reservation.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Everyone on a provider's team right now, with their role: the only people who can be named for a job. */
public record TeamRoster(String providerId, Map<String, TeamRole> members) {

    public TeamRoster {
        members = Map.copyOf(members);
    }

    public static TeamRoster empty(String providerId) {
        return new TeamRoster(providerId, Map.of());
    }

    public TeamRoster with(String memberId, TeamRole role) {
        Map<String, TeamRole> next = new HashMap<>(members);
        next.put(memberId, role);
        return new TeamRoster(providerId, next);
    }

    public TeamRoster without(String memberId) {
        Map<String, TeamRole> next = new HashMap<>(members);
        next.remove(memberId);
        return new TeamRoster(providerId, next);
    }

    public Optional<TeamRole> roleOf(String memberId) {
        return Optional.ofNullable(members.get(memberId));
    }
}
