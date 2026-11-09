package account.domain;

/** A team member's one role. */
public enum TeamRole {
    /** Does the booked jobs. The customer books and pays for helpers. */
    HELPER,
    /** The person in charge on site. Not counted or charged as a helper. */
    SUPERVISOR
}
