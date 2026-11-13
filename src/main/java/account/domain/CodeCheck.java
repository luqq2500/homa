package account.domain;

/**
 * What the SMS provider says about a code a person typed. The provider sends the code and applies
 * its own rules for how long a code is valid and how many tries are allowed; Homa relies on its answer.
 */
public enum CodeCheck {
    PASSED,
    WRONG,
    EXPIRED,
    TOO_MANY_TRIES
}
