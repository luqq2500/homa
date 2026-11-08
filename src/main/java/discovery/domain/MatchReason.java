package discovery.domain;

/** Why a provider is a close match or no match. */
public enum MatchReason {
    /** Close: offers some but not all of the chosen jobs. */
    MISSING_JOBS,
    /** Close: not free at the chosen time, but free near it. */
    NEARBY_TIME,
    /** None: does not cover the customer's city. */
    CITY_NOT_COVERED,
    /** None: the helpers chosen are outside the provider's range. */
    HELPERS_OUTSIDE_RANGE,
    /** None: the duration chosen is outside the provider's range. */
    DURATION_OUTSIDE_RANGE,
    /** None: offers none of the chosen jobs. */
    NO_CHOSEN_JOBS,
    /** None: not free at or near the chosen time. */
    NOT_FREE
}
