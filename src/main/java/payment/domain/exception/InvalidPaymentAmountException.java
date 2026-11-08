package payment.domain.exception;

/** A booking total is above zero. */
public class InvalidPaymentAmountException extends PaymentRuleViolationException {

    public InvalidPaymentAmountException(Object amount) {
        super("A payment amount must be above zero, was " + amount);
    }
}
