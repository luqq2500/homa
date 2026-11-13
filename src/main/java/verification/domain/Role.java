package verification.domain;

/** The one role each team member has. */
public enum Role {
    /** Does the booked jobs; customers book and pay for helpers. */
    HELPER,
    /** The person in charge on site; not counted or charged as a helper. */
    SUPERVISOR
}
