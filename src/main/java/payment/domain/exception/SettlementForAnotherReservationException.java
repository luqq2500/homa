package payment.domain.exception;

/** A settlement is carried out on its own reservation's payment. */
public class SettlementForAnotherReservationException extends PaymentRuleViolationException {

    public SettlementForAnotherReservationException(String expected, String given) {
        super("The settlement is for reservation " + given + ", not " + expected);
    }
}
