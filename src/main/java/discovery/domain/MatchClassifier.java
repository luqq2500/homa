package discovery.domain;

import java.time.Duration;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Sorts a provider into an exact match, a close match or no match for a search.
 *
 * <ul>
 *   <li><b>Exact:</b> covers the city, offers every chosen job, fits the helpers and duration, and is free
 *       at the chosen time.
 *   <li><b>Close:</b> covers the city and fits the helpers and duration, but offers only some of the jobs,
 *       or is free only near the chosen time.
 *   <li><b>No match:</b> anything else.
 * </ul>
 */
public final class MatchClassifier {

    /** How far from the chosen start a provider may be free and still be a close match. */
    public static final Duration NEAR_THE_CHOSEN_TIME = Duration.ofHours(2);

    private MatchClassifier() {
    }

    public static MatchResult classify(SearchCriteria criteria, ProviderCandidate candidate) {
        Set<MatchReason> none = failing(noMatchRules(criteria), candidate);
        if (!none.isEmpty()) {
            return new MatchResult(candidate.providerId(), Match.NONE, none);
        }
        Set<MatchReason> close = failing(closeMatchRules(criteria), candidate);
        return new MatchResult(candidate.providerId(), close.isEmpty() ? Match.EXACT : Match.CLOSE, close);
    }

    /** Each rule a provider must meet to be any match at all. */
    private static Map<MatchReason, Predicate<ProviderCandidate>> noMatchRules(SearchCriteria criteria) {
        Map<MatchReason, Predicate<ProviderCandidate>> rules = new LinkedHashMap<>();
        rules.put(MatchReason.CITY_NOT_COVERED, candidate -> candidate.cities().contains(criteria.city()));
        rules.put(MatchReason.HELPERS_OUTSIDE_RANGE,
                candidate -> criteria.helpers() >= candidate.minHelpers() && criteria.helpers() <= candidate.maxHelpers());
        rules.put(MatchReason.DURATION_OUTSIDE_RANGE,
                candidate -> criteria.hours() >= candidate.minHours() && criteria.hours() <= candidate.maxHours());
        rules.put(MatchReason.NO_CHOSEN_JOBS, candidate -> criteria.jobs().stream().anyMatch(candidate.jobs()::contains));
        rules.put(MatchReason.NOT_FREE, candidate -> candidate.nearestFreeStart().isPresent());
        return rules;
    }

    /** Each rule an exact match also meets. */
    private static Map<MatchReason, Predicate<ProviderCandidate>> closeMatchRules(SearchCriteria criteria) {
        Map<MatchReason, Predicate<ProviderCandidate>> rules = new LinkedHashMap<>();
        rules.put(MatchReason.MISSING_JOBS, candidate -> candidate.jobs().containsAll(criteria.jobs()));
        rules.put(MatchReason.NEARBY_TIME, candidate -> candidate.nearestFreeStart().orElseThrow().equals(criteria.start()));
        return rules;
    }

    private static Set<MatchReason> failing(Map<MatchReason, Predicate<ProviderCandidate>> rules, ProviderCandidate candidate) {
        Set<MatchReason> failed = EnumSet.noneOf(MatchReason.class);
        rules.forEach((reason, met) -> {
            if (!met.test(candidate)) {
                failed.add(reason);
            }
        });
        return failed;
    }
}
