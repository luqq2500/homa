package account.usecase.port.out;

import account.domain.BusinessAccount;
import account.domain.PhoneNumber;
import java.util.List;
import java.util.Optional;

public interface BusinessAccountRepository {

    void save(BusinessAccount businessAccount);

    Optional<BusinessAccount> find(String id);

    List<BusinessAccount> all();

    Optional<BusinessAccount> findByPhone(PhoneNumber phone);
}
