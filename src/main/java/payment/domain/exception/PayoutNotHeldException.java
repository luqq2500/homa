package payment.domain.exception;

/** Only a held payout is released or withheld after the founders' review. */
public class PayoutNotHeldException extends PaymentRuleViolationException {

    public PayoutNotHeldException(String payoutId, Object status) {
        super("Payout " + payoutId + " is not held; it is " + status);
    }
}
