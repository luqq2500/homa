package payment.domain.exception;

/** Only a payout ready to be paid can be put on hold. */
public class PayoutNotPayableException extends PaymentRuleViolationException {

    public PayoutNotPayableException(String payoutId, Object status) {
        super("Payout " + payoutId + " cannot be held; it is " + status);
    }
}
