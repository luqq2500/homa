package account.usecase.exception;

/** A team member is a helper or a supervisor. */
public class UnknownTeamRoleException extends AccountUsecaseException {

    public UnknownTeamRoleException(String role) {
        super("A team member is a HELPER or a SUPERVISOR, was " + role);
    }
}
