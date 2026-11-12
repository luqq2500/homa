package account.usecase.port.out;

import account.domain.CustomerAccount;
import account.usecase.exception.ChangedMeanwhileException;
import java.util.Optional;

/**
 * Keeps the customer accounts. A phone number has at most one customer account; infra also keeps this
 * with a unique constraint, so two registrations at once cannot both succeed.
 *
 * <p>A save is refused with {@link ChangedMeanwhileException} when what is stored changed after this copy
 * was found. Nothing is overwritten; the caller tries again on what is there now.
 */
public interface CustomerAccountRepository {

    Optional<CustomerAccount> find(String customerId);

    boolean existsWithPhone(String phone);

    void save(CustomerAccount account);
}
