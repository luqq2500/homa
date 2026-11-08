package payment.domain.exception;

/** A fine is above zero. */
public class InvalidFineAmountException extends PaymentRuleViolationException {

    public InvalidFineAmountException(Object amount) {
        super("A fine must be above zero, was " + amount);
    }
}
