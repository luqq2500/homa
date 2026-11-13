package account.support;

import account.domain.BusinessAccount;
import account.domain.PhoneNumber;
import account.usecase.port.out.BusinessAccountRepository;
import java.util.Optional;
import support.InMemoryStore;

public final class InMemoryBusinessAccountRepository extends InMemoryStore<BusinessAccount> implements BusinessAccountRepository {

    public InMemoryBusinessAccountRepository() {
        super(BusinessAccount::id);
    }

    @Override
    public Optional<BusinessAccount> findByPhone(PhoneNumber phone) {
        return all().stream().filter(account -> account.phone().equals(phone)).findFirst();
    }
}
