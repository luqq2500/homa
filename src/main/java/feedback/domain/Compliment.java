package feedback.domain;

/** Something that went well, which a customer can pick for a helper. Compliments are positive only. */
public enum Compliment {
    /** Pleasant and polite. */
    FRIENDLY,
    /** Kept working through the booked time. */
    HARDWORKING,
    /** Handled the home's things with care. */
    CAREFUL_WITH_BELONGINGS,
    /** Followed the customer's requests within the booked jobs. */
    COOPERATIVE,
    /** Paid attention to detail. */
    THOROUGH
}
