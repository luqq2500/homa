package account.domain.exception;

public class AccountClosedException extends RuntimeException {

    public AccountClosedException(String accountId) {
        super("The account " + accountId + " is closed");
    }
}
