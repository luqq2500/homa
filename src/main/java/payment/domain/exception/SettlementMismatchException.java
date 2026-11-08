package payment.domain.exception;

/** A settlement accounts for exactly the amount held. */
public class SettlementMismatchException extends PaymentRuleViolationException {

    public SettlementMismatchException(Object settled, Object held) {
        super("The settlement adds up to " + settled + " but " + held + " is held");
    }
}
