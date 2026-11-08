package feedback.domain;

/** What a customer can rate a provider on, besides the overall stars. */
public enum ProviderCategory {
    /** The booked jobs were done well. */
    QUALITY_OF_WORK,
    /** The team arrived on time. */
    PUNCTUALITY,
    /** Replies in chat were clear and timely. */
    COMMUNICATION,
    /** Belongings and the home were treated with care. */
    CARE_FOR_THE_HOME
}
