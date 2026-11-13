package verification.usecase.exception;

public class UnknownRoleException extends RuntimeException {

    public UnknownRoleException(String role) {
        super("A team member is a helper or a supervisor, not " + role);
    }
}
