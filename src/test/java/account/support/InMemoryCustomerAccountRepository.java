package account.support;

import account.domain.CustomerAccount;
import account.domain.PhoneNumber;
import account.usecase.port.out.CustomerAccountRepository;
import java.util.Optional;
import support.InMemoryStore;

public final class InMemoryCustomerAccountRepository extends InMemoryStore<CustomerAccount> implements CustomerAccountRepository {

    public InMemoryCustomerAccountRepository() {
        super(CustomerAccount::id);
    }

    @Override
    public Optional<CustomerAccount> findByPhone(PhoneNumber phone) {
        return all().stream().filter(account -> account.phone().equals(phone)).findFirst();
    }
}
