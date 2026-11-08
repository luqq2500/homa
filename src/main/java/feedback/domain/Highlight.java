package feedback.domain;

/** Something that went well, which a customer can pick for a provider. Highlights are positive only. */
public enum Highlight {
    /** The booked jobs were done well. */
    GREAT_WORK,
    /** The team arrived on time. */
    ON_TIME,
    /** Replies in chat were clear and timely. */
    RESPONSIVE,
    /** Belongings and the home were treated with care. */
    CAREFUL_WITH_THE_HOME
}
