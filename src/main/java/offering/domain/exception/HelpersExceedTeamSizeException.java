package offering.domain.exception;

/** Max helpers cannot exceed the verified team size, so the provider can always name everyone who is coming. */
public class HelpersExceedTeamSizeException extends OfferingRuleViolationException {

    public HelpersExceedTeamSizeException(int max, int teamSize) {
        super("Max helpers " + max + " exceeds the verified team size of " + teamSize);
    }
}
