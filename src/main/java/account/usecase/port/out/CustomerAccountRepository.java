package account.usecase.port.out;

import account.domain.CustomerAccount;
import account.domain.PhoneNumber;
import java.util.List;
import java.util.Optional;

public interface CustomerAccountRepository {

    void save(CustomerAccount customerAccount);

    Optional<CustomerAccount> find(String id);

    List<CustomerAccount> all();

    Optional<CustomerAccount> findByPhone(PhoneNumber phone);
}
