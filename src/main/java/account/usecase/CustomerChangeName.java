package account.usecase;

import account.domain.CustomerAccount;
import account.usecase.dto.ChangeNameRequest;
import account.usecase.dto.ChangeNameResponse;
import account.usecase.port.in.CustomerNameChanger;
import account.usecase.port.out.AccountIEPublisher;
import account.usecase.port.out.CustomerAccountRepository;
import java.time.Clock;

public final class CustomerChangeName implements CustomerNameChanger {

    private final CustomerAccountRepository customers;
    private final AccountIEPublisher publisher;
    private final Clock clock;

    public CustomerChangeName(CustomerAccountRepository customers, AccountIEPublisher publisher, Clock clock) {
        this.customers = customers;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public ChangeNameResponse change(ChangeNameRequest request) {
        CustomerAccount account = Accounts.customer(customers, request.customerId());
        account.changeName(request.name());
        customers.save(account);
        publisher.publish(Accounts.detailsChanged(account, clock.instant()));
        return new ChangeNameResponse(account.id(), account.name());
    }
}
