package payment.domain.exception;

/** Ids cannot be left empty. */
public class PaymentDetailRequiredException extends PaymentRuleViolationException {

    public PaymentDetailRequiredException(String what) {
        super(what + " is required");
    }
}
