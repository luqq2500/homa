package payment.domain.exception;

/** A payout pays a provider something. */
public class InvalidPayoutAmountException extends PaymentRuleViolationException {

    public InvalidPayoutAmountException(Object amount) {
        super("A payout must be above zero, was " + amount);
    }
}
