package account.usecase;

import account.domain.CustomerAccount;
import account.usecase.dto.ChangeAccessNotesRequest;
import account.usecase.dto.ChangeAccessNotesResponse;
import account.usecase.port.in.CustomerAccessNotesChanger;
import account.usecase.port.out.AccountIEPublisher;
import account.usecase.port.out.CustomerAccountRepository;
import java.time.Clock;

public final class CustomerChangeAccessNotes implements CustomerAccessNotesChanger {

    private final CustomerAccountRepository customers;
    private final AccountIEPublisher publisher;
    private final Clock clock;

    public CustomerChangeAccessNotes(CustomerAccountRepository customers, AccountIEPublisher publisher, Clock clock) {
        this.customers = customers;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public ChangeAccessNotesResponse change(ChangeAccessNotesRequest request) {
        CustomerAccount account = Accounts.customer(customers, request.customerId());
        account.changeAccessNotes(request.accessNotes());
        customers.save(account);
        publisher.publish(Accounts.detailsChanged(account, clock.instant()));
        return new ChangeAccessNotesResponse(account.id(), account.accessNotes());
    }
}
