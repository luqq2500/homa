package account.domain.exception;

/** A removed helper, whether a withdrawn invite or a removed member, cannot come back onto the team. */
public class HelperRemovedException extends AccountRuleViolationException {

    public HelperRemovedException(String helperId) {
        super("Helper " + helperId + " has been removed from the team");
    }
}
