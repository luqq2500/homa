package account.domain.exception;

/** A verified person joins the team only once the provider confirms they are lawfully employed or engaged. */
public class EmploymentNotConfirmedException extends AccountRuleViolationException {

    public EmploymentNotConfirmedException() {
        super("The provider must confirm lawful employment to add a team member");
    }
}
