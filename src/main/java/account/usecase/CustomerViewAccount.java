package account.usecase;

import account.domain.CustomerAccount;
import account.usecase.dto.CustomerViewAccountRequest;
import account.usecase.dto.CustomerViewAccountResponse;
import account.usecase.port.in.CustomerAccountViewer;
import account.usecase.port.out.CustomerAccountRepository;

/** Shows a customer their own account. */
public final class CustomerViewAccount implements CustomerAccountViewer {

    private final CustomerAccountRepository customers;

    public CustomerViewAccount(CustomerAccountRepository customers) {
        this.customers = customers;
    }

    @Override
    public CustomerViewAccountResponse view(CustomerViewAccountRequest request) {
        CustomerAccount customer = Accounts.customer(customers, request.customerId());
        return new CustomerViewAccountResponse(
                customer.id(), customer.name(), customer.phone().value(), customer.address().street(),
                customer.address().city().name());
    }
}
