package account.usecase;

import account.domain.CustomerAccount;
import account.usecase.dto.CloseCustomerAccountRequest;
import account.usecase.dto.CloseCustomerAccountResponse;
import account.usecase.port.in.CustomerAccountCloser;
import account.usecase.port.out.AccountIEPublisher;
import account.usecase.port.out.CustomerAccountRepository;
import ie.events.AccountClosedIE;
import java.time.Clock;
import java.time.Instant;

public final class CustomerCloseAccount implements CustomerAccountCloser {

    private final CustomerAccountRepository customers;
    private final AccountIEPublisher publisher;
    private final Clock clock;

    public CustomerCloseAccount(CustomerAccountRepository customers, AccountIEPublisher publisher, Clock clock) {
        this.customers = customers;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public CloseCustomerAccountResponse close(CloseCustomerAccountRequest request) {
        Instant now = clock.instant();
        CustomerAccount account = Accounts.customer(customers, request.customerId());
        account.close(now);
        customers.save(account);
        publisher.publish(new AccountClosedIE(account.id(), "CUSTOMER", now));
        return new CloseCustomerAccountResponse(account.id(), now);
    }
}
