package feedback.domain;

/** What a customer rates a provider on, each from 1 to 5 stars. */
public enum ProviderCategory {
    /** How well the booked jobs were done. */
    QUALITY_OF_WORK,
    /** Whether the team arrived on time. */
    PUNCTUALITY,
    /** How clear and timely the replies in chat were. */
    COMMUNICATION,
    /** How carefully the home and belongings were treated. */
    CARE_FOR_THE_HOME
}
