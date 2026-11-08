package reservation.domain.exception;

/** A partial refund gives the provider between nothing and the amount at stake. */
public class InvalidDecisionAmountException extends ReservationRuleViolationException {

    public InvalidDecisionAmountException(Object amount, Object most) {
        super("The provider can receive from RM0.00 to " + most + ", not " + amount);
    }
}
