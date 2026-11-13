package reservation.domain;

/** A helper does the booked jobs; a supervisor is in charge on site and is not counted as a helper. */
public enum TeamRole {
    HELPER,
    SUPERVISOR
}
