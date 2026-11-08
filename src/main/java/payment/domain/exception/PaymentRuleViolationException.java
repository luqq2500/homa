package payment.domain.exception;

/** A business rule of money movement was broken. Every payment domain exception is one of these. */
public abstract class PaymentRuleViolationException extends RuntimeException {

    protected PaymentRuleViolationException(String message) {
        super(message);
    }
}
