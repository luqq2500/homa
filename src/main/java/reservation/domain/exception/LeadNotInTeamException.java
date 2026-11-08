package reservation.domain.exception;

/** The lead is one of the named helpers. */
public class LeadNotInTeamException extends ReservationRuleViolationException {

    public LeadNotInTeamException(String lead) {
        super("The lead " + lead + " is not one of the named helpers");
    }
}
