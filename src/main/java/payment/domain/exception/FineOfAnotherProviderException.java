package payment.domain.exception;

/** A provider's payout pays only that provider's fines. */
public class FineOfAnotherProviderException extends PaymentRuleViolationException {

    public FineOfAnotherProviderException(String fineId, String providerId) {
        super("Fine " + fineId + " does not belong to provider " + providerId);
    }
}
