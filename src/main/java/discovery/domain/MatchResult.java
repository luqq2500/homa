package discovery.domain;

import java.util.Objects;
import java.util.Set;

/** A provider's match for a search, with the reasons it is not exact. */
public record MatchResult(String providerId, Match match, Set<MatchReason> reasons) {

    public MatchResult {
        Objects.requireNonNull(providerId);
        Objects.requireNonNull(match);
        reasons = Set.copyOf(reasons);
    }
}
