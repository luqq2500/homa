package account.usecase.port.out;

import account.domain.ProviderAccount;
import account.usecase.exception.ChangedMeanwhileException;
import java.util.Optional;

/**
 * Keeps the business provider accounts. A phone number has at most one provider account; infra also keeps
 * this with a unique constraint.
 *
 * <p>A save is refused with {@link ChangedMeanwhileException} when what is stored changed after this copy
 * was found. Nothing is overwritten; the caller tries again on what is there now.
 */
public interface ProviderAccountRepository {

    Optional<ProviderAccount> find(String providerId);

    boolean existsWithPhone(String phone);

    void save(ProviderAccount account);
}
