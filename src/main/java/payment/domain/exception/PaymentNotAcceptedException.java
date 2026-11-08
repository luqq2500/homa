package payment.domain.exception;

/** Only a payment kept for an accepted booking is settled. */
public class PaymentNotAcceptedException extends PaymentRuleViolationException {

    public PaymentNotAcceptedException() {
        super("The provider has not accepted; end the request before acceptance instead");
    }
}
