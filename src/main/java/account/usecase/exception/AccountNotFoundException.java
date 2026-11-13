package account.usecase.exception;

public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(String accountId) {
        super("No account " + accountId);
    }
}
