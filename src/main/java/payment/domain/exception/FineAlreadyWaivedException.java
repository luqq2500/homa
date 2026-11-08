package payment.domain.exception;

/** A fine is waived once. */
public class FineAlreadyWaivedException extends PaymentRuleViolationException {

    public FineAlreadyWaivedException(String fineId) {
        super("Fine " + fineId + " was already waived");
    }
}
