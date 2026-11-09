package account.domain.exception;

/** A team member's profile photo is matched against their eKYC once. */
public class PhotoMatchAlreadyRecordedException extends AccountRuleViolationException {

    public PhotoMatchAlreadyRecordedException(String memberId) {
        super("The photo match of team member " + memberId + " is already recorded");
    }
}
