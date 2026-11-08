package account.domain.exception;

/** Names, addresses, cities and photos cannot be left empty. */
public class AccountDetailRequiredException extends AccountRuleViolationException {

    public AccountDetailRequiredException(String what) {
        super(what + " is required");
    }
}
