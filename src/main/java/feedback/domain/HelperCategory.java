package feedback.domain;

/** What a customer can rate a helper on, each from 1 to 5 stars. */
public enum HelperCategory {
    /** How pleasant and polite the helper was. */
    FRIENDLINESS,
    /** How steadily the helper worked through the booked time. */
    HARD_WORK,
    /** How carefully the helper handled the home's things. */
    CARE_WITH_BELONGINGS,
    /** How well the helper followed requests within the booked jobs. */
    COOPERATION,
    /** How much attention to detail the helper paid. */
    THOROUGHNESS
}
