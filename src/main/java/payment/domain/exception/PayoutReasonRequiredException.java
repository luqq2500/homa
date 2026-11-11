package payment.domain.exception;

/** Holding or withholding a payout is recorded with a reason. */
public class PayoutReasonRequiredException extends PaymentRuleViolationException {

    public PayoutReasonRequiredException() {
        super("A reason is required to hold or withhold a payout");
    }
}
