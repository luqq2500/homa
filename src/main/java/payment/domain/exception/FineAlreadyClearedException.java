package payment.domain.exception;

/** A fine that is fully deducted has nothing left to waive. */
public class FineAlreadyClearedException extends PaymentRuleViolationException {

    public FineAlreadyClearedException(String fineId) {
        super("Fine " + fineId + " is already cleared");
    }
}
