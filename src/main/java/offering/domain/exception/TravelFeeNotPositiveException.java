package offering.domain.exception;

/** A separate travel fee is above zero; a provider who charges none includes travel in the price. */
public class TravelFeeNotPositiveException extends OfferingRuleViolationException {

    public TravelFeeNotPositiveException(Object fee) {
        super("A separate travel fee must be above zero, was " + fee);
    }
}
