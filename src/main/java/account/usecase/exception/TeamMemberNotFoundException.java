package account.usecase.exception;

/** No team member has this id, or they were removed and cannot come back. */
public class TeamMemberNotFoundException extends AccountUsecaseException {

    public TeamMemberNotFoundException(String memberId) {
        super("There is no team member " + memberId);
    }
}
