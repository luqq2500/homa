package payment.domain.exception;

/** After the provider accepts, the payment no longer changes with the request. */
public class PaymentAlreadyAcceptedException extends PaymentRuleViolationException {

    public PaymentAlreadyAcceptedException() {
        super("The provider already accepted; the payment is kept for the booking");
    }
}
