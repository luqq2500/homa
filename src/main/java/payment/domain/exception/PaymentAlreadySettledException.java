package payment.domain.exception;

/** A payment is settled once. */
public class PaymentAlreadySettledException extends PaymentRuleViolationException {

    public PaymentAlreadySettledException(String reservationId) {
        super("The payment for reservation " + reservationId + " was already settled");
    }
}
