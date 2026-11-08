package resolution.domain.exception;

/** A partial refund gives the provider between nothing and what is at stake. */
public class InvalidDecisionAmountException extends ResolutionRuleViolationException {

    public InvalidDecisionAmountException(Object amount, Object atStake) {
        super("The provider can receive from RM0.00 to " + atStake + ", not " + amount);
    }
}
