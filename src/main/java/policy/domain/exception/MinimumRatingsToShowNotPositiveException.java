package policy.domain.exception;

/** Average stars are shown after at least one rating. */
public class MinimumRatingsToShowNotPositiveException extends PolicyRuleViolationException {

    public MinimumRatingsToShowNotPositiveException(int ratings) {
        super("The minimum ratings to show must be at least 1, was " + ratings);
    }
}
