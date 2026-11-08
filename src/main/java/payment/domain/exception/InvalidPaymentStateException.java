package payment.domain.exception;

/** Each payment step is allowed only from certain states. */
public class InvalidPaymentStateException extends PaymentRuleViolationException {

    public InvalidPaymentStateException(Object state, String action) {
        super("A payment that is " + state + " cannot be " + action);
    }
}
