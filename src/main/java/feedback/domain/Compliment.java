package feedback.domain;

/** A positive-only compliment a customer can give a helper. Anything negative goes in the stars or a report. */
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
