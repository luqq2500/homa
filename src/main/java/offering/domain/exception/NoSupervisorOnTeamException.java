package offering.domain.exception;

/** A provider can set when a supervisor is sent only with a supervisor on the team. */
public class NoSupervisorOnTeamException extends OfferingRuleViolationException {

    public NoSupervisorOnTeamException() {
        super("The team has no supervisor");
    }
}
