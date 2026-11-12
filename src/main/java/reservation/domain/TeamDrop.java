package reservation.domain;

/** What a named person coming off the team did to this reservation, so the provider and customer can be told. */
public enum TeamDrop {
    /** The person wasn't named on this reservation's upcoming or ongoing job; nothing changed. */
    NOT_AFFECTED,
    /** The person came off the named team, and the team is still complete without them, for example an optional supervisor. */
    REMOVED,
    /** The person came off the named team before the booked start, so it is incomplete until the provider names someone in their place. */
    TEAM_INCOMPLETE,
    /** The booked start has passed, so the team is locked: the person stays named but cannot work. */
    CANNOT_WORK
}
