package account.usecase;

import account.domain.CustomerAccount;
import account.usecase.dto.ChooseCustomerLanguageRequest;
import account.usecase.dto.ChooseCustomerLanguageResponse;
import account.usecase.port.in.CustomerLanguageChooser;
import account.usecase.port.out.AccountIEPublisher;
import account.usecase.port.out.CustomerAccountRepository;
import java.time.Clock;

public final class CustomerChooseLanguage implements CustomerLanguageChooser {

    private final CustomerAccountRepository customers;
    private final AccountIEPublisher publisher;
    private final Clock clock;

    public CustomerChooseLanguage(CustomerAccountRepository customers, AccountIEPublisher publisher, Clock clock) {
        this.customers = customers;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public ChooseCustomerLanguageResponse choose(ChooseCustomerLanguageRequest request) {
        CustomerAccount account = Accounts.customer(customers, request.customerId());
        account.chooseLanguage(Accounts.language(request.language()));
        customers.save(account);
        publisher.publish(Accounts.detailsChanged(account, clock.instant()));
        return new ChooseCustomerLanguageResponse(account.id(), account.language().name());
    }
}
