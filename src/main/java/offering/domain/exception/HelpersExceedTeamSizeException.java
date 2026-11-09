package offering.domain.exception;

/** Max helpers cannot exceed the helpers on the team, so the provider can always name everyone who is coming. */
public class HelpersExceedTeamSizeException extends OfferingRuleViolationException {

    public HelpersExceedTeamSizeException(int max, int teamSize) {
        super("Max helpers " + max + " exceeds the " + teamSize + " helpers on the team");
    }
}
