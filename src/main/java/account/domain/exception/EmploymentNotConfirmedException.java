package account.domain.exception;

/** The add form cannot be sent without the provider confirming this person is lawfully employed or engaged. */
public class EmploymentNotConfirmedException extends AccountRuleViolationException {

    public EmploymentNotConfirmedException() {
        super("The provider must confirm lawful employment to add a team member");
    }
}
