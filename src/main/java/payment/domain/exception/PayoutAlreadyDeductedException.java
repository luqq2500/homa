package payment.domain.exception;

/** Fines are deducted from a payout once. */
public class PayoutAlreadyDeductedException extends PaymentRuleViolationException {

    public PayoutAlreadyDeductedException(String payoutId) {
        super("Fines were already deducted from payout " + payoutId);
    }
}
