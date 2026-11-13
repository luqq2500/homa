package account.usecase;

import account.domain.CustomerAccount;
import account.usecase.dto.ChangeAddressRequest;
import account.usecase.dto.ChangeAddressResponse;
import account.usecase.port.in.CustomerAddressChanger;
import account.usecase.port.out.AccountIEPublisher;
import account.usecase.port.out.CityLocator;
import account.usecase.port.out.CustomerAccountRepository;
import java.time.Clock;

public final class CustomerChangeAddress implements CustomerAddressChanger {

    private final CustomerAccountRepository customers;
    private final CityLocator cities;
    private final AccountIEPublisher publisher;
    private final Clock clock;

    public CustomerChangeAddress(CustomerAccountRepository customers, CityLocator cities, AccountIEPublisher publisher,
                                 Clock clock) {
        this.customers = customers;
        this.cities = cities;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public ChangeAddressResponse change(ChangeAddressRequest request) {
        CustomerAccount account = Accounts.customer(customers, request.customerId());
        account.changeAddress(Accounts.address(cities, request.addressLine(), request.latitude(), request.longitude()));
        customers.save(account);
        publisher.publish(Accounts.detailsChanged(account, clock.instant()));
        return new ChangeAddressResponse(account.id(), account.address().city().name());
    }
}
