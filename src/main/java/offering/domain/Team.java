package offering.domain;

import java.util.HashMap;
import java.util.Map;

/** Who is on a provider's team, by role, as far as the offering needs to know. */
public record Team(Map<String, TeamRole> roles) {

    public static final Team EMPTY = new Team(Map.of());

    public Team {
        roles = Map.copyOf(roles);
    }

    /** The team with this person in this role, whether they are new or changed role. */
    public Team with(String memberId, TeamRole role) {
        Map<String, TeamRole> next = new HashMap<>(roles);
        next.put(memberId, role);
        return new Team(next);
    }

    public Team without(String memberId) {
        Map<String, TeamRole> next = new HashMap<>(roles);
        next.remove(memberId);
        return new Team(next);
    }

    public int helpers() {
        return count(TeamRole.HELPER);
    }

    public int supervisors() {
        return count(TeamRole.SUPERVISOR);
    }

    private int count(TeamRole role) {
        return (int) roles.values().stream().filter(role::equals).count();
    }
}
